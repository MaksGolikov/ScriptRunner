package holikov.scriptrunner.service;

import holikov.scriptrunner.model.Script;
import holikov.scriptrunner.model.ScriptStatus;
import holikov.scriptrunner.model.ScriptStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScriptServiceTest {

    @Mock
    private ScriptStorage scriptStorage;

    @InjectMocks
    private ScriptService scriptService;

//    @Test
//    void shouldExecuteScriptSynchronously() {
//        var script = new Script(123L, "print('Hello, World!')");
//        when(scriptStorage.addScript("print('Hello, World!')")).thenReturn(script);
//        Future future = CompletableFuture.completedFuture(null);
//        when(executorServiceForScripts.submit(any(Runnable.class))).thenReturn(future);
//
//        var actual = scriptService.executeScript("print('Hello, World!')", true);
//
//        verify(executorServiceForScripts).submit(any(Runnable.class));
//        verify(scriptStorage).addScript("print('Hello, World!')");
//        verify(scriptStorage).addScriptFuture(eq(123L), any());
//        assertThat(actual).isEqualTo(script);
//
//    }

    @Test
    void shouldReturnScript() {
        var script = new Script(123L, "print('Hello, World!')");
        when(scriptStorage.getScript(123L)).thenReturn(script);

        var actual = scriptService.getScript(123L);

        assertThat(actual).isEqualTo(script);
        verify(scriptStorage).getScript(123L);
    }

    @Test
    void testGetScript() {
        Long scriptId = 1L;
        Script script = new Script(scriptId, "print('Hello')");
        when(scriptStorage.getScript(scriptId)).thenReturn(script);

        Script result = scriptService.getScript(scriptId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(scriptId);
    }

    @Test
    void shouldStopScript() {
        Long scriptId = 1L;
        Script script = new Script(scriptId, "print('Hello')");
        script.setStatus(ScriptStatus.EXECUTING);
        when(scriptStorage.getScript(scriptId)).thenReturn(script);
        when(scriptStorage.getScriptFutureMap()).thenReturn(Map.of(1L, new CompletableFuture<>()));

        Script result = scriptService.stopScript(scriptId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ScriptStatus.STOPPED);
    }

    @Test
     void shouldDeleteScript() {
        Long scriptId = 1L;
        Script script = new Script(scriptId, "print('Hello')");
        script.setStatus(ScriptStatus.COMPLETED);
        when(scriptStorage.getScript(scriptId)).thenReturn(script);

        scriptService.deleteScript(scriptId);

        verify(scriptStorage).removeScript(scriptId);
        verify(scriptStorage).removeScriptFuture(scriptId);
    }

}