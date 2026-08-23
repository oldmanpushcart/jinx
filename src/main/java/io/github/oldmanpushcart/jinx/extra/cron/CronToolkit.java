package io.github.oldmanpushcart.jinx.extra.cron;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;

/**
 * 定时任务工具集
 */
class CronToolkit implements Toolkit {

    private final CronDetector detector;
    private final List<Tool> tools;

    public CronToolkit(CronDetector detector) {
        this.detector = detector;
        this.tools = List.of(cronAdd());
    }

    @Override
    public @NonNull Iterator<Tool> iterator() {
        return tools.iterator();
    }

    private Tool cronAdd() {
        return FunctionTool.newBuilder()
                .name("cron$add")
                .description("创建定时调度任务。支持定时执行（指定具体时间）和定期执行（指定 cron 表达式）。")
                .parameterType(AddSpec.class)
                .<AddSpec>function(spec -> {
                    try {
                        final var meta = new CronMeta(spec.name, spec.cron, spec.prompt, true, parseMode(spec.mode));
                        detector.create(meta);
                        return "定时任务创建成功: %s".formatted(spec.name);
                    } catch (Exception e) {
                        return ToolExecutionException.callFailed("cron$add", e);
                    }
                })
                .build();
    }

    /**
     * 解析调度模式：宽容大小写，非法或缺省返回 null（由 CronMeta 归一为 DELAY）。
     */
    private static CronMeta.Mode parseMode(String mode) {
        if (null == mode || mode.isBlank()) {
            return null;
        }
        try {
            return CronMeta.Mode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    record AddSpec(
            @JsonProperty("name")
            @JsonPropertyDescription("任务名称（英文，小写字母、数字和连字符）")
            String name,

            @JsonProperty("cron")
            @JsonPropertyDescription("Cron 表达式，如 '0 30 10 * * ?' 表示每天10:30执行")
            String cron,

            @JsonProperty("prompt")
            @JsonPropertyDescription("触发时执行的指令")
            String prompt,

            @JsonProperty("mode")
            @JsonPropertyDescription("调度模式（可选，默认 delay）：delay=上次执行完成后再调度下一个触发点，串行不重叠；fixed=严格按 cron 时刻触发，上次未完成时允许并行执行")
            String mode
    ) {
    }

}
