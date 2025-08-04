package com.specrec;

import java.util.HashSet;
import java.util.Set;

public class CallLogFormatterContext {
    private static final ThreadLocal<CallLogger> currentCallLogger = new ThreadLocal<>();
    private static final ThreadLocal<String> currentMethodName = new ThreadLocal<>();
    private static final ThreadLocal<String[]> constructorArgNames = new ThreadLocal<>();

    public static void setCurrentLogger(CallLogger logger) {
        currentCallLogger.set(logger);
    }

    public static void setCurrentMethodName(String methodName) {
        currentMethodName.set(methodName);
    }

    public static void clearCurrentLogger() {
        currentCallLogger.remove();
        currentMethodName.remove();
        constructorArgNames.remove();
    }

    public static void addNote(String note) {
        CallLogger logger = currentCallLogger.get();
        if (logger != null) {
            logger.withNote(note);
        }
    }

    public static void setConstructorArgumentNames(String... argumentNames) {
        constructorArgNames.set(argumentNames);
    }

    static String[] getConstructorArgumentNames() {
        return constructorArgNames.get();
    }

    public static void ignoreCall() {
        String methodName = currentMethodName.get();
        CallLogger logger = currentCallLogger.get();
        if (methodName != null && logger != null) {
            logger.ignoredCalls.add(methodName);
        }
    }

    public static void ignoreArgument(int argumentIndex) {
        String methodName = currentMethodName.get();
        CallLogger logger = currentCallLogger.get();
        if (methodName != null && logger != null) {
            logger.ignoredArguments.computeIfAbsent(methodName, k -> new HashSet<>()).add(argumentIndex);
        }
    }

    public static void ignoreAllArguments() {
        String methodName = currentMethodName.get();
        CallLogger logger = currentCallLogger.get();
        if (methodName != null && logger != null) {
            logger.ignoredAllArguments.add(methodName);
        }
    }

    public static void ignoreReturnValue() {
        String methodName = currentMethodName.get();
        CallLogger logger = currentCallLogger.get();
        if (methodName != null && logger != null) {
            logger.ignoredReturnValues.add(methodName);
        }
    }
}