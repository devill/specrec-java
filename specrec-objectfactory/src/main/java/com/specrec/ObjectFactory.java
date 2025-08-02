package com.specrec;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ObjectFactory {
    private static volatile ObjectFactory instance;
    private static final Object lock = new Object();

    private final Map<Class<?>, Queue<Object>> queuedObjects = new HashMap<>();
    private final Map<Class<?>, Object> alwaysObjects = new HashMap<>();

    public ObjectFactory() {
    }

    public static ObjectFactory getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ObjectFactory();
                }
            }
        }
        return instance;
    }

    public <T> T create(Class<T> type, Object... args) {
        return create(type, type, args);
    }

    public <I, T extends I> I create(Class<I> interfaceType, Class<T> implementationType, Object... args) {
        I obj = fetchObject(interfaceType, implementationType, args);
        logConstructorCall(obj, args);
        return obj;
    }

    @SuppressWarnings("unchecked")
    private <I, T extends I> I fetchObject(Class<I> interfaceType, Class<T> implementationType, Object... args) {
        if (queuedObjects.containsKey(interfaceType) && !queuedObjects.get(interfaceType).isEmpty()) {
            return (I) queuedObjects.get(interfaceType).poll();
        }

        if (alwaysObjects.containsKey(interfaceType)) {
            return (I) alwaysObjects.get(interfaceType);
        }

        return createInstance(implementationType, args);
    }

    private static void logConstructorCall(Object obj, Object[] args) {
        if (obj instanceof IConstructorCalledWith) {
            ((IConstructorCalledWith) obj).constructorCalledWith(args);
        }
    }

    public <T> void setOne(Class<T> type, T obj) {
        queuedObjects.computeIfAbsent(type, k -> new LinkedList<>()).offer(obj);
    }

    public <T> void setAlways(Class<T> type, T obj) {
        alwaysObjects.put(type, obj);
    }

    public <T> void clear(Class<T> type) {
        alwaysObjects.remove(type);
        queuedObjects.remove(type);
    }

    public void clearAll() {
        alwaysObjects.clear();
        queuedObjects.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> type, Object... args) {
        try {
            if (args.length == 0) {
                return type.getDeclaredConstructor().newInstance();
            }

            // Find constructor that matches the arguments
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == args.length) {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    boolean matches = true;
                    
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null && !isAssignable(paramTypes[i], args[i].getClass())) {
                            matches = false;
                            break;
                        }
                    }
                    
                    if (matches) {
                        constructor.setAccessible(true);
                        return (T) constructor.newInstance(args);
                    }
                }
            }

            throw new RuntimeException("No suitable constructor found for type " + type.getName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + type.getName(), e);
        }
    }

    private boolean isAssignable(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }

        // Handle primitive to wrapper conversions
        if (paramType.isPrimitive()) {
            if (paramType == int.class && argType == Integer.class) return true;
            if (paramType == long.class && argType == Long.class) return true;
            if (paramType == double.class && argType == Double.class) return true;
            if (paramType == float.class && argType == Float.class) return true;
            if (paramType == boolean.class && argType == Boolean.class) return true;
            if (paramType == char.class && argType == Character.class) return true;
            if (paramType == byte.class && argType == Byte.class) return true;
            if (paramType == short.class && argType == Short.class) return true;
        }

        return false;
    }
}

// Global convenience functions
class GlobalObjectFactory {
    public static <T> T create(Class<T> type, Object... args) {
        return ObjectFactory.getInstance().create(type, args);
    }

    public static <I, T extends I> I create(Class<I> interfaceType, Class<T> implementationType, Object... args) {
        return ObjectFactory.getInstance().create(interfaceType, implementationType, args);
    }
}