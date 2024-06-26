package holikov.scriptrunner.service;

import holikov.scriptrunner.model.Script;
import holikov.scriptrunner.model.ScriptStatus;
import holikov.scriptrunner.model.ScriptStorage;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service class for managing and executing scripts.
 */
@Service
public class ScriptService {

    private final ExecutorService executorServiceForScripts;
    private final ExecutorService executorServiceForNonBlocking;
    private final ScheduledExecutorService scheduler;
    private final ScriptStorage scriptStorage;

    /**
     * Constructs a new ScriptService.
     *
     * @param executorServiceForScripts Executor service for handling script execution.
     * @param executorServiceForNonBlocking Executor service for handling non-blocking script execution.
     * @param scheduler Scheduled executor service for periodic cleanup tasks.
     * @param scriptStorage Storage for managing scripts.
     */
    public ScriptService(ExecutorService executorServiceForScripts, ExecutorService executorServiceForNonBlocking,
                         ScheduledExecutorService scheduler, ScriptStorage scriptStorage) {
        this.executorServiceForScripts = executorServiceForScripts;
        this.executorServiceForNonBlocking = executorServiceForNonBlocking;
        this.scheduler = scheduler;
        this.scriptStorage = scriptStorage;
    }


    /**
     * Initializes the service and schedules periodic cleanup tasks.
     */
    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::cleanUp, 0, 1, TimeUnit.HOURS);
    }

    /**
     * Executes a script either in a blocking or non-blocking manner.
     * The script is stored and executed based on the `blocking` parameter.
     * If blocking is true, the script is executed in the current thread and the method waits for completion.
     * If blocking is false, the script is executed asynchronously.
     *
     * @param scriptBody the body of the script to be executed
     * @param blocking whether the execution should be blocking or non-blocking
     * @return a Script object representing the script being executed
     */
    public Script executeScript(String scriptBody, boolean blocking) {
        Script script = scriptStorage.addScript(scriptBody);

        if(blocking){
            executeScriptInternal(script, blocking);
        } else {
            Runnable task = () -> executeScriptInternal(script, blocking);
            executorServiceForNonBlocking.submit(task);
        }
        return script;
    }

    /**
     * Internally executes the script and manages its lifecycle.
     * This method handles script execution, including setting the status,
     * capturing output, and handling exceptions.
     *
     * @param script the Script object to be executed
     * @param blocking whether the execution should be blocking or non-blocking
     */
    public void executeScriptInternal(Script script, boolean blocking) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayOutputStream err = new ByteArrayOutputStream()) {

            try (Context context = Context.newBuilder("js")
                    .out(out)
                    .err(err)
                    .allowInnerContextOptions(true)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build()) {

                Source source = Source.newBuilder("js", script.getBody(), "script.js").build();
                Future<?> evalFuture = executorServiceForScripts.submit(() -> {
                    script.setStatus(ScriptStatus.EXECUTING);
                    script.setStartTime(new Date());
                    context.eval(source);
                });
                scriptStorage.addScriptFuture(script.getId(), evalFuture);
                while (!evalFuture.isDone()) {
                    sendOutput(script, out, err);
                }

                if (evalFuture.isCancelled()) {
                    context.close(true);
                    evalFuture.cancel(true);
                    throw new InterruptedException();
                }
                if(blocking){
                    evalFuture.get();
                }
                script.setStatus(ScriptStatus.COMPLETED);
            } catch (InterruptedException e) {
                handleException(script, e);
                script.setStatus(ScriptStatus.STOPPED);
            } catch (CancellationException | ExecutionException e) {
                handleException(script, e);
            }
        } catch (IOException e) {
            handleException(script, e);
        } finally {
            script.setEndTime(new Date());
        }
    }

    /**
     * Updates the script's standard output and error streams.
     *
     * @param script the Script object whose output is to be updated
     * @param out the ByteArrayOutputStream for standard output
     * @param err the ByteArrayOutputStream for error output
     * @throws IOException if an I/O error occurs
     */
    private void sendOutput(Script script, ByteArrayOutputStream out, ByteArrayOutputStream err) throws IOException {
        script.setStdout(out.toString(StandardCharsets.UTF_8));
        script.setStderr(err.toString(StandardCharsets.UTF_8));
    }

    /**
     * Handles exceptions that occur during script execution.
     *
     * @param script the Script object where the exception occurred
     * @param e the exception to handle
     */
    private void handleException(Script script, Exception e) {
        script.setStatus(ScriptStatus.FAILED);
        script.setError(e.toString());
    }

    /**
     * Lists scripts based on optional filters and sorting.
     *
     * @param status the optional status filter.
     * @param orderBy the optional order by field (id or time).
     * @return a list of Script objects.
     */
    public List<Script> listScripts(Optional<String> status, Optional<String> orderBy) {
        List<Script> scripts = new ArrayList<>(scriptStorage.getScriptMap().values());

        ScriptStatus filterStatus = status.map(String::toUpperCase)
                .flatMap(this::parseScriptStatus)
                .orElse(null);

        if (filterStatus != null) {
            scripts.removeIf(script -> script.getStatus() != filterStatus);
        }

        orderBy.ifPresent(order -> {
            if (order.equalsIgnoreCase("id")) {
                scripts.sort(Comparator.comparing(Script::getId));
            } else if (order.equalsIgnoreCase("time")) {
                scripts.sort(Comparator.comparing(Script::getStartTime).reversed());
            }
        });

        return scripts;
    }

    /**
     * Parses a string to a ScriptStatus enum.
     *
     * @param status the status string to parse.
     * @return an Optional of ScriptStatus.
     */
    private Optional<ScriptStatus> parseScriptStatus(String status) {
        try {
            return Optional.of(ScriptStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets a script by its ID.
     *
     * @param id the ID of the script.
     * @return a Script object.
     */
    public Script getScript(Long id) {
        return scriptStorage.getScript(id);
    }

    /**
     * Stops an executing script by its ID.
     *
     * @param id the ID of the script.
     * @return the stopped Script object.
     */
    public Script stopScript(Long id) {
        Script script = scriptStorage.getScript(id);
        if (script != null && script.getStatus().equals(ScriptStatus.EXECUTING)) {
            Future<?> future = scriptStorage.getScriptFutureMap().get(id);
            if (future != null && !future.isDone()) {
                future.cancel(true);  // Прерывание выполнения скрипта
                script.setStatus(ScriptStatus.STOPPED);
                script.setEndTime(new Date());
            }
        }
        return script;
    }

    /**
     * Deletes a script by its ID.
     *
     * @param id the ID of the script to delete.
     */
    public void deleteScript(Long id) {
        Script script = scriptStorage.getScript(id);
        if (script != null && !script.getStatus().equals(ScriptStatus.EXECUTING) && !script.getStatus().equals(ScriptStatus.QUEUED)) {
            scriptStorage.removeScript(id);
            scriptStorage.removeScriptFuture(id);
        }
    }

    /**
     * Cleans up completed, failed, and stopped scripts, and their associated futures.
     */
    private void cleanUp() {
        scriptStorage.getScriptMap().values().removeIf(script -> {
            ScriptStatus status = script.getStatus();
            return status == ScriptStatus.COMPLETED ||
                    status == ScriptStatus.FAILED ||
                    status == ScriptStatus.STOPPED;
        });
        scriptStorage.getScriptFutureMap().keySet().removeIf(id -> !scriptStorage.getScriptMap().containsKey(id));
    }
}


