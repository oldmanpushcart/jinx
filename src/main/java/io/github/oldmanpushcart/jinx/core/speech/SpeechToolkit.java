package io.github.oldmanpushcart.jinx.core.speech;

import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherLatcher;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

class SpeechToolkit implements Toolkit {

    private final SpeakerSetting speakerSetting;
    private final CatcherSetting catcherSetting;
    private final CatcherLatcher catcherLatcher;
    private final List<Tool> tools;

    public SpeechToolkit(
            final SpeakerSetting speakerSetting,
            final CatcherSetting catcherSetting,
            final CatcherLatcher catcherLatcher
    ) {
        this.speakerSetting = speakerSetting;
        this.catcherSetting = catcherSetting;
        this.catcherLatcher = catcherLatcher;
        this.tools = List.of(
                speakerSwitch(),
                catcherSwitch(),
                speechSettingShow()
        );
    }

    @Override
    public @NonNull Iterator<Tool> iterator() {
        return tools.iterator();
    }

    private Tool speakerSwitch() {
        return FunctionTool.newBuilder()
                .name("speech$speaker$switch")
                .description("语音播报开关")
                .parameterType(SwitchSpec.class)
                .<SwitchSpec>function(spec -> {
                    speakerSetting.setEnabled(spec.enabled());
                    return "SUCCESS";
                })
                .build();
    }

    private Tool catcherSwitch() {
        return FunctionTool.newBuilder()
                .name("speech$catcher$switch")
                .description("语音捕获开关")
                .parameterType(SwitchSpec.class)
                .<SwitchSpec>function(spec -> {
                    catcherSetting.setEnabled(spec.enabled());
                    if (spec.enabled()) {
                        catcherLatcher.begin();
                    } else {
                        catcherLatcher.stop();
                    }
                    return "SUCCESS";
                })
                .build();
    }

    private Tool speechSettingShow() {
        return FunctionTool.newBuilder()
                .name("speech$setting$show")
                .description("语音配置显示")
                .parameterType(Object.class)
                .function(u -> Map.of(
                        "speaker", speakerSetting,
                        "catcher", catcherSetting
                ))
                .build();
    }

    private record SwitchSpec(boolean enabled) {

    }

}
