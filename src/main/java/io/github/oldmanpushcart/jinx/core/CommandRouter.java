package io.github.oldmanpushcart.jinx.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandRouter {
    
    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);
    
    private final CommandRegistry registry;
    
    public CommandRouter(CommandRegistry registry) {
        this.registry = registry;
    }
    
    public CommandHandler getHandler(String commandPath) {
        CommandHandler handler = registry.getHandler(commandPath);
        
        if (handler == null) {
            log.warn("未找到命令处理器: {}", commandPath);
        }
        
        return handler;
    }
}
