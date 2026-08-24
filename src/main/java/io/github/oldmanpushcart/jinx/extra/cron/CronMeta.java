package io.github.oldmanpushcart.jinx.extra.cron;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 定时任务定义
 *
 * @param name      任务名（须与文件名一致）
 * @param cron      Cron 表达式
 * @param prompt    触发时发送给 Agent 的指令
 * @param enabled   启停控制
 * @param mode      调度模式（缺省为 DELAY）
 * @param sessionId 发起任务的 CHAT 会话ID，触发结果回写该会话（必填）
 */
public record CronMeta(
        String name,
        String cron,
        String prompt,
        boolean enabled,
        Mode mode,
        String sessionId
) {

    public CronMeta {
        // 兼容旧配置文件：缺省按 DELAY 处理
        mode = (null == mode) ? Mode.DELAY : mode;
        // 会话ID必填：缺失时解析失败，任务不会被加载
        if (null == sessionId || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required! name=%s".formatted(name));
        }
    }

    /**
     * 调度模式
     */
    public enum Mode {

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
