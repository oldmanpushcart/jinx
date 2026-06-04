package io.github.oldmanpushcart.jinx.core.speech.speaker.impl;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.jinx.core.speech.speaker.Speaker;

record SpeakerImpl(QwenTtsRealtimeEmitter.ServerVad emitter) implements Speaker {

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

}
