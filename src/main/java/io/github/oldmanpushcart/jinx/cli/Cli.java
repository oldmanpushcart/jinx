package io.github.oldmanpushcart.jinx.cli;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * CLI命令接口
 * <p>
 * 每个实现代表一个命令域（可包含多条命令），
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
     * 命令条目
     *
     * @param name        命令语法（如 "mcp detail <NAME>"）
     * @param description 命令描述
     */
    record Item(String name, String description) {
    }

    /**
     * @return 命令名称（路由键 + 分组键）
     */
    String command();

    /**
     * @return 该 Bean 提供的所有命令条目
     */
    List<Item> usage();

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
