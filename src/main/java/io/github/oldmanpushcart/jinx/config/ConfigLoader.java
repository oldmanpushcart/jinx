package io.github.oldmanpushcart.jinx.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ConfigLoader {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final ObjectMapper objectMapper = createObjectMapper();
    
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        
        // 支持 kebab-case 命名（如 context-path -> contextPath）
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        
        // 支持 record 和构造器参数名称解析（需要 -parameters 编译选项）
        mapper.registerModule(new ParameterNamesModule());
        
        return mapper;
    }
    
    public static Config load(String configPath) throws IOException {
        log.info("加载配置文件: {}", configPath);
        File configFile = new File(configPath);
        
        if (!configFile.exists()) {
            throw new IOException("配置文件不存在: " + configPath);
        }
        
        Config config = objectMapper.readValue(configFile, Config.class);
        log.info("配置文件加载成功");
        
        return config;
    }
}
