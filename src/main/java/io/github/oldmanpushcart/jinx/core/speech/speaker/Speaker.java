package io.github.oldmanpushcart.jinx.core.speech.speaker;

public interface Speaker extends AutoCloseable {

    void speak(String text);

    boolean isClosed();

    @Override
    void close();

    void abort();

}
