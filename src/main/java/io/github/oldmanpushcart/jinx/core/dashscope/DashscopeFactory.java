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

import static io.github.oldmanpushcart.dashscope4j.client.Constants.MULTIMODAL_GENERATION_PATH;

@Factory
public class DashscopeFactory {

    private static final Interceptor retryInterceptor = RetryInterceptor.newBuilder()
            .strategy(RetryStrategies.fixedDelay(Duration.ofSeconds(5), 5))
            .build();

    @Singleton
    public ChatModel makeChatModel() {
        return ChatModel.of("qwen3.8-flash", MULTIMODAL_GENERATION_PATH)
                .parameter("enable_thinking", false);
    }

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
    public Agent makeDashscopeAgent(
            final DashscopeConfig.Agent agentCfg,
            final DashscopeClient client,
            final ChatModel model,
            final List<Hook> hooks) {
        return ReActAgent.newBuilder()
                .name(agentCfg.name())
                .description(agentCfg.description())
                .client(client)
                .hooks(hooks)
                .model(model)
                .build();
    }

    @Singleton
    public SessionHook makeSessionHook(
            final ChatModel model
    ) {
        return SessionHook.newBuilder()
                .storage(FileFragmentStorage.newBuilder()
                        .directory(Constants.DATA.resolve("session"))
                        .build())
                // 500K
                .maxTokens(500 * 1000)

                // 70%触发压缩
                .gcRatio(0.7)

                // 压缩模型
                .model(model)

                .build();
    }

}
