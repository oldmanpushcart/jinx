package io.github.oldmanpushcart.jinx.extra.information;

import io.github.oldmanpushcart.jinx.Constants;
import jakarta.inject.Singleton;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;

/**
 * 运行时信息
 * <p>
 * 在构造期一次性采集进程生命周期内不变的运行时信息，渲染为 {@code KEY: VALUE} 纯文本。
 * </p>
 * <ul>
 *     <li>注入上下文：由 {@link InformationHook} 追加 SESSION 后作为 SYSTEM 消息注入</li>
 *     <li>命令行查询：由 {@link InformationCli} 追加调用方 SESSION 后输出</li>
 * </ul>
 */
@Singleton
class Information {

    private final String content;

    public Information() {
        this.content = """
                OS: %s
                SHELL: %s
                TIMEZONE: %s
                JAVA: %s
                JINX: %s
                WORKDIR: %s
                USER-HOME: %s""".formatted(
                os(),
                shell(),
                timezone(),
                java(),
                Constants.VERSION,
                Constants.HOME,
                Constants.USER_HOME
        );
    }

    /**
     * @return 静态运行时信息文本（不含 SESSION）
     */
    public String content() {
        return content;
    }

    /**
     * @return 操作系统（含架构）
     */
    private static String os() {
        return "%s (%s)".formatted(
                System.getProperty("os.name"),
                System.getProperty("os.arch")
        );
    }

    /**
     * shell$exec 采用 fork/exec 裸执行，无 shell 解释器；
     * 如需管道、重定向、变量展开等 shell 语法，须显式用包裹器执行
     *
     * @return 命令执行语义及推荐包裹器
     */
    private static String shell() {
        final var wrapper = isWindows()
                ? "cmd.exe /c"
                : Files.exists(Path.of("/bin/bash")) ? "/bin/bash -c" : "/bin/sh -c";
        return "direct exec without interpreter; wrap with \"%s\" for pipe/redirect/variable syntax".formatted(wrapper);
    }

    /**
     * @return 时区（含 UTC 偏移）
     */
    private static String timezone() {
        final var now = ZonedDateTime.now();
        final var offset = now.getOffset();
        return "%s (UTC%s)".formatted(
                now.getZone(),
                offset.getTotalSeconds() == 0 ? "" : offset.toString()
        );
    }

    /**
     * @return Java 运行时
     */
    private static String java() {
        return "%s %s".formatted(
                System.getProperty("java.runtime.name"),
                System.getProperty("java.version")
        );
    }

    /**
     * @return 是否 Windows 系统
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

}
