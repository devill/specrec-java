package com.specrec;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;

public class CallLoggerProxy implements InvocationHandler, IConstructorCalledWith {
    private final Object target;
    private final CallLogger logger;
    private final String emoji;
    private final Class<?> explicitInterfaceType;

    private CallLoggerProxy(Object target, CallLogger logger, String emoji, Class<?> explicitInterfaceType) {
        this.target = target;
        this.logger = logger;
        this.emoji = emoji != null ? emoji : "";
        this.explicitInterfaceType = explicitInterfaceType;
    }

    @Override
    public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
        setupContextForTarget(parameters);
        notifyTargetOfConstructorCall(parameters);
        String interfaceName = determineInterfaceName();
        logConstructorCall(interfaceName, parameters);
        CallLogFormatterContext.clearCurrentLogger();
    }

    private void setupContextForTarget(ConstructorParameterInfo[] parameters) {
        CallLogger contextLogger = new CallLogger(logger.specBook, emoji);
        CallLogFormatterContext.setCurrentLogger(contextLogger);
        CallLogFormatterContext.setCurrentMethodName("constructorCalledWith");
    }

    private void notifyTargetOfConstructorCall(ConstructorParameterInfo[] parameters) {
        if (target instanceof IConstructorCalledWith) {
            ((IConstructorCalledWith) target).constructorCalledWith(parameters);
        }
    }

    private String determineInterfaceName() {
        return explicitInterfaceType.getSimpleName();
    }

    private String findMainInterface() {
        Class<?>[] interfaces = target.getClass().getInterfaces();
        
        // Find the interface with the most methods (most specific)
        Class<?> mainInterface = null;
        int maxMethods = 0;
        
        for (Class<?> iface : interfaces) {
            // Skip marker interfaces (interfaces with no methods or only marker methods)
            if (isMarkerInterface(iface)) {
                continue;
            }
            
            int methodCount = iface.getDeclaredMethods().length;
            if (methodCount > maxMethods) {
                maxMethods = methodCount;
                mainInterface = iface;
            }
        }
        
        if (mainInterface != null) {
            return mainInterface.getSimpleName();
        }
        
        return target.getClass().getSimpleName();
    }
    
    private boolean isMarkerInterface(Class<?> iface) {
        // IConstructorCalledWith is a marker interface for our framework
        if (iface.equals(IConstructorCalledWith.class)) {
            return true;
        }
        
        // Consider interfaces with no methods as marker interfaces
        return iface.getDeclaredMethods().length == 0;
    }
    
    // Utility method to find the main interface from a class
    public static String findMainInterfaceFromClass(Class<?> clazz) {
        Class<?>[] interfaces = clazz.getInterfaces();
        
        // Find the interface with the most methods (most specific)
        Class<?> mainInterface = null;
        int maxMethods = 0;
        
        for (Class<?> iface : interfaces) {
            // Skip marker interfaces
            if (isMarkerInterfaceStatic(iface)) {
                continue;
            }
            
            int methodCount = iface.getDeclaredMethods().length;
            if (methodCount > maxMethods) {
                maxMethods = methodCount;
                mainInterface = iface;
            }
        }
        
        if (mainInterface != null) {
            return mainInterface.getSimpleName();
        }
        
        return clazz.getSimpleName();
    }
    
    private static boolean isMarkerInterfaceStatic(Class<?> iface) {
        // IConstructorCalledWith is a marker interface for our framework
        if (iface.equals(IConstructorCalledWith.class)) {
            return true;
        }
        
        // Consider interfaces with no methods as marker interfaces
        return iface.getDeclaredMethods().length == 0;
    }

    private void logConstructorCall(String interfaceName, ConstructorParameterInfo[] parameters) {
        // Use the context logger that may have been modified by the target's constructorCalledWith() method
        CallLogger contextLogger = CallLogFormatterContext.getCurrentLogger();
        if (contextLogger != null) {
            contextLogger.forInterface(interfaceName);
            addConstructorArguments(contextLogger, parameters);
            contextLogger.log("constructorCalledWith");
        } else {
            // Fallback to creating a new logger if context is not available
            CallLogger callLogger = new CallLogger(logger.specBook, emoji);
            callLogger.forInterface(interfaceName);
            addConstructorArguments(callLogger, parameters);
            callLogger.log("constructorCalledWith");
        }
    }

    private void addConstructorArguments(CallLogger callLogger, ConstructorParameterInfo[] parameters) {
        if (parameters == null) return;

        String[] constructorArgNames = CallLogFormatterContext.getConstructorArgumentNames();
        for (int i = 0; i < parameters.length; i++) {
            // Check if this argument should be ignored (same logic as method arguments)
            if (shouldIgnoreArgument(callLogger, "constructorCalledWith", i)) {
                continue;
            }
            
            ConstructorParameterInfo parameter = parameters[i];
            String argName = getArgumentName(constructorArgNames, i, parameter.getName());
            callLogger.withArgument(parameter.getValue(), argName);
        }
    }

    private String getArgumentName(String[] constructorArgNames, int index, String actualParameterName) {
        if (constructorArgNames != null && index < constructorArgNames.length) {
            return constructorArgNames[index];
        }
        return actualParameterName != null ? actualParameterName : "Arg" + index;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        
        // Handle constructorCalledWith specially
        if ("constructorCalledWith".equals(methodName) && 
            args != null && args.length == 1 && args[0] instanceof ConstructorParameterInfo[]) {
            constructorCalledWith((ConstructorParameterInfo[]) args[0]);
            return null;
        }
        
        CallLogger callLogger = createCallLogger();
        
        setupLoggingContext(callLogger, methodName);

        Object result = null;
        Exception caughtException = null;
        
        try {
            result = invokeTargetMethod(method, args);
        } catch (Exception ex) {
            caughtException = ex;
            handleMethodException(callLogger, ex, methodName);
            throw ex;
        }

        if (shouldIgnoreCall(callLogger, methodName)) {
            CallLogFormatterContext.clearCurrentLogger();
            return result;
        }

        logMethodCall(callLogger, method, args, result, methodName);
        
        CallLogFormatterContext.clearCurrentLogger();
        return result;
    }

    private CallLogger createCallLogger() {
        return new CallLogger(new StringBuilder(), emoji);
    }

    private void setupLoggingContext(CallLogger callLogger, String methodName) {
        CallLogFormatterContext.setCurrentLogger(callLogger);
        CallLogFormatterContext.setCurrentMethodName(methodName);
    }

    private Object invokeTargetMethod(Method method, Object[] args) throws Exception {
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception) {
                throw (Exception) ex.getCause();
            } else if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            } else if (ex.getCause() instanceof Error) {
                throw (Error) ex.getCause();
            } else {
                throw new RuntimeException("Unexpected exception type", ex.getCause());
            }
        }
    }

    private void handleMethodException(CallLogger callLogger, Exception ex, String methodName) {
        callLogger.withNote("Exception: " + ex.getMessage());
        callLogger.log(methodName);
        logger.specBook.append(callLogger.specBook.toString());
        CallLogFormatterContext.clearCurrentLogger();
    }

    private boolean shouldIgnoreCall(CallLogger callLogger, String methodName) {
        return callLogger.ignoredCalls.contains(methodName);
    }

    private void logMethodCall(CallLogger callLogger, Method method, Object[] args, Object result, String methodName) {
        logInputArguments(callLogger, method, args, methodName);
        logReturnValue(callLogger, result, methodName);
        logOutputParameters(callLogger, method, args);
        
        callLogger.log(methodName);
        logger.specBook.append(callLogger.specBook.toString());
    }

    private void logInputArguments(CallLogger callLogger, Method method, Object[] args, String methodName) {
        if (args == null || callLogger.ignoredAllArguments.contains(methodName)) {
            return;
        }

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < args.length && i < parameters.length; i++) {
            if (shouldIgnoreArgument(callLogger, methodName, i)) {
                continue;
            }

            logSingleArgument(callLogger, parameters[i], args[i]);
        }
    }

    private boolean shouldIgnoreArgument(CallLogger callLogger, String methodName, int argumentIndex) {
        return callLogger.ignoredArguments.containsKey(methodName) &&
               callLogger.ignoredArguments.get(methodName).contains(argumentIndex);
    }

    private void logSingleArgument(CallLogger callLogger, Parameter parameter, Object argumentValue) {
        // Java doesn't have ref/out parameters like C#, so we just log the value
        callLogger.withArgument(argumentValue, parameter.getName());
    }

    private void logReturnValue(CallLogger callLogger, Object result, String methodName) {
        if (result != null && !callLogger.ignoredReturnValues.contains(methodName)) {
            callLogger.withReturn(result);
        }
    }

    private void logOutputParameters(CallLogger callLogger, Method method, Object[] args) {
        // Java doesn't have out parameters like C#, so this is a no-op
        // In Java, modifications to object references would be reflected in the original objects
    }

    @SuppressWarnings("unchecked")
    public static <I> I create(Class<I> interfaceType, I target, CallLogger logger, String emoji) {
        Class<?> targetClass = target.getClass();
        Class<?>[] targetInterfaces = targetClass.getInterfaces();
        
        if (targetInterfaces.length == 0) {
            throw new IllegalArgumentException("Target must implement at least one interface to be proxied");
        }

        // Always include IConstructorCalledWith in the proxy interfaces
        boolean hasIConstructorCalledWith = false;
        for (Class<?> iface : targetInterfaces) {
            if (iface == IConstructorCalledWith.class) {
                hasIConstructorCalledWith = true;
                break;
            }
        }
        
        Class<?>[] proxyInterfaces;
        if (hasIConstructorCalledWith) {
            proxyInterfaces = targetInterfaces;
        } else {
            proxyInterfaces = new Class<?>[targetInterfaces.length + 1];
            System.arraycopy(targetInterfaces, 0, proxyInterfaces, 0, targetInterfaces.length);
            proxyInterfaces[targetInterfaces.length] = IConstructorCalledWith.class;
        }

        CallLoggerProxy handler = new CallLoggerProxy(target, logger, emoji, interfaceType);
        
        return (I) Proxy.newProxyInstance(
            targetClass.getClassLoader(),
            proxyInterfaces,
            handler
        );
    }
}