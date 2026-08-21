package io.github.oldmanpushcart.jinx.controller;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.Map;

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
                        jinx.catcher.enable=${jinx.catcher.enable}
                        """)
                .variable("user.home", System.getProperty("user.home"))
                .variable("os.name", System.getProperty("os.name"))
                .variable("os.arch", System.getProperty("os.arch"))
                .variable("jinx.version", Constants.VERSION)
                .variable("jinx.home", Constants.HOME)
                .variable("jinx.conf", Constants.CONF)
                .variable("jinx.logs", Constants.LOGS)
                .variable("jinx.data", Constants.DATA)
                .variable("jinx.work", Constants.WORK)
                .variable("jinx.speaker.enable", String.valueOf(speakerSetting.isEnabled()))
                .variable("jinx.catcher.enable", String.valueOf(catcherSetting.isEnabled()))
                .build()
                .render();
    }

    public String setting(Map<String, String> parameters) {

    }

}
