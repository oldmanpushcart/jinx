package io.github.oldmanpushcart.jinx.core.speech.speaker.impl;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * 音频播放通道
 */
class SourceDataLineChannel implements SourceDataLine, WritableByteChannel {

    private final SourceDataLine source;
    private final byte[] buf;

    public SourceDataLineChannel(SourceDataLine source) {
        this.source = source;
        this.buf = new byte[source.getBufferSize()];
    }

    @Override
    public int write(ByteBuffer src) {
        int total = src.remaining();
        while (src.hasRemaining()) {
            final var limit = Math.min(src.remaining(), buf.length);
            src.get(buf, 0, limit);
            int written = 0;
            while (written < limit) {
                written += source.write(buf, 0, limit);
            }
        }
        return total;
    }

    @Override
    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        source.open(format, bufferSize);
    }

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
        source.open(format);
    }

    @Override
    public int write(byte[] b, int off, int len) {
        return source.write(b, off, len);
    }

    @Override
    public void drain() {
        source.drain();
    }

    @Override
    public void flush() {
        // No-op for SourceDataLine
    }

    @Override
    public void start() {
        source.start();
    }

    @Override
    public void stop() {
        source.stop();
    }

    @Override
    public boolean isRunning() {
        return source.isRunning();
    }

    @Override
    public boolean isActive() {
        return source.isActive();
    }

    @Override
    public AudioFormat getFormat() {
        return source.getFormat();
    }

    @Override
    public int getBufferSize() {
        return source.getBufferSize();
    }

    @Override
    public int available() {
        return source.available();
    }

    @Override
    public int getFramePosition() {
        return source.getFramePosition();
    }

    @Override
    public long getLongFramePosition() {
        return source.getLongFramePosition();
    }

    @Override
    public long getMicrosecondPosition() {
        return source.getMicrosecondPosition();
    }

    @Override
    public float getLevel() {
        return source.getLevel();
    }

    @Override
    public Line.Info getLineInfo() {
        return source.getLineInfo();
    }

    @Override
    public void open() throws LineUnavailableException {
        source.open();
    }

    @Override
    public void close() {
        source.close();
    }

    @Override
    public boolean isOpen() {
        return source.isOpen();
    }

    @Override
    public Control[] getControls() {
        return source.getControls();
    }

    @Override
    public boolean isControlSupported(Control.Type control) {
        return source.isControlSupported(control);
    }

    @Override
    public Control getControl(Control.Type control) {
        return source.getControl(control);
    }

    @Override
    public void addLineListener(LineListener listener) {
        source.addLineListener(listener);
    }

    @Override
    public void removeLineListener(LineListener listener) {
        source.removeLineListener(listener);
    }
}
