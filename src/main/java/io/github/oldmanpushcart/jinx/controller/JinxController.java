package io.github.oldmanpushcart.jinx.controller;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.HashMap;

@Controller("/api")
public class JinxController {

    private final SpeakerSetting speakerSetting;
    private final CatcherSetting catcherSetting;

    public JinxController(SpeakerSetting speakerSetting, CatcherSetting catcherSetting) {
        this.speakerSetting = speakerSetting;
        this.catcherSetting = catcherSetting;
    }

    @Get(uri = "/health", produces = MediaType.TEXT_PLAIN)
    public String health() {
        return "OK";
    }

    @Get(uri = "/version", produces = MediaType.TEXT_PLAIN)
    public String version() {
        return Constants.VERSION;
    }

    @Get(uri = "/info", produces = MediaType.TEXT_PLAIN)
    public String info() {
        return PromptTemplate.newBuilder()
                .template("""
                        user.home=${user.home}
                        os.name=${os.name}
                        os.arch=${os.arch}
                        jinx.version=${jinx.version}
                        jinx.home=${jinx.home}
                        jinx.conf=${jinx.conf}
                        jinx.logs=${jinx.logs}
                        jinx.data=${jinx.data}
                        jinx.work=${jinx.work}
                        jinx.speaker.enable=${jinx.speaker.enable}
                        jinx.catcher.enable=${jinx.catcher.enable}""")
                .build()
                .render(new HashMap<>() {{

                    // user
                    put("user.home", System.getProperty("user.home"));

                    // -- system
                    put("os.name", System.getProperty("os.name"));
                    put("os.arch", System.getProperty("os.arch"));

                    // -- jinx
                    put("jinx.version", Constants.VERSION);
                    put("jinx.home", Constants.HOME);
                    put("jinx.conf", Constants.CONF);
                    put("jinx.logs", Constants.LOGS);
                    put("jinx.data", Constants.DATA);
                    put("jinx.work", Constants.WORK);
                    put("jinx.speaker.enable", speakerSetting.isEnabled());
                    put("jinx.catcher.enable", catcherSetting.isEnabled());

                }});
    }

}
