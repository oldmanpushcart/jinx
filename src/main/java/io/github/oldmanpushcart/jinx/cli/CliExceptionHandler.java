package io.github.oldmanpushcart.jinx.cli;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

/**
 * CLI全局异常处理器
 * <p>
 * 仅匹配 {@link CliException}，因此只对Cli执行链路生效：
 * 无论是同步抛出的异常，还是响应提交前到达的Publisher错误，
 * 都在这里统一渲染为"自定义状态码 + 处理后的文本"。
 * </p>
 */
@Produces
@Singleton
@Requires(classes = {CliException.class, ExceptionHandler.class})
class CliExceptionHandler implements ExceptionHandler<CliException, HttpResponse<String>> {

    @Override
    public HttpResponse<String> handle(HttpRequest request, CliException exception) {
        return HttpResponse.<String>status(exception.status(), CliException.textOf(exception))
                .contentType("text/plain; charset=UTF-8");
    }

}
