package io.github.oldmanpushcart.jinx.extra.persona;

import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * 人格工具集
 */
@Singleton
public class PersonaToolkit implements Toolkit {

    private final Persona persona;
    private final List<Tool> tools = List.of(
            personaReload()
    );

    public PersonaToolkit(Persona persona) {
        this.persona = persona;
    }

    @Override
    public @NonNull Iterator<Tool> iterator() {
        return tools.iterator();
    }

    /**
     * @return 重新加载人格工具
     */
    private Tool personaReload() {
        return FunctionTool.newBuilder()
                .name("persona$reload")
                .description("重新加载人格")
                .parameterType(ReloadSpec.class)
                .<ReloadSpec>function(spec -> {
                    try {
                        persona.load();
                        return "重新加载人格成功";
                    } catch (IOException ioEx) {
                        return ToolExecutionException.callFailed("persona$reload", ioEx);
                    }
                })
                .build();
    }

    private record UpdateSpec(String content) {

    }

    private record ReloadSpec() {

    }

    private record GetSpec() {

    }

}
