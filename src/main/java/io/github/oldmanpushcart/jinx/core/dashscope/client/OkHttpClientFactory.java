package io.github.oldmanpushcart.jinx.core.dashscope.client;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;

@Factory
public class OkHttpClientFactory {

    @Singleton
    @Bean
    public OkHttpClient makeOkHttpClient(DashscopeClientConfig.HttpClientConfig config) {
        return new OkHttpClient.Builder()
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .writeTimeout(config.writeTimeout())
                .build();
    }

}
