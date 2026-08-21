package io.github.oldmanpushcart.jinx.controller;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.core.speech.SpeechSwitchEvent;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.isBlankString;
import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.isNotBlankString;

@Controller("/api")
public class JinxController {

    private final List<Setting> settings;

    public JinxController(
            ApplicationEventPublisher<SpeechSwitchEvent> speechSwitchEventPublisher,
            SpeakerSetting speakerSetting,
            CatcherSetting catcherSetting
    ) {
        settings = List.of(

                // read-only
                new Setting("user.home", () -> System.getProperty("user.home")),
                new Setting("os.name", () -> System.getProperty("os.name")),
                new Setting("os.arch", () -> System.getProperty("os.arch")),
                new Setting("jinx.version", () -> Constants.VERSION),
                new Setting("jinx.home", Constants.HOME::toString),
                new Setting("jinx.conf", Constants.CONF::toString),
                new Setting("jinx.logs", Constants.LOGS::toString),
                new Setting("jinx.data", Constants.DATA::toString),
                new Setting("jinx.work", Constants.WORK::toString),

                // writable
                new Setting("jinx.speaker.enabled",
                        () -> String.valueOf(speakerSetting.isEnabled()),
                        v -> speechSwitchEventPublisher.publishEvent(
                                new SpeechSwitchEvent(
                                        SpeechSwitchEvent.Type.SPEAKER,
                                        Boolean.parseBoolean(v)
                                ))),
                new Setting("jinx.catcher.enabled",
                        () -> String.valueOf(catcherSetting.isEnabled()),
                        v -> speechSwitchEventPublisher.publishEvent(
                                new SpeechSwitchEvent(
                                        SpeechSwitchEvent.Type.CATCHER,
                                        Boolean.parseBoolean(v)
                                )))

        );
    }

    @Get(uri = "/health", produces = MediaType.TEXT_PLAIN)
    public String health() {
        return "OK";
    }

    @Get(uri = "/version", produces = MediaType.TEXT_PLAIN)
    public String version() {
        return Constants.VERSION;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Get(uri = "/setting", produces = MediaType.TEXT_PLAIN)
    public String setting(

            @QueryValue(value = "name")
            Optional<String> nameOpt,

            @QueryValue(value = "value")
            Optional<String> valueOpt

    ) {

        final var name = nameOpt.orElse(null);
        final var value = valueOpt.orElse(null);

        // list all settings
        if (isBlankString(name)) {
            return settings.stream()
                    .map(s -> "%s=%s".formatted(s.name(), s.get()))
                    .collect(Collectors.joining("\n"));
        }

        // get or set a specific setting
        final var setting = settings.stream()
                .filter(x -> x.name().equals(name))
                .findFirst()
                .orElse(null);

        if (setting == null) {
            return "Unknown setting: %s".formatted(name);
        }

        if (isNotBlankString(value)) {
            if (setting.isReadOnly()) {
                return "Setting is read-only: %s".formatted(name);
            }
            setting.set(value);
        }

        return "%s=%s".formatted(name, setting.get());
    }

    record Setting(String name, Supplier<String> getter, Consumer<String> setter) {

        public Setting(String name, Supplier<String> getter) {
            this(name, getter, null);
        }

        public boolean isReadOnly() {
            return null == setter;
        }

        public String get() {
            return getter().get();
        }

        public void set(String value) {
            if (isReadOnly()) {
                throw new UnsupportedOperationException("Setting is read-only: %s".formatted(name));
            }
            setter().accept(value);
        }

    }

}
