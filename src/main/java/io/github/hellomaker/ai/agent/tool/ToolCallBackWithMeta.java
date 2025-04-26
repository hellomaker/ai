package io.github.hellomaker.ai.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

public class ToolCallBackWithMeta implements ToolCallback, FunctionCallback {

    private static final Logger logger = LoggerFactory.getLogger(ToolCallBackWithMeta.class);


    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return toolMetadata;
    }


    private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

    private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

    private final ToolDefinition toolDefinition;

    private final ToolMetadata toolMetadata;

    private final Method toolMethod;

    @Nullable
    private final Object toolObject;

    private final ToolCallResultConverter toolCallResultConverter;

    public ToolCallBackWithMeta(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Method toolMethod,
                              @Nullable Object toolObject, @Nullable ToolCallResultConverter toolCallResultConverter) {
        Assert.notNull(toolDefinition, "toolDefinition cannot be null");
        Assert.notNull(toolMethod, "toolMethod cannot be null");
        Assert.isTrue(Modifier.isStatic(toolMethod.getModifiers()) || toolObject != null,
                "toolObject cannot be null for non-static methods");
        this.toolDefinition = toolDefinition;
        this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
        this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
                : DEFAULT_RESULT_CONVERTER;
    }


    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        Assert.hasText(toolInput, "toolInput cannot be null or empty");

        logger.debug("Starting execution of tool: {}", toolDefinition.name());

        validateToolContextSupport(toolContext);

        Map<String, Object> toolArguments = extractToolArguments(toolInput);

        Object[] methodArguments = buildMethodArguments(toolArguments, toolContext);

        Object result = callMethod(methodArguments);

        logger.debug("Successful execution of tool: {}", toolDefinition.name());

        Type returnType = toolMethod.getGenericReturnType();

//        return toolCallResultConverter.convert(result, returnType);
        return JSON.toJSONString(result);
    }

    private void validateToolContextSupport(@Nullable ToolContext toolContext) {
        var isToolContextRequired = toolContext != null && !CollectionUtils.isEmpty(toolContext.getContext());
        var isToolContextAcceptedByMethod = Stream.of(toolMethod.getParameterTypes())
                .anyMatch(type -> ClassUtils.isAssignable(type, ToolContext.class));
        if (isToolContextRequired && !isToolContextAcceptedByMethod) {
            throw new IllegalArgumentException("ToolContext is not supported by the method as an argument");
        }
    }

    private Map<String, Object> extractToolArguments(String toolInput) {
        return JsonParser.fromJson(toolInput, new TypeReference<>() {
        });
    }

    // Based on the implementation in MethodInvokingFunctionCallback.
    private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, @Nullable ToolContext toolContext) {
        return Stream.of(toolMethod.getParameters()).map(parameter -> {
            if (parameter.getType().isAssignableFrom(ToolContext.class)) {
                return toolContext;
            }
            Object rawArgument = toolInputArguments.get(parameter.getName());

            return buildTypedArgument(rawArgument, parameter);
        }).toArray();
    }

    @Nullable
    private Object buildTypedArgument(@Nullable Object value, Parameter parameter) {
        if (value == null) {
            return null;
        }

        Object o = coerceArgument(value, parameter.getName(), parameter.getType(), parameter.getParameterizedType());

//        return JsonParser.toTypedObject(value, type);
        return o;
    }

    static Object coerceArgument(Object argument, String parameterName, Class<?> parameterClass, Type parameterType) {
        if (argument == null) {
            return null;
        }
        if (parameterClass == String.class) {
            return argument.toString();
        }

        // TODO handle enum and collection of enums (e.g. wrong case, etc)
        if (parameterClass.isEnum()) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Class<Enum> enumClass = (Class<Enum>) parameterClass;
                try {
                    return Enum.valueOf(
                            enumClass, Objects.requireNonNull(argument).toString());
                } catch (IllegalArgumentException e) {
                    // try to convert to uppercase as a last resort
                    return Enum.valueOf(
                            enumClass,
                            Objects.requireNonNull(argument).toString().toUpperCase());
                }
            } catch (Exception | Error e) {
                throw new IllegalArgumentException(
                        String.format(
                                "Argument \"%s\" is not a valid enum value for %s: <%s>",
                                parameterName, parameterClass.getName(), argument),
                        e);
            }
        }

        if (parameterClass == Boolean.class || parameterClass == boolean.class) {
            if (argument instanceof Boolean) {
                return argument;
            }
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is not convertable to %s, got %s: <%s>",
                    parameterName, parameterClass.getName(), argument.getClass().getName(), argument));
        }

        if (parameterClass == Double.class || parameterClass == double.class) {
            return getDoubleValue(argument, parameterName, parameterClass);
        }

        if (parameterClass == Float.class || parameterClass == float.class) {
            double doubleValue = getDoubleValue(argument, parameterName, parameterClass);
            checkBounds(doubleValue, parameterName, parameterClass, -Float.MIN_VALUE, Float.MAX_VALUE);
            return (float) doubleValue;
        }

        if (parameterClass == BigDecimal.class) {
            return BigDecimal.valueOf(getDoubleValue(argument, parameterName, parameterClass));
        }

        if (parameterClass == Integer.class || parameterClass == int.class) {
            return (int)
                    getBoundedLongValue(argument, parameterName, parameterClass, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        if (parameterClass == Long.class || parameterClass == long.class) {
            return getBoundedLongValue(argument, parameterName, parameterClass, Long.MIN_VALUE, Long.MAX_VALUE);
        }

        if (parameterClass == Short.class || parameterClass == short.class) {
            return (short)
                    getBoundedLongValue(argument, parameterName, parameterClass, Short.MIN_VALUE, Short.MAX_VALUE);
        }

        if (parameterClass == Byte.class || parameterClass == byte.class) {
            return (byte) getBoundedLongValue(argument, parameterName, parameterClass, Byte.MIN_VALUE, Byte.MAX_VALUE);
        }

        if (parameterClass == BigInteger.class) {
            return BigDecimal.valueOf(getNonFractionalDoubleValue(argument, parameterName, parameterClass))
                    .toBigInteger();
        }

        if (parameterClass.isArray() && argument instanceof Collection) {
            Class<?> type = parameterClass.getComponentType();
            if (type == String.class) {
                return ((Collection<String>) argument).toArray(new String[0]);
            }
            // TODO: Consider full type coverage.
        }

        if (Collection.class.isAssignableFrom(parameterClass) || Map.class.isAssignableFrom(parameterClass)) {
            // Conversion to JSON and back is required when parameterType is a POJO
            return JSON.parseObject(JSON.toJSONString(argument), parameterType);
        }

        if (parameterClass == UUID.class) {
            return UUID.fromString(argument.toString());
        }

        if (argument instanceof String) {
            return JSON.parseObject(argument.toString(), parameterClass);
        } else {
            // Conversion to JSON and back is required when parameterClass is a POJO
//            return Json.fromJson(Json.toJson(argument), parameterClass);

            return JSON.parseObject(JSON.toJSONString(argument), parameterClass);
        }
    }

    private static double getDoubleValue(Object argument, String parameterName, Class<?> parameterType) {
        if (argument instanceof String) {
            try {
                return Double.parseDouble(argument.toString());
            } catch (Exception e) {
                // nothing, will be handled with bellow code
            }
        }
        if (!(argument instanceof Number)) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is not convertable to %s, got %s: <%s>",
                    parameterName, parameterType.getName(), argument.getClass().getName(), argument));
        }
        return ((Number) argument).doubleValue();
    }

    private static double getNonFractionalDoubleValue(Object argument, String parameterName, Class<?> parameterType) {
        double doubleValue = getDoubleValue(argument, parameterName, parameterType);
        if (!hasNoFractionalPart(doubleValue)) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" has non-integer value for %s: <%s>",
                    parameterName, parameterType.getName(), argument));
        }
        return doubleValue;
    }

    private static void checkBounds(
            double doubleValue, String parameterName, Class<?> parameterType, double minValue, double maxValue) {
        if (doubleValue < minValue || doubleValue > maxValue) {
            throw new IllegalArgumentException(String.format(
                    "Argument \"%s\" is out of range for %s: <%s>",
                    parameterName, parameterType.getName(), doubleValue));
        }
    }

    private static long getBoundedLongValue(
            Object argument, String parameterName, Class<?> parameterType, long minValue, long maxValue) {
        double doubleValue = getNonFractionalDoubleValue(argument, parameterName, parameterType);
        checkBounds(doubleValue, parameterName, parameterType, minValue, maxValue);
        return (long) doubleValue;
    }

    static boolean hasNoFractionalPart(Double doubleValue) {
        return doubleValue.equals(Math.floor(doubleValue));
    }

    @Nullable
    private Object callMethod(Object[] methodArguments) {
        if (isObjectNotPublic() || isMethodNotPublic()) {
            toolMethod.setAccessible(true);
        }

        Object result;
        try {
            result = toolMethod.invoke(toolObject, methodArguments);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
        }
        catch (InvocationTargetException ex) {
            throw new ToolExecutionException(toolDefinition, ex.getCause());
        }
        return result;
    }

    private boolean isObjectNotPublic() {
        return toolObject != null && !Modifier.isPublic(toolObject.getClass().getModifiers());
    }

    private boolean isMethodNotPublic() {
        return !Modifier.isPublic(toolMethod.getModifiers());
    }

    @Override
    public String toString() {
        return "MethodToolCallback{" + "toolDefinition=" + toolDefinition + ", toolMetadata=" + toolMetadata + '}';
    }

    public static ToolCallBackWithMeta.Builder builder() {
        return new ToolCallBackWithMeta.Builder();
    }

    public static class Builder {

        private ToolDefinition toolDefinition;

        private ToolMetadata toolMetadata;

        private Method toolMethod;

        private Object toolObject;

        private ToolCallResultConverter toolCallResultConverter;

        private Builder() {
        }

        public ToolCallBackWithMeta.Builder toolDefinition(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
            return this;
        }

        public ToolCallBackWithMeta.Builder toolMetadata(ToolMetadata toolMetadata) {
            this.toolMetadata = toolMetadata;
            return this;
        }

        public ToolCallBackWithMeta.Builder toolMethod(Method toolMethod) {
            this.toolMethod = toolMethod;
            return this;
        }

        public ToolCallBackWithMeta.Builder toolObject(Object toolObject) {
            this.toolObject = toolObject;
            return this;
        }

        public ToolCallBackWithMeta.Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
            this.toolCallResultConverter = toolCallResultConverter;
            return this;
        }

        public ToolCallBackWithMeta build() {
            return new ToolCallBackWithMeta(toolDefinition, toolMetadata, toolMethod, toolObject,
                    toolCallResultConverter);
        }

    }

    Method method;
    Object instance;
    boolean isDoubleCheck;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public boolean isDoubleCheck() {
        return isDoubleCheck;
    }

    public void setDoubleCheck(boolean doubleCheck) {
        isDoubleCheck = doubleCheck;
    }
}
