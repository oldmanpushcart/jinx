package io.github.oldmanpushcart.jinx.core.speech.speaker.impl;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.jinx.core.speech.speaker.Speaker;
import org.jspecify.annotations.NonNull;

final class SpeakerImpl implements Speaker {

    private final QwenTtsRealtimeEmitter.ServerVad emitter;
    private final String _toString;

    SpeakerImpl(QwenTtsRealtimeEmitter.ServerVad emitter) {
        this.emitter = emitter;
        this._toString = "jinx:/speaker/%s".formatted(emitter.session().id());
    }

    @Override
    public void speak(String text) {
        emitter.text(text);
    }

    @Override
    public boolean isClosed() {
        return emitter.isClosed();
    }

    @Override
    public void close() {
        emitter.close();
    }

    @Override
    public void abort() {
        emitter.abort();
    }


    @Override
    public @NonNull String toString() {
        return _toString;
    }

}
