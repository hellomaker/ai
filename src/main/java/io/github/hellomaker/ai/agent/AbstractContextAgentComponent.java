package io.github.hellomaker.ai.agent;

public abstract class AbstractContextAgentComponent<IN, OUT> extends AbstractAgentComponent<ComponentContext<IN>, ComponentContext<OUT>> {

    public AbstractContextAgentComponent(String componentKey) {
        super(componentKey);
    }

    @Override
    protected ComponentContext<OUT> doChainInternal(ComponentContext<IN> input) {
        OUT out = doChainOnContext(input);
        ComponentContext<OUT> outComponentContext = componentContext(input, out);
        outComponentContext.putExecuteMap(componentKey(), out);
        return outComponentContext;
    }

    @Override
    public ComponentContext<OUT> handelException(Exception e, ComponentContext<IN> input) {
        OUT out = onException(e, input);
        return componentContext(input, out);
    }

    public abstract OUT doChainOnContext(ComponentContext<IN> input);

    protected abstract OUT onException(Exception e, ComponentContext<IN> input);

    protected abstract ComponentContext<OUT> componentContext(ComponentContext<IN> input, OUT output);

}
