package io.github.hellomaker.ai.agent.context;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatMemoryUtil {

    public static void removeIfRecently(ChatMemory chatMemory, String conversationId, int startIndex, int size) {
        List<Message> messages = chatMemory.get(conversationId, Integer.MAX_VALUE);
        if (messages == null || messages.isEmpty()
                || messages.size() < startIndex + 1
                || startIndex < 0 || size <= 0
                || messages.size() < startIndex + size) {
            return;
        }

        List<Message> newMessages = new ArrayList<>(size);
        for (int i = 0; i < messages.size() - startIndex - size; i++) {
            newMessages.add(messages.get(i));
        }
        for (int i = messages.size() - startIndex - 1; i < size ; i++) {
            newMessages.add(messages.get(i));
        }
        chatMemory.clear(conversationId);
        chatMemory.add(conversationId, newMessages);
    }


}
