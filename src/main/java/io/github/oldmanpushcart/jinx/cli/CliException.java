package io.github.oldmanpushcart.jinx.cli;

import io.micronaut.http.HttpStatus;
import reactor.core.Exceptions;

/**
 * CLI异常
 * <p>
 * Cli执行链路上漏出错误的统一包装，携带HTTP状态码，
 * 由 {@link CliExceptionHandler} 捕获并渲染为文本响应。
 * {@code chat} 流式过程中的错误由 ChatCli 内部转为文本流出，不会抛出此异常。
 * </p>
 */
public class CliException extends RuntimeException {

    private final HttpStatus status;

    public CliException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public CliException(HttpStatus status, Throwable cause) {
        super(messageOf(cause), Exceptions.unwrap(cause));
        this.status = status;
    }

    /**
     * @return HTTP状态码
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * 渲染统一的错误文本
     *
     * @param t 异常
     * @return 错误文本
     */
    public static String textOf(Throwable t) {
        return "ERROR: " + messageOf(t);
    }

    private static String messageOf(Throwable t) {
        final var cause = Exceptions.unwrap(t);
        final var msg = cause.getMessage();
        return msg != null ? msg : cause.toString();
    }

}
