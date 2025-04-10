package io.github.hellomaker.ai.agent;

public class ContextAgentComponent<IN, OUT> extends AbstractAgentComponent<InputWithContext<IN>, OUT> {


    @Override
    public OUT doChainInternal(InputWithContext<IN> input) {
        return null;
    }

    @Override
    public OUT exception(Exception e, InputWithContext<IN> input) {
        return null;
    }
}
