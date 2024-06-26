package holikov.scriptrunner.controller;

import holikov.scriptrunner.model.Script;
import holikov.scriptrunner.service.ScriptService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/scripts")
public class ScriptController {

    private final ScriptService scriptService;

    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @PostMapping("/execute")
    public DeferredResult<ResponseEntity<?>> executeScript(@RequestBody Map<String, String> request) {
        DeferredResult<ResponseEntity<?>> output = new DeferredResult<>();
        boolean blocking = Boolean.parseBoolean(request.getOrDefault("blocking", "false"));

        String scriptBody = request.get("script");
        if (scriptBody == null) {
            output.setErrorResult(new IllegalArgumentException("Script is null"));
            return output;
        }
        Script script = scriptService.executeScript(scriptBody, blocking);

        if (blocking) {
            output.setResult(ResponseEntity.ok(script));
        } else {
            output.setResult(ResponseEntity.accepted().body(script));
        }
        return output;
    }

    @GetMapping
    public ResponseEntity<List<Script>> listScripts(@RequestParam Optional<String> status,
                                                    @RequestParam Optional<String> orderBy) {
        List<Script> scripts = scriptService.listScripts(status, orderBy);
        return ResponseEntity.ok(scripts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Script> getScript(@PathVariable Long id) {
        Script script = scriptService.getScript(id);
        return ResponseEntity.ok(script);
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Script> stopScript(@PathVariable Long id) {
        Script script = scriptService.stopScript(id);
        return ResponseEntity.ok(script);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScript(@PathVariable Long id) {
        scriptService.deleteScript(id);
        return ResponseEntity.ok().build();
    }
}

