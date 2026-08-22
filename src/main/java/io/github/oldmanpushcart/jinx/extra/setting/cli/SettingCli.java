package io.github.oldmanpushcart.jinx.extra.setting.cli;

import io.github.oldmanpushcart.jinx.Constants;
import io.github.oldmanpushcart.jinx.cli.Cli;
import io.github.oldmanpushcart.jinx.core.speech.SpeechSwitchEvent;
import io.github.oldmanpushcart.jinx.core.speech.catcher.CatcherSetting;
import io.github.oldmanpushcart.jinx.core.speech.speaker.SpeakerSetting;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.isBlankString;
import static io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils.isNotBlankString;

/**
 * setting — 查看和修改系统设置
 * <p>
 * 无参：列出全部设置<br>
 * args[0] = name：查询指定设置<br>
 * args[0] = name, args[1] = value：修改指定设置
 * </p>
 */
@Singleton
class SettingCli implements Cli {

    private final List<Setting> settings;

    public SettingCli(
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

    @Override
    public String command() {
        return "setting";
    }

    @Override
    public String description() {
        return "View or modify system settings.";
    }

    @Override
    public Publisher<String> execute(Context ctx) {
        final var args = ctx.args();

        final var name = !args.isEmpty() ? args.get(0) : null;
        final var value = args.size() > 1 ? args.get(1) : null;

        // list all settings
        if (isBlankString(name)) {
            return Mono.just(settings.stream()
                    .map(s -> "%s=%s".formatted(s.name(), s.get()))
                    .collect(Collectors.joining("\n")));
        }

        // get or set a specific setting
        final var setting = settings.stream()
                .filter(x -> x.name().equals(name))
                .findFirst()
                .orElse(null);

        if (setting == null) {
            return Mono.just("Unknown setting: %s".formatted(name));
        }

        if (isNotBlankString(value)) {
            if (setting.isReadOnly()) {
                return Mono.just("Setting is read-only: %s".formatted(name));
            }
            setting.set(value);
        }

        return Mono.just("%s=%s".formatted(name, setting.get()));
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
