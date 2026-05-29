package io.github.oldmanpushcart.jinx.core.dashscope.client;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.RetryInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.retry.RetryStrategies;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.List;

@Factory
public class DashscopeClientFactory {

    private static final Interceptor retryInterceptor = RetryInterceptor.newBuilder()
            .strategy(RetryStrategies.fixedDelay(Duration.ofSeconds(5), 5))
            .build();

    @Singleton
    @Bean
    public DashscopeClient makeDashscopeClient(DashscopeClientConfig config, OkHttpClient http) {
        return DashscopeClient.newBuilder()
                .ak(config.ak())
                .http(http)
                .interceptors(List.of(retryInterceptor))
                .build();
    }

}
