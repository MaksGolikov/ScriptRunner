package holikov.scriptrunner.exception;

public class ScriptNotFoundException extends RuntimeException {
    public ScriptNotFoundException(String message) {
        super(message);
    }
}
