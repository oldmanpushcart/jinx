package io.github.oldmanpushcart.jinx.extra.cron;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * 定时任务定义
 * <p>
 * 由{@code type}字段（必填）区分两种实现：
 * {@code cron}为周期任务（{@link Cron}），按Cron表达式重复触发；
 * {@code at}为一次性任务（{@link At}），在指定绝对时间触发一次后终结。
 * {@code type}缺失或值未知时解析失败。
 * </p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CronMeta.Cron.class, name = "cron"),
        @JsonSubTypes.Type(value = CronMeta.At.class, name = "at")
})
public sealed interface CronMeta permits CronMeta.Cron, CronMeta.At {

    /**
     * @return 任务名（须与文件名一致）
     */
    String name();

    /**
     * @return 触发时发送给 Agent 的指令
     */
    String prompt();

    /**
     * @return 启停控制
     */
    boolean enabled();

    /**
     * @return 发起任务的 CHAT 会话ID，触发结果回写该会话
     */
    String sessionId();

    /**
     * 公共校验：会话ID必填，缺失时解析失败，任务不会被加载
     */
    static void requireSessionId(String name, String sessionId) {
        if (null == sessionId || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required! name=%s".formatted(name));
        }
    }

    /**
     * 周期任务：按Cron表达式重复触发
     *
     * @param name      任务名（须与文件名一致）
     * @param cron      Cron 表达式
     * @param prompt    触发时发送给 Agent 的指令
     * @param enabled   启停控制
     * @param mode      调度模式（缺省为 DELAY）
     * @param sessionId 发起任务的 CHAT 会话ID（必填）
     */
    record Cron(
            String name,
            String cron,
            String prompt,
            boolean enabled,
            Mode mode,
            String sessionId
    ) implements CronMeta {

        public Cron {
            // 缺省按 DELAY 处理
            mode = (null == mode) ? Mode.DELAY : mode;
            requireSessionId(name, sessionId);
        }

    }

    /**
     * 一次性任务：在指定绝对时间触发一次后终结，不续期
     *
     * @param name      任务名（须与文件名一致）
     * @param at        触发时刻（ISO本地时间，如 2026-08-27T16:31，按系统默认时区解释）
     * @param prompt    触发时发送给 Agent 的指令
     * @param enabled   启停控制
     * @param sessionId 发起任务的 CHAT 会话ID（必填）
     */
    record At(
            String name,
            String at,
            String prompt,
            boolean enabled,
            String sessionId
    ) implements CronMeta {

        public At {
            requireSessionId(name, sessionId);
            requireAt(name, at);
        }

        /**
         * @return 触发时刻
         */
        public LocalDateTime atTime() {
            return LocalDateTime.parse(at);
        }

    }

    /**
     * 校验at字段：必填且为合法的ISO本地时间格式
     */
    private static void requireAt(String name, String at) {
        if (null == at || at.isBlank()) {
            throw new IllegalArgumentException("at is required! name=%s".formatted(name));
        }
        try {
            LocalDateTime.parse(at);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "invalid at format! name=%s; at=%s; expect ISO local date-time like 2026-08-27T16:31".formatted(name, at)
            );
        }
    }

    /**
     * 调度模式（仅周期任务）
     */
    enum Mode {

        /**
         * 固定频率：严格按 cron 时刻触发，上次未完成时允许并行执行
         */
        @JsonProperty("fixed")
        FIXED,

        /**
         * 固定延迟：上次执行完成后，再调度完成时刻之后的下一个 cron 点，天然串行
         */
        @JsonProperty("delay")
        DELAY
    }

}
