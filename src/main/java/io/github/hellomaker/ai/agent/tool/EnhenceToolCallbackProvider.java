package io.github.hellomaker.ai.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.util.ToolUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnhenceToolCallbackProvider implements ToolCallbackProvider {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallbackProvider.class);
    private final List<Object> toolObjects;

    private EnhenceToolCallbackProvider(List<Object> toolObjects) {
        Assert.notNull(toolObjects, "toolObjects cannot be null");
        Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
        this.toolObjects = toolObjects;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return getToolCallBackWithMeta();
    }

    public ToolCallBackWithMeta[] getToolCallBackWithMeta() {
        ToolCallBackWithMeta[] toolCallbacks = (ToolCallBackWithMeta[])this.toolObjects.stream().map((toolObject) -> {
            return (ToolCallback[]) Stream.of(ReflectionUtils.getDeclaredMethods(toolObject.getClass())).filter((toolMethod) -> {
                return toolMethod.isAnnotationPresent(Tool.class);
            }).filter((toolMethod) -> {
                return !this.isFunctionalType(toolMethod);
            }).map((toolMethod) -> {
                ToolCallBackWithMeta toolCallBackWithMeta = ToolCallBackWithMeta.builder().toolDefinition(ToolDefinition.from(toolMethod)).toolMetadata(ToolMetadata.from(toolMethod)).toolMethod(toolMethod).toolObject(toolObject).toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod)).build();
//                ToolCallBackWithMeta toolCallBackWithMeta = new ToolCallBackWithMeta();
//                toolCallBackWithMeta.setToolCallback(delegateCallBack);
                if (toolMethod.isAnnotationPresent(DoubleCheckTool.class)) {
                    toolCallBackWithMeta.setDoubleCheck(true);
                }
                toolCallBackWithMeta.setMethod(toolMethod);
                toolCallBackWithMeta.setInstance(toolObject);
                return toolCallBackWithMeta;
            }).toArray((x$0) -> {
                return new ToolCallBackWithMeta[x$0];
            });
        }).flatMap(Stream::of).toArray((x$0) -> {
            return new ToolCallBackWithMeta[x$0];
        });
        this.validateToolCallbacks(toolCallbacks);
        return toolCallbacks;
    }

    private boolean isFunctionalType(Method toolMethod) {
        boolean isFunction = ClassUtils.isAssignable(toolMethod.getReturnType(), Function.class) || ClassUtils.isAssignable(toolMethod.getReturnType(), Supplier.class) || ClassUtils.isAssignable(toolMethod.getReturnType(), Consumer.class);
        if (isFunction) {
            logger.warn("Method {} is annotated with @Tool but returns a functional type. This is not supported and the method will be ignored.", toolMethod.getName());
        }

        return isFunction;
    }

    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException("Multiple tools with the same name (%s) found in sources: %s".formatted(String.join(", ", duplicateToolNames), this.toolObjects.stream().map((o) -> {
                return o.getClass().getName();
            }).collect(Collectors.joining(", "))));
        }
    }

    public static EnhenceToolCallbackProvider.Builder builder() {
        return new EnhenceToolCallbackProvider.Builder();
    }

    public static class Builder {
        private List<Object> toolObjects;

        private Builder() {
        }

        public EnhenceToolCallbackProvider.Builder toolObjects(Object... toolObjects) {
            Assert.notNull(toolObjects, "toolObjects cannot be null");
            this.toolObjects = Arrays.asList(toolObjects);
            return this;
        }

        public EnhenceToolCallbackProvider build() {
            return new EnhenceToolCallbackProvider(this.toolObjects);
        }
    }


}
