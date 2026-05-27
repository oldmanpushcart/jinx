module jinx {
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.module.paramnames;
    requires org.reflections;
    requires org.reactivestreams;
    requires reactor.core;
    requires reactor.adapter;
    requires dashscope4j.agent;
    requires dashscope4j.client;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.ee10.servlet;
    requires jakarta.servlet;
}