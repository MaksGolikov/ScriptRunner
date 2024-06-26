package holikov.scriptrunner.model;

import holikov.scriptrunner.exception.BadRequestException;
import holikov.scriptrunner.exception.ScriptNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages storage and retrieval of scripts along with their execution futures.
 * Provides methods to add, retrieve, remove scripts and manage their execution futures.
 */
@Component
public class ScriptStorage {

    private final Map<Long, Script> scriptMap = new ConcurrentHashMap<>();
    private final Map<Long, Future<?>> scriptFutureMap = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    /**
     * Adds a new script to the storage.
     *
     * @param scriptBody The body of the script to add.
     * @return The `Script` object representing the added script.
     */
    public Script addScript(String scriptBody) {
        Long scriptId = counter.incrementAndGet();
        Script script = new Script(scriptId, scriptBody);
        script.setStatus(ScriptStatus.QUEUED);
        scriptMap.put(scriptId, script);
        return script;
    }

    /**
     * Retrieves a script by its ID.
     *
     * @param id The ID of the script to retrieve.
     * @return The `Script` object corresponding to the given ID.
     * @throws BadRequestException If the provided ID is null or less than or equal to zero.
     * @throws ScriptNotFoundException If no script exists with the given ID.
     */
    public Script getScript(Long id) {

        Script script = scriptMap.get(id);

        if (id == null || id <= 0) {
            throw new BadRequestException("Invalid script ID");
        }
        if (script == null) {
            throw new ScriptNotFoundException("Script with ID " + id + " does not exist.");
        }
        return script;
    }

    /**
     * Associates a script ID with its execution future.
     *
     * @param id The ID of the script.
     * @param future The `Future` object representing the execution future of the script.
     */
    public void addScriptFuture(Long id, Future<?> future) {
        scriptFutureMap.put(id, future);
    }

    /**
     * Removes a script from the storage by its ID.
     *
     * @param id The ID of the script to remove.
     */
    public void removeScript(Long id) {
        scriptMap.remove(id);
    }

    /**
     * Removes the execution future associated with a script by its ID.
     *
     * @param id The ID of the script.
     */
    public void removeScriptFuture(Long id) {
        scriptFutureMap.remove(id);
    }

    /**
     * Retrieves the map containing all stored scripts.
     *
     * @return A `Map` object where keys are script IDs and values are `Script` objects.
     */
    public Map<Long, Script> getScriptMap() {
        return scriptMap;
    }

    /**
     * Retrieves the map containing all script execution futures.
     *
     * @return A `Map` object where keys are script IDs and values are `Future` objects.
     */
    public Map<Long, Future<?>> getScriptFutureMap() {
        return scriptFutureMap;
    }
}

