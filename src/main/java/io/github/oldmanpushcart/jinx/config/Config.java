package io.github.oldmanpushcart.jinx.config;

import io.github.oldmanpushcart.jinx.core.dashscope.agent.AgentConfig;
import io.github.oldmanpushcart.jinx.core.mcp.McpConfig;
import io.github.oldmanpushcart.jinx.core.skill.SkillConfig;
import io.micronaut.context.annotation.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("jinx")
public record Config(
        Path dataspace,
        Path workspace,
        SkillConfig skill,
        McpConfig mcp,
        AgentConfig agent
) {
}
