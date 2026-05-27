package io.github.oldmanpushcart.jinx;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import io.github.oldmanpushcart.jinx.config.Config;
import io.github.oldmanpushcart.jinx.config.ConfigLoader;
import io.github.oldmanpushcart.jinx.core.AgentService;
import io.github.oldmanpushcart.jinx.core.CommandRegistry;
import io.github.oldmanpushcart.jinx.core.CommandRouter;
import io.github.oldmanpushcart.jinx.server.JettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class JinxApplication {
    
    private static final Logger log = LoggerFactory.getLogger(JinxApplication.class);
    
    public static void main(String[] args) throws Exception {
        log.info("========================================");
        log.info("启动 Jinx Agent 服务端");
        log.info("========================================");
        
        Config config = ConfigLoader.load("conf/application.yaml");
        
        initLogging(config.logbackConfigPath());
        
        CommandRegistry registry = new CommandRegistry();
        registry.scanAndRegister("io.github.oldmanpushcart.jinx.handler");
        
        AgentService agentService = new AgentService();
        agentService.init();
        
        CommandRouter router = new CommandRouter(registry);
        
        JettyServer server = new JettyServer(router);
        server.start(config.server());
        
        log.info("========================================");
        log.info("Jinx Agent 服务端启动成功");
        log.info("监听地址: {}:{}", config.server().host(), config.server().port());
        log.info("API端点: {}", config.server().contextPath());
        log.info("========================================");
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
                log.info("Jinx Agent 服务端已关闭");
            } catch (Exception e) {
                log.error("关闭服务器时出错", e);
            }
        }));
    }
    
    private static void initLogging(String logbackConfigPath) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(new File(logbackConfigPath));
        } catch (Exception e) {
            System.err.println("初始化日志系统失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
