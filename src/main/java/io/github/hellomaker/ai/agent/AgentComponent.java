package io.github.hellomaker.ai.agent;


/**
 * ai agent 组件
 * @author xianzhikun
 */
public interface AgentComponent<IN, OUT> {

    OUT doChain(IN input);

    String componentKey();

}