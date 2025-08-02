package com.specrec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
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

    public <T> CreateBuilder<T> create(Class<T> type) {
        return new CreateBuilder<>(this, type, type);
    }

    public <I, T extends I> CreateBuilder<I> create(Class<I> interfaceType, Class<T> implementationType) {
        return new CreateBuilder<>(this, interfaceType, implementationType);
    }

    <I, T extends I> I createInternal(Class<I> interfaceType, Class<T> implementationType, Object... args) {
        I obj = fetchObject(interfaceType, implementationType, args);
        logConstructorCall(obj, implementationType, args);
        return obj;
    }

    public static class CreateBuilder<I> {
        private final ObjectFactory factory;
        private final Class<I> interfaceType;
        private final Class<?> implementationType;

        @SuppressWarnings("unchecked")
        private <T extends I> CreateBuilder(ObjectFactory factory, Class<I> interfaceType, Class<T> implementationType) {
            this.factory = factory;
            this.interfaceType = interfaceType;
            this.implementationType = implementationType;
        }

        public I with(Object... args) {
            return factory.createInternal(interfaceType, (Class<? extends I>) implementationType, args);
        }
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

    private static <T> void logConstructorCall(Object obj, Class<T> implementationType, Object[] args) {
        if (obj instanceof IConstructorCalledWith) {
            ConstructorParameterInfo[] parameterInfos = extractParameterInfo(implementationType, args);
            ((IConstructorCalledWith) obj).constructorCalledWith(parameterInfos);
        }
    }

    private static <T> ConstructorParameterInfo[] extractParameterInfo(Class<T> type, Object[] args) {
        Constructor<T> matchingConstructor = findMatchingConstructor(type, args);
        
        return matchingConstructor == null 
            ? createParameterInfosWithoutConstructor(args) 
            : createParameterInfosFromConstructor(matchingConstructor, args);
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> findMatchingConstructor(Class<T> type, Object[] args) {
        Constructor<?>[] constructors = type.getConstructors();

        for (Constructor<?> constructor : constructors) {
            if (isConstructorMatch(constructor, args)) {
                return (Constructor<T>) constructor;
            }
        }
        
        return null;
    }

    private static boolean isConstructorMatch(Constructor<?> constructor, Object[] args) {
        Class<?>[] ctorParams = constructor.getParameterTypes();
        if (ctorParams.length != args.length) {
            return false;
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null && !ctorParams[i].isAssignableFrom(args[i].getClass())) {
                if (!isPrimitiveCompatible(ctorParams[i], args[i].getClass())) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private static <T> ConstructorParameterInfo[] createParameterInfosFromConstructor(Constructor<T> constructor, Object[] args) {
        Parameter[] parameters = constructor.getParameters();
        ConstructorParameterInfo[] parameterInfos = new ConstructorParameterInfo[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = i < args.length ? args[i] : null;
            parameterInfos[i] = new ConstructorParameterInfo(parameter.getName(), parameter.getType(), value);
        }
        
        return parameterInfos;
    }

    private static ConstructorParameterInfo[] createParameterInfosWithoutConstructor(Object[] args) {
        ConstructorParameterInfo[] parameterInfos = new ConstructorParameterInfo[args.length];
        
        for (int i = 0; i < args.length; i++) {
            Class<?> argType = args[i] != null ? args[i].getClass() : Object.class;
            parameterInfos[i] = new ConstructorParameterInfo("arg" + i, argType, args[i]);
        }
        
        return parameterInfos;
    }

    private static boolean isPrimitiveCompatible(Class<?> parameterType, Class<?> argumentType) {
        if (parameterType.isAssignableFrom(argumentType)) return true;
        
        // Handle primitive to wrapper conversions
        if (parameterType == int.class && argumentType == Integer.class) return true;
        if (parameterType == String.class && argumentType == String.class) return true;
        if (parameterType == boolean.class && argumentType == Boolean.class) return true;
        if (parameterType == double.class && argumentType == Double.class) return true;
        if (parameterType == float.class && argumentType == Float.class) return true;
        if (parameterType == long.class && argumentType == Long.class) return true;
        if (parameterType == char.class && argumentType == Character.class) return true;
        if (parameterType == byte.class && argumentType == Byte.class) return true;
        if (parameterType == short.class && argumentType == Short.class) return true;
        
        return false;
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
                        if (args[i] != null && !isPrimitiveCompatible(paramTypes[i], args[i].getClass())) {
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

}

// Global convenience functions
class GlobalObjectFactory {
    public static <T> ObjectFactory.CreateBuilder<T> create(Class<T> type) {
        return ObjectFactory.getInstance().create(type);
    }

    public static <I, T extends I> ObjectFactory.CreateBuilder<I> create(Class<I> interfaceType, Class<T> implementationType) {
        return ObjectFactory.getInstance().create(interfaceType, implementationType);
    }
}