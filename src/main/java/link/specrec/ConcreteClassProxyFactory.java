package link.specrec;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.Factory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

/**
 * Factory for creating proxies of concrete classes using CGLib.
 * This enables wrapping concrete classes that don't implement interfaces.
 */
public class ConcreteClassProxyFactory {

    /**
     * Creates a proxy for a concrete class that wraps the target instance and logs method calls.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createLoggingProxy(Class<T> concreteClass, T target, CallLogger logger, String emoji) {
        if (!canCreateProxy(concreteClass)) {
            throw new IllegalArgumentException(
                String.format("Cannot create proxy for class %s. Class must be non-final with at least one non-final public method.",
                    concreteClass.getName())
            );
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(concreteClass);
        enhancer.setUseFactory(true);
        enhancer.setCallbackTypes(new Class[]{MethodInterceptor.class});

        try {
            // Create the proxy class without calling constructor
            Class<?> proxyClass = enhancer.createClass();

            // Use the Factory interface to create instance without calling constructor
            Factory factory = (Factory) createInstanceWithoutConstructor(proxyClass);
            factory.setCallback(0, new ConcreteClassMethodInterceptor(target, logger, emoji, concreteClass));

            return (T) factory;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create CGLib proxy for " + concreteClass.getName(), e);
        }
    }

    /**
     * Creates a "parrot" proxy for a concrete class that replays method calls from verified files.
     */
    @SuppressWarnings("unchecked")
    public static <T> T createParrotProxy(Class<T> concreteClass, CallLogger logger, String emoji) {
        if (!canCreateProxy(concreteClass)) {
            throw new IllegalArgumentException(
                String.format("Cannot create parrot proxy for class %s. Class must be non-final with at least one non-final public method.",
                    concreteClass.getName())
            );
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(concreteClass);
        enhancer.setUseFactory(true);
        enhancer.setCallbackTypes(new Class[]{MethodInterceptor.class});

        try {
            // Create the proxy class without calling constructor
            Class<?> proxyClass = enhancer.createClass();

            // Use the Factory interface to create instance without calling constructor
            Factory factory = (Factory) createInstanceWithoutConstructor(proxyClass);
            factory.setCallback(0, new ConcreteClassMethodInterceptor(null, logger, emoji, concreteClass));

            return (T) factory;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create CGLib parrot proxy for " + concreteClass.getName(), e);
        }
    }

    /**
     * Creates an instance without calling any constructor using reflection.
     * This is similar to how C# creates uninitialized instances.
     */
    private static Object createInstanceWithoutConstructor(Class<?> clazz) throws Exception {
        try {
            // Try to use sun.misc.Unsafe first (most reliable)
            return createWithUnsafe(clazz);
        } catch (Exception e) {
            try {
                // Fallback: Try to use ReflectionFactory (if available)
                return createWithReflectionFactory(clazz);
            } catch (Exception e2) {
                // Last resort: Use parameterless constructor if available
                return clazz.getDeclaredConstructor().newInstance();
            }
        }
    }

    private static Object createWithUnsafe(Class<?> clazz) throws Exception {
        // Access sun.misc.Unsafe to allocate instance without constructor
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);

        java.lang.reflect.Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return allocateInstance.invoke(unsafe, clazz);
    }

    private static Object createWithReflectionFactory(Class<?> clazz) throws Exception {
        // Use ReflectionFactory if available (internal JDK API)
        Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
        Object reflectionFactory = reflectionFactoryClass.getMethod("getReflectionFactory").invoke(null);

        java.lang.reflect.Constructor<?> objectConstructor = Object.class.getDeclaredConstructor();
        java.lang.reflect.Method newConstructorForSerialization = reflectionFactoryClass.getMethod(
            "newConstructorForSerialization", Class.class, java.lang.reflect.Constructor.class);

        java.lang.reflect.Constructor<?> constructor = (java.lang.reflect.Constructor<?>)
            newConstructorForSerialization.invoke(reflectionFactory, clazz, objectConstructor);

        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /**
     * Checks if a concrete class can be proxied using CGLib.
     */
    public static boolean canCreateProxy(Class<?> concreteClass) {
        // Must be a class (not interface or primitive)
        if (concreteClass.isInterface() || concreteClass.isPrimitive() || concreteClass.isArray()) {
            return false;
        }

        // Must not be final
        if (Modifier.isFinal(concreteClass.getModifiers())) {
            return false;
        }

        // Must have at least one public constructor
        if (concreteClass.getConstructors().length == 0) {
            return false;
        }

        // Must have at least one non-final public method that we can intercept
        Method[] methods = concreteClass.getMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers()) &&
                !Modifier.isFinal(method.getModifiers()) &&
                !Modifier.isStatic(method.getModifiers()) &&
                method.getDeclaringClass() != Object.class) {
                return true;
            }
        }

        return false;
    }

    /**
     * CGLib method interceptor that handles concrete class method calls.
     */
    private static class ConcreteClassMethodInterceptor implements MethodInterceptor {
        private final Object target;
        private final CallLogger logger;
        private final String emoji;
        private final Class<?> proxyClass;

        public ConcreteClassMethodInterceptor(Object target, CallLogger logger, String emoji, Class<?> proxyClass) {
            this.target = target;
            this.logger = logger;
            this.emoji = emoji;
            this.proxyClass = proxyClass;
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            String methodName = method.getName();

            // Skip Object methods that we don't want to log
            if (shouldSkipMethod(method)) {
                return invokeTarget(method, args, proxy, obj);
            }

            CallLogger callLogger = createCallLogger();
            setupLoggingContext(callLogger, methodName);

            Object result = null;
            Exception caughtException = null;

            try {
                result = invokeTarget(method, args, proxy, obj);
            } catch (Exception ex) {
                caughtException = ex;
                handleMethodException(callLogger, ex, methodName);
                throw ex;
            }

            if (!shouldIgnoreCall(callLogger, methodName)) {
                logMethodCall(callLogger, method, args, result, methodName);
            }

            CallLogFormatterContext.clearCurrentLogger();
            return result;
        }

        private boolean shouldSkipMethod(Method method) {
            // Skip basic Object methods
            if (method.getDeclaringClass() == Object.class) {
                return true;
            }

            // Skip CGLib internal methods
            if (method.getName().contains("CGLIB")) {
                return true;
            }

            return false;
        }

        private Object invokeTarget(Method method, Object[] args, MethodProxy proxy, Object obj) throws Throwable {
            if (target != null) {
                // Logging mode: call the real target
                return method.invoke(target, args);
            } else {
                // Parrot mode: this is more complex for concrete classes
                // For now, we'll throw an exception indicating parrot mode isn't fully implemented
                throw new UnsupportedOperationException(
                    "Parrot mode for concrete classes is not yet implemented. " +
                    "Consider using interface-based stubs or manual implementations.");
            }
        }

        private CallLogger createCallLogger() {
            return new CallLogger(new StringBuilder(), emoji);
        }

        private void setupLoggingContext(CallLogger callLogger, String methodName) {
            CallLogFormatterContext.setCurrentLogger(callLogger);
            CallLogFormatterContext.setCurrentMethodName(methodName);
        }

        private void handleMethodException(CallLogger callLogger, Exception ex, String methodName) {
            callLogger.withNote("Exception: " + ex.getMessage());
            callLogger.log(methodName);
            logger.getSpecBook().append(callLogger.getSpecBook().toString());
            CallLogFormatterContext.clearCurrentLogger();
        }

        private boolean shouldIgnoreCall(CallLogger callLogger, String methodName) {
            return callLogger.ignoredCalls.contains(methodName);
        }

        private void logMethodCall(CallLogger callLogger, Method method, Object[] args, Object result, String methodName) {
            logInputArguments(callLogger, method, args, methodName);
            logReturnValue(callLogger, result, methodName);

            callLogger.log(methodName);
            logger.getSpecBook().append(callLogger.getSpecBook().toString());
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

                callLogger.withArgument(args[i], parameters[i].getName());
            }
        }

        private boolean shouldIgnoreArgument(CallLogger callLogger, String methodName, int argumentIndex) {
            return callLogger.ignoredArguments.containsKey(methodName) &&
                   callLogger.ignoredArguments.get(methodName).contains(argumentIndex);
        }

        private void logReturnValue(CallLogger callLogger, Object result, String methodName) {
            if (result != null && !callLogger.ignoredReturnValues.contains(methodName)) {
                callLogger.withReturn(result);
            }
        }
    }
}