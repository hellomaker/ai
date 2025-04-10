package io.github.hellomaker.ai.agent;


public interface InputWithContext<IN> {

    IN getInput();

    ChatMemory chatMemory();

}
