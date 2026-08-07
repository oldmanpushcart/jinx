package io.github.oldmanpushcart.jinx.core2;

public class CoreException extends RuntimeException {

    private final String code;

    public CoreException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public CoreException(String code, String message) {
        super(message);
        this.code = code;
    }

    @Override
    public String getLocalizedMessage() {
        return "Jinx Error! code=%s;message=%s".formatted(code, getMessage());
    }

    public String code() {
        return code;
    }

}
