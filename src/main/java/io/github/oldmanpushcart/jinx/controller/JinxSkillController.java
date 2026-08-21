package io.github.oldmanpushcart.jinx.controller;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.jinx.core.skill.SkillDetector;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Controller("/api/skill")
public class JinxSkillController {

    private final SkillDetector detector;

    public JinxSkillController(SkillDetector detector) {
        this.detector = detector;
    }

    @Get(value = "/list", produces = MediaType.TEXT_PLAIN)
    public String list() {
        return detector.list().stream()
                .map(skill -> skill.header().name())
                .collect(Collectors.joining("\n"));
    }

    @Get(value = "/detail", produces = MediaType.TEXT_PLAIN)
    public String detail(

            @QueryValue("name")
            String name

    ) {

        final var skill = detector.get(name).orElse(null);
        if (skill == null) {
            return "SKILL not found: %s".formatted(name);
        }

        return PromptTemplate.newBuilder()
                .template("""
                        HOME: ${skill.home}
                        NAME: ${skill.name}
                        DESCRIPTION: ${skill.description}
                        BODY:
                        ${skill.body}
                        """)
                .variable("skill.name", skill.header().name())
                .variable("skill.description", skill.header().description())
                .variable("skill.home", skill.home())
                .variable("skill.body", skill.body())
                .build()
                .render();
    }

    @Get(value = "/reload", produces = MediaType.TEXT_PLAIN)
    public CompletionStage<String> reload(

            @QueryValue("name")
            String name

    ) {
        return detector.reload(name)
                .thenApply(skill -> "SKILL reloaded: %s".formatted(skill.header().name()));
    }

}
