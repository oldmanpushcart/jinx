package io.github.oldmanpushcart.jinx.cli;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * CLI命令接口
 * <p>
 * 每个实现代表一条具体的命令（一级或二级），
 * 由 {@link CliController} 通过容器自动发现并注册。
 * </p>
 */
public interface Cli {

    /**
     * CLI执行上下文
     *
     * @param args      路由后的剩余参数
     * @param sessionId 会话ID（来自 X-Jinx-Session 请求头，可为 null）
     */
    record Context(List<String> args, String sessionId) {
    }

    /**
     * @return 一级命令名称
     */
    String command();

    /**
     * @return 二级命令名称，无则返回 null
     */
    default String sub() {
        return null;
    }

    /**
     * @return 命令描述
     */
    String description();

    /**
     * 执行命令
     *
     * @param ctx 执行上下文
     * @return 执行结果（单值或流式）
     */
    default Publisher<String> execute(Context ctx) {
        return Mono.error(() -> new UnsupportedOperationException("Not supported: %s".formatted(command())));
    }

}
