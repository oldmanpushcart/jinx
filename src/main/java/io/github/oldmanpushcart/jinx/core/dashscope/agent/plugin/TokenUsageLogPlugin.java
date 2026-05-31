package io.github.oldmanpushcart.jinx.core.dashscope.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;


public class TokenUsageLogPlugin implements Plugin {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageLogPlugin.class);
    private static final ChatInterceptor logInterceptor = new LogInterceptor();
    private static final ChatInterceptor collectInterceptor = new CollectInterceptor();

    @Override
    public CompletionStage<Extension> install(Agent agent) {
        return CompletableFuture.completedStage(new Extension() {
            @Override
            public Plugin plugin() {
                return TokenUsageLogPlugin.this;
            }

            @Override
            public List<ChatInterceptor> interceptors(Phases phases) {
                return switch (phases) {
                    case PREPARATION -> List.of(logInterceptor, collectInterceptor);
                    case INTERACTION -> List.of(collectInterceptor);
                };
            }
        });
    }

    @Override
    public CompletionStage<Void> uninstall() {
        return CompletableFuture.completedStage(null);
    }

    static class LogInterceptor implements ChatInterceptor {

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

            final var sessionIdObject = request.context().get("SESSION-ID");
            if (!(sessionIdObject instanceof String sessionId)) {
                return chain.proceed(request);
            }

            final var key = "USAGES$" + sessionId;
            final var usages = Collections.synchronizedList(new ArrayList<Usage>());
            request.context().put(key, usages);

            return chain.proceed(request)
                    .thenApply(r ->
                            switch (chain.type()) {
                                case ASYNC -> {
                                    log(sessionId, usages);
                                    yield r;
                                }
                                case FLOW -> {
                                    //noinspection unchecked
                                    final var publisher = (Publisher<AigcResponse<Output>>) r;
                                    yield Flux.from(publisher)
                                            .doOnComplete(() -> log(sessionId, usages))
                                            ;
                                }
                                case TASK -> r;
                            });
        }

        private static void log(String sessionId, List<Usage> usages) {
            final var tokenUsage = usages.stream()
                    .map(TokenUsage::of)
                    .reduce(TokenUsage::accumulate)
                    .orElse(null);
            if (null == tokenUsage) {
                return;
            }
            log.info("jinx://api/chat/{}/tokens total={};input={};output={};cache_cached={};cache_creation={};cache_rate={};",
                    sessionId,
                    tokenUsage.total(),
                    tokenUsage.input(),
                    tokenUsage.output(),
                    tokenUsage.cache().cached(),
                    tokenUsage.cache().creation(),
                    tokenUsage.cache().rateFormatted()
            );
        }

    }

    static class CollectInterceptor implements ChatInterceptor {

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

            final var sessionIdObject = request.context().get("SESSION-ID");
            if (!(sessionIdObject instanceof String sessionId)) {
                return chain.proceed(request);
            }

            final var key = "USAGES$" + sessionId;
            //noinspection unchecked
            final var usages = (List<Usage>) request.context().get(key);
            if (null == usages) {
                return chain.proceed(request);
            }

            return chain.proceed(request)
                    .thenApply(r ->
                            switch (chain.type()) {
                                case ASYNC -> {
                                    //noinspection unchecked
                                    final var response = (AigcResponse<Output>) r;
                                    if (null != response.usage()) {
                                        usages.add(response.usage());
                                    }
                                    yield r;
                                }
                                case FLOW -> {
                                    //noinspection unchecked
                                    final var publisher = (Publisher<AigcResponse<Output>>) r;
                                    final var responseRef = new AtomicReference<AigcResponse<Output>>();
                                    yield Flux.from(publisher)
                                            .doOnNext(responseRef::set)
                                            .doOnComplete(() -> {
                                                final var response = responseRef.get();
                                                if (null != response && null != response.usage()) {
                                                    usages.add(response.usage());
                                                }
                                            });
                                }
                                case TASK -> r;
                            });
        }

    }

    /**
     * TOKEN 统计
     *
     * @param total  总数
     * @param input  输入
     * @param output 输出
     * @param cache  缓存
     */
    private record TokenUsage(
            int total,
            int input,
            int output,
            Cache cache
    ) implements Accumulator<TokenUsage> {

        @Override
        public TokenUsage accumulate(TokenUsage next) {
            if (null == next) {
                return this;
            }

            final var _total = total() + next.total();
            final var _cached = cache().cached() + next.cache().cached();
            return new TokenUsage(
                    _total,
                    input() + next.input(),
                    output() + next.output(),
                    new Cache(
                            cache().creation() + next.cache().creation(),
                            _cached,
                            (float) _cached / _total
                    )
            );
        }

        /**
         * 缓存
         *
         * @param creation 创建
         * @param cached   命中
         * @param rate     命中率
         */
        public record Cache(
                int creation,
                int cached,
                float rate
        ) {

            public String rateFormatted() {
                return String.format("%.2f%%", rate() * 100);
            }

        }


        /**
         * 根据{@link Usage}创建统计
         *
         * @param usage 使用情况
         * @return 统计
         */
        public static TokenUsage of(Usage usage) {
            final var total = usage.total(item -> "total_tokens".equals(item.name()));
            final var input = usage.total(item -> "input_tokens".equals(item.name()));
            final var output = usage.total(item -> "output_tokens".equals(item.name()));

            Cache cache;
            final var promptTokensDetailsUsage = usage.children().get("prompt_tokens_details");
            if (null != promptTokensDetailsUsage) {
                final var cached = promptTokensDetailsUsage.total(item -> "cached_tokens".equals(item.name()));
                final var creation = promptTokensDetailsUsage.total(item -> "cache_creation_input_tokens".equals(item.name()));
                final var rate = cached / (float) total;
                cache = new Cache(creation, cached, rate);
            } else {
                cache = new Cache(0, 0, 0f);
            }

            return new TokenUsage(total, input, output, cache);
        }

    }


    @Factory
    static class MakeFactory {

        @Singleton
        public Plugin makeTokenUsageLogPlugin() {
            return new TokenUsageLogPlugin();
        }

    }

}
