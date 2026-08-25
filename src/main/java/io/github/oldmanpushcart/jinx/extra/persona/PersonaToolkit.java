package io.github.oldmanpushcart.jinx.extra.persona;

import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;

/**
 * 人格工具集
 */
@Singleton
public class PersonaToolkit implements Toolkit {

    private final PersonaDetector detector;
    private final List<Tool> tools;

    public PersonaToolkit(PersonaDetector detector) {
        this.detector = detector;
        this.tools = List.of(
                personaReload()
        );
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
                        detector.reload(PersonaDetector.NAME)
                                .toCompletableFuture()
                                .join();
                        return "重新加载人格成功";
                    } catch (Exception ex) {
                        return ToolExecutionException.callFailed("persona$reload", ex);
                    }
                })
                .build();
    }

    private record ReloadSpec() {

    }

}
