package io.github.oldmanpushcart.jinx.servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.jinx.core.CommandHandler;
import io.github.oldmanpushcart.jinx.core.CommandRouter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = "/api/v1/agent/*", loadOnStartup = 1)
public class JinxServlet extends HttpServlet {
    
    private static final Logger log = LoggerFactory.getLogger(JinxServlet.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private CommandRouter commandRouter;
    
    @Override
    public void init() throws ServletException {
        super.init();
        this.commandRouter = (CommandRouter) getServletContext().getAttribute("commandRouter");
        log.info("JinxServlet 初始化完成");
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.isEmpty()) {
                sendError(asyncContext, 400, "命令路径不能为空");
                return;
            }
            
            String commandPath = pathInfo.startsWith("/") ? 
                pathInfo.substring(1) : pathInfo;
            
            log.debug("收到命令请求: {}", commandPath);
            
            Map<String, String> params = parseRequestParams(req);
            
            CommandHandler handler = commandRouter.getHandler(commandPath);
            if (handler == null) {
                sendError(asyncContext, 404, "未找到命令: " + commandPath);
                return;
            }
            
            Publisher<String> publisher = handler.handle(params);
            
            subscribeAndWriteResponse(asyncContext, resp, publisher);
            
        } catch (Exception e) {
            log.error("处理请求异常", e);
            sendError(asyncContext, 500, "服务器内部错误: " + e.getMessage());
        }
    }
    
    private void subscribeAndWriteResponse(AsyncContext asyncContext, 
                                           HttpServletResponse resp,
                                           Publisher<String> publisher) {
        
        // 标记是否已经开始写入响应
        final boolean[] responseStarted = {false};
        
        PrintWriter writer;
        try {
            writer = resp.getWriter();
        } catch (IOException e) {
            log.error("获取Writer失败", e);
            asyncContext.complete();
            return;
        }
        
        Flux.from(publisher)
            .doOnSubscribe(subscription -> {
                // 订阅时设置成功状态码和Content-Type
                if (!resp.isCommitted()) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain;charset=utf-8");
                    responseStarted[0] = true;
                }
            })
            .subscribe(
                chunk -> {
                    try {
                        if (!resp.isCommitted()) {
                            writer.write(chunk);
                            writer.flush();
                        }
                    } catch (Exception e) {
                        log.error("写入响应失败", e);
                        // 如果写入失败，取消订阅
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    log.error("流式输出错误", error);
                    if (!resp.isCommitted()) {
                        // 响应未开始，返回标准HTTP错误
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.setContentType("application/json;charset=utf-8");
                        try {
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("success", false);
                            errorResponse.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            errorResponse.put("message", "处理失败: " + error.getMessage());
                            objectMapper.writeValue(writer, errorResponse);
                            writer.flush();
                        } catch (Exception e) {
                            log.error("写入错误响应失败", e);
                        }
                    } else {
                        // 响应已开始，只能追加错误信息
                        try {
                            writer.write("\n[ERROR] " + error.getMessage());
                            writer.flush();
                        } catch (Exception e) {
                            log.error("写入错误信息失败", e);
                        }
                    }
                    asyncContext.complete();
                },
                () -> {
                    log.debug("流式输出完成");
                    asyncContext.complete();
                }
            );
    }
    
    private Map<String, String> parseRequestParams(HttpServletRequest req) 
            throws IOException {
        
        Map<String, String> params = new HashMap<>();
        
        Enumeration<String> paramNames = req.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            params.put(name, req.getParameter(name));
        }
        
        String body = readRequestBody(req);
        if (body != null && !body.isEmpty()) {
            try {
                Map<String, Object> jsonMap = objectMapper.readValue(
                    body, 
                    new TypeReference<Map<String, Object>>() {}
                );
                
                jsonMap.forEach((k, v) -> {
                    params.put(k, v != null ? v.toString() : "");
                });
            } catch (Exception e) {
                log.warn("解析JSON失败，忽略Body参数", e);
            }
        }
        
        return params;
    }
    
    private String readRequestBody(HttpServletRequest request) throws IOException {
        return request.getReader().lines()
            .collect(Collectors.joining(System.lineSeparator()));
    }
    
    private void sendError(AsyncContext asyncContext, int code, String message) {
        try {
            HttpServletResponse resp = (HttpServletResponse) asyncContext.getResponse();
            resp.setStatus(code);
            resp.setContentType("application/json;charset=utf-8");
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("code", code);
            error.put("message", message);
            
            objectMapper.writeValue(resp.getWriter(), error);
        } catch (IOException e) {
            log.error("写入错误响应失败", e);
        } finally {
            asyncContext.complete();
        }
    }
}
