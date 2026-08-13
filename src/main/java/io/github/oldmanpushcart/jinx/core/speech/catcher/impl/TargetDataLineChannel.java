package io.github.oldmanpushcart.jinx.core.speech.catcher.impl;

import javax.sound.sampled.*;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * 音频输入通道
 */
class TargetDataLineChannel implements TargetDataLine, ReadableByteChannel {

    private final TargetDataLine target;

    public TargetDataLineChannel(TargetDataLine target) {
        this.target = target;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int bytesRead = target.read(dst.array(), dst.position(), dst.remaining());
        if (bytesRead > 0) {
            dst.position(dst.position() + bytesRead);
        }
        if (bytesRead == -1) {
            throw new EOFException();
        }
        return bytesRead;
    }

    @Override
    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        target.open(format, bufferSize);
    }

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
        target.open(format);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        return target.read(b, off, len);
    }

    @Override
    public void drain() {
        target.drain();
    }

    @Override
    public void flush() {
        target.flush();
    }

    @Override
    public void start() {
        target.start();
    }

    @Override
    public void stop() {
        target.stop();
    }

    @Override
    public boolean isRunning() {
        return target.isRunning();
    }

    @Override
    public boolean isActive() {
        return target.isActive();
    }

    @Override
    public AudioFormat getFormat() {
        return target.getFormat();
    }

    @Override
    public int getBufferSize() {
        return target.getBufferSize();
    }

    @Override
    public int available() {
        return target.available();
    }

    @Override
    public int getFramePosition() {
        return target.getFramePosition();
    }

    @Override
    public long getLongFramePosition() {
        return target.getLongFramePosition();
    }

    @Override
    public long getMicrosecondPosition() {
        return target.getMicrosecondPosition();
    }

    @Override
    public float getLevel() {
        return target.getLevel();
    }

    @Override
    public Line.Info getLineInfo() {
        return target.getLineInfo();
    }

    @Override
    public void open() throws LineUnavailableException {
        target.open();
    }

    @Override
    public void close() {
        target.close();
    }

    @Override
    public Control[] getControls() {
        return target.getControls();
    }

    @Override
    public boolean isControlSupported(Control.Type control) {
        return target.isControlSupported(control);
    }

    @Override
    public Control getControl(Control.Type control) {
        return target.getControl(control);
    }

    @Override
    public void addLineListener(LineListener listener) {
        target.addLineListener(listener);
    }

    @Override
    public void removeLineListener(LineListener listener) {
        target.removeLineListener(listener);
    }

    @Override
    public boolean isOpen() {
        return target.isOpen();
    }

}