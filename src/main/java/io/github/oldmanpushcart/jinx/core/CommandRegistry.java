package io.github.oldmanpushcart.jinx.core;

import io.github.oldmanpushcart.jinx.annotation.Command;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(CommandRegistry.class);
    
    private final Map<String, CommandHandler> handlerMap = new HashMap<>();
    
    public void scanAndRegister(String basePackage) {
        log.info("扫描命令处理器，包路径: {}", basePackage);
        
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> commandClasses = reflections.getTypesAnnotatedWith(Command.class);
        
        for (Class<?> clazz : commandClasses) {
            if (!CommandHandler.class.isAssignableFrom(clazz)) {
                log.warn("类 {} 标注了 @Command 但未实现 CommandHandler 接口", clazz.getName());
                continue;
            }
            
            try {
                Command command = clazz.getAnnotation(Command.class);
                String path = command.path();
                
                CommandHandler handler = (CommandHandler) clazz.getDeclaredConstructor().newInstance();
                handlerMap.put(path, handler);
                
                log.info("注册命令: {} -> {}", path, clazz.getSimpleName());
                
            } catch (Exception e) {
                log.error("注册命令失败: {}", clazz.getName(), e);
            }
        }
        
        log.info("命令注册完成，共注册 {} 个命令", handlerMap.size());
    }
    
    public CommandHandler getHandler(String path) {
        return handlerMap.get(path);
    }
    
    public boolean hasHandler(String path) {
        return handlerMap.containsKey(path);
    }
}
