package io.github.oldmanpushcart.jinx.handler;

import io.github.oldmanpushcart.jinx.annotation.Command;
import io.github.oldmanpushcart.jinx.core.CommandHandler;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

@Command(path = "help", description = "帮助信息")
public class HelpCommandHandler implements CommandHandler {
    
    private static final Logger log = LoggerFactory.getLogger(HelpCommandHandler.class);
    
    @Override
    public Publisher<String> handle(Map<String, String> params) {
        log.info("收到help命令请求");
        
        String helpText = "可用命令：\n" +
                         "- help: 显示帮助信息\n" +
                         "- chat: 智能对话";
        
        return Mono.just(helpText);
    }
}
