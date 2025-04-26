package io.github.hellomaker.ai.agent.tool;

import io.github.hellomaker.ai.agent.context.ChatMemoryUtil;
import io.github.hellomaker.ai.common.ConfirmWordUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ToolCallConfirmManager {

    ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

    public boolean handlePotentialFunctionCall(
            ChatMemory chatMemory,
            String conversationId,
            UserMessage userMessage) {

        // 检查是否是确认消息
        if (pendingConfirmations.containsKey(conversationId)) {
            List<PendingFunctionCall> pendingCalls = pendingConfirmations.get(conversationId);
            if (pendingCalls.isEmpty()) {
                pendingConfirmations.remove(conversationId);
                return false;
            }
            if (ConfirmWordUtils.isSimpleConfirmMeaning(userMessage.getText())
                //TODO 语义判断
//                    || "是".equals(confirmAnalyzer.isConfirm(userMessage.singleText().trim()))
            ) {
                pendingConfirmations.remove(conversationId);
                for (PendingFunctionCall pendingCall : pendingCalls) {
                    ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(pendingCall.getPrompt(), pendingCall.getToolCallAssistantMessage());
                    Message message = toolExecutionResult.conversationHistory().getLast();

                    //使用自定义回复,注：这里可以根据execute 结果判断执行错误的情况
//                    chatMemory.add(conversationId, new UserMessage(userMessage.getText()));
                    chatMemory.add(conversationId, new AssistantMessage(pendingCall.getSuccessPrompt()));
                }
                return true;
            } else {
                pendingConfirmations.remove(conversationId);
//                chatMemory.add(AiMessage.from("取消或者修改"));
//                chatMemory.add(userMessage);

                ChatMemoryUtil.removeIfRecently(chatMemory, conversationId, 0, 1);
                return false;
            }
        }

        // 如果不是确认流程，正常处理消息
//        chatMemory.add(userMessage);
        return false;
    }

    public void registerPendingConfirmation(
            String conversationId,
            ChatResponse toolCallAssistantMessage,
            String successPrompt,
            Prompt prompt) {
        String success = "已为您" + successPrompt + "\n如果您还有其他问题，请直接提出。";
        pendingConfirmations.putIfAbsent(conversationId, new ArrayList<>());
        List<PendingFunctionCall> pendingFunctionCalls = pendingConfirmations.get(conversationId);
        pendingFunctionCalls.add(
                new ToolCallConfirmManager.PendingFunctionCall(toolCallAssistantMessage, success, prompt));
    }

    Map<String, List<PendingFunctionCall>> pendingConfirmations = new ConcurrentHashMap<>();

    private static class PendingFunctionCall {
        private final ChatResponse toolCallAssistantMessage;
        private final String successPrompt;
        private final Prompt prompt;

        public PendingFunctionCall(ChatResponse toolCallAssistantMessage, String successPrompt, Prompt prompt) {
            this.toolCallAssistantMessage = toolCallAssistantMessage;
            this.successPrompt = successPrompt;
            this.prompt = prompt;
        }

        public ChatResponse getToolCallAssistantMessage() {
            return toolCallAssistantMessage;
        }

        public String getSuccessPrompt() {
            return successPrompt;
        }

        public Prompt getPrompt() {
            return prompt;
        }
    }

}
