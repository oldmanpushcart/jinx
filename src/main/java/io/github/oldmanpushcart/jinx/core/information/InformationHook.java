package io.github.oldmanpushcart.jinx.core.information;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.hook.PreparationHook;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.jinx.JinxConfig;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Singleton
public class InformationHook implements PreparationHook {

    private final ChatInterceptor settingInterceptor;

    public InformationHook(JinxConfig config) {
        this.settingInterceptor = new SettingInterceptor(Information.newInstant(config));
    }

    @Override
    public List<? extends ChatInterceptor> onPreparation(Agent agent) {
        return List.of(settingInterceptor);
    }

    private static class SettingInterceptor implements ChatInterceptor {

        private final Message informationMessage;

        public SettingInterceptor(Information information) {
            this.informationMessage = Message.system(PromptTemplate.newBuilder()
                    .template("""
                            ## 系统信息
                             - 操作系统：${os.name}
                             - 系统架构：${os.arch}
                            
                            ## JINX 目录结构
                            
                            ### 配置目录：${jinx.confspace}
                            存放 JINX 的配置文件（如 `application.yml`、`jinx.xml`、`logback.xml`）。
                            > ️此目录内容在运行期间为只读，修改后需重启服务才能生效。
                            
                            ### 数据目录：${jinx.dataspace}
                            存放 JINX 运行过程中产生和使用的持久化数据，包括技能、缓存、MCP元数据等。
                            > 此目录需要读写权限，程序运行期间会动态读写其中的内容。
                            
                            ### 工作目录：${jinx.workspace}
                            存放 JINX 工作过程中产出的临时文件和最终结果文件。
                            > 此目录内容可随时清理，不影响服务正常运行。
                            """)
                    .build()
                    .render(Map.of(
                            "os.name", information.computer().osName(),
                            "os.arch", information.computer().osArch(),
                            "jinx.confspace", information.jinx().confspace(),
                            "jinx.dataspace", information.jinx().dataspace(),
                            "jinx.workspace", information.jinx().workspace()
                    ))
            ).withCache();
        }

        @Override
        public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
            final var newRequest = AigcRequest.newBuilder(request)
                    .input(input -> Input.newBuilder(input)
                            .messages(messages -> {
                                messages.add(0, informationMessage);
                                return messages;
                            })
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

    }

}
