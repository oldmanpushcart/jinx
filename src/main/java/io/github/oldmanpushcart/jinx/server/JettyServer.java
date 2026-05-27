package io.github.oldmanpushcart.jinx.server;

import io.github.oldmanpushcart.jinx.config.ServerConfig;
import io.github.oldmanpushcart.jinx.core.CommandRouter;
import jakarta.servlet.ServletContext;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyServer {
    
    private static final Logger log = LoggerFactory.getLogger(JettyServer.class);
    
    private Server server;
    private final CommandRouter commandRouter;
    
    public JettyServer(CommandRouter commandRouter) {
        this.commandRouter = commandRouter;
    }
    
    public void start(ServerConfig config) throws Exception {
        log.info("启动 Jetty 服务器，端口: {}, 上下文: {}", config.port(), config.contextPath());
        
        server = new Server(config.port());
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        ServletHolder servletHolder = new ServletHolder("jinx-servlet", 
            io.github.oldmanpushcart.jinx.servlet.JinxServlet.class);
        context.addServlet(servletHolder, config.contextPath() + "/*");
        
        ServletContext servletContext = context.getServletContext();
        servletContext.setAttribute("commandRouter", commandRouter);
        
        server.setHandler(context);
        
        server.start();
        log.info("Jetty 服务器启动成功");
    }
    
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            log.info("Jetty 服务器已停止");
        }
    }
}
