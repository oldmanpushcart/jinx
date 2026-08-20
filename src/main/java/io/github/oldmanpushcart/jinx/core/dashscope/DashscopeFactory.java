package io.github.oldmanpushcart.jinx.core.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.Hook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.session.SessionHook;
import io.github.oldmanpushcart.dashscope4j.agent.hook.session.storage.FileFragmentStorage;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.RetryInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.retry.RetryStrategies;
import io.github.oldmanpushcart.jinx.Constants;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.List;

@Factory
public class DashscopeFactory {

    private static final Interceptor retryInterceptor = RetryInterceptor.newBuilder()
            .strategy(RetryStrategies.fixedDelay(Duration.ofSeconds(5), 5))
            .build();

    @Singleton
    public DashscopeClient makeDashscopeClient(DashscopeConfig.Client clientCfg) {
        return DashscopeClient.newBuilder()

                // 设置AK
                .ak(clientCfg.ak())

                // 设置HTTP
                .building(builder -> {
                    final var httpCfg = clientCfg.http();
                    builder.http(new OkHttpClient.Builder()
                            .connectTimeout(httpCfg.connectTimeout())
                            .readTimeout(httpCfg.readTimeout())
                            .writeTimeout(httpCfg.writeTimeout())
                            .pingInterval(Duration.ZERO)
                            .build());
                })

                // 设置重试拦截器
                .interceptors(List.of(retryInterceptor))

                // 构建千问客户端
                .build();
    }

    @Singleton
    public Agent makeDashscopeAgent(DashscopeConfig.Agent agentCfg, DashscopeClient client, List<Hook> hooks) {
        return ReActAgent.newBuilder()
                .name(agentCfg.name())
                .description(agentCfg.description())
                .client(client)
                .hooks(hooks)
                .model(ChatModel.QWEN_PLUS)
                .build();
    }

    @Singleton
    public SessionHook makeSessionHook() {
        return SessionHook.newBuilder()
                .storage(FileFragmentStorage.newBuilder()
                        .directory(Constants.DATA.resolve("session"))
                        .build())
                // 1M
                .maxTokens(1000 * 1000)

                // 70%触发压缩
                .gcRatio(0.7)

                // 压缩模型
                .model(ChatModel.QWEN_FLASH)

                .build();
    }

}
