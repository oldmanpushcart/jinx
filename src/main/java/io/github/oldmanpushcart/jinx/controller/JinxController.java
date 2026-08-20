package io.github.oldmanpushcart.jinx.controller;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

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
        final var header = new Column[]{
                new Column().header("ITEM").dataAlign(HorizontalAlign.RIGHT),
                new Column().header("VALUE").dataAlign(HorizontalAlign.LEFT)
        };
        final var body = new String[][]{
                {"user.home", System.getProperty("user.home")},
                {"os.name", System.getProperty("os.name")},
                {"os.arch", System.getProperty("os.arch")},
                {"jinx.version", Constants.VERSION},
                {"jinx.home", Constants.HOME.toString()},
                {"jinx.conf", Constants.CONF.toString()},
                {"jinx.logs", Constants.LOGS.toString()},
                {"jinx.data", Constants.DATA.toString()},
                {"jinx.work", Constants.WORK.toString()},
                {"jinx.speaker.enable", String.valueOf(speakerSetting.isEnabled())},
                {"jinx.catcher.enable", String.valueOf(catcherSetting.isEnabled())}
        };
        return AsciiTable.getTable(header, body);
    }

}
