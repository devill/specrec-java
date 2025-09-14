package link.specrec;

/**
 * Unified factory that automatically chooses the appropriate proxy strategy
 * based on whether the target is an interface or concrete class.
 */
public class UnifiedProxyFactory {

    /**
     * Creates a logging proxy that wraps the target and logs method calls.
     * Automatically chooses between interface proxy (JDK Proxy) and concrete class proxy (CGLib).
     */
    @SuppressWarnings("unchecked")
    public static <T> T createLoggingProxy(Class<T> type, T target, CallLogger logger, String emoji) {
        if (type.isInterface()) {
            // Use existing interface proxy logic
            return CallLoggerProxy.create(type, target, logger, emoji);
        } else if (target != null && hasInterfaces(target.getClass())) {
            // If target implements interfaces, prefer interface proxy for backward compatibility
            return CallLoggerProxy.create(type, target, logger, emoji);
        } else {
            // Use CGLib for concrete classes without interfaces
            return ConcreteClassProxyFactory.createLoggingProxy(type, target, logger, emoji);
        }
    }

    /**
     * Checks if a class implements any interfaces (excluding marker interfaces).
     */
    private static boolean hasInterfaces(Class<?> clazz) {
        Class<?>[] interfaces = clazz.getInterfaces();
        return interfaces.length > 0;
    }

    /**
     * Creates a parrot proxy that replays method calls from verified files.
     * Automatically chooses between interface proxy (JDK Proxy) and concrete class proxy (CGLib).
     */
    @SuppressWarnings("unchecked")
    public static <T> T createParrotProxy(Class<T> type, CallLogger logger, String emoji) {
        if (type.isInterface()) {
            // For interfaces, create a proxy without target (parrot mode)
            return CallLoggerProxy.create(type, null, logger, emoji);
        } else {
            // For concrete classes, parrot mode is not fully implemented yet
            throw new UnsupportedOperationException(
                "Parrot mode for concrete classes is not yet implemented. " +
                "Consider creating interface-based abstractions or manual stubs for concrete class " + type.getName());
        }
    }

    /**
     * Checks if a type can be proxied (either as interface or concrete class).
     */
    public static boolean canCreateProxy(Class<?> type) {
        if (type.isInterface()) {
            // Interfaces can always be proxied
            return true;
        } else {
            // Check if concrete class can be proxied with CGLib
            return ConcreteClassProxyFactory.canCreateProxy(type);
        }
    }

    /**
     * Gets a human-readable explanation of why a type cannot be proxied.
     */
    public static String getProxyLimitation(Class<?> type) {
        if (type.isInterface()) {
            return "Interfaces can be proxied.";
        }

        if (type.isPrimitive()) {
            return "Primitive types cannot be proxied.";
        }

        if (type.isArray()) {
            return "Array types cannot be proxied.";
        }

        if (java.lang.reflect.Modifier.isFinal(type.getModifiers())) {
            return "Final classes cannot be proxied. Consider wrapping in an interface.";
        }

        if (type.getConstructors().length == 0) {
            return "Classes without public constructors cannot be proxied.";
        }

        // Check for interceptable methods
        java.lang.reflect.Method[] methods = type.getMethods();
        boolean hasInterceptableMethod = false;
        for (java.lang.reflect.Method method : methods) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers()) &&
                !java.lang.reflect.Modifier.isFinal(method.getModifiers()) &&
                !java.lang.reflect.Modifier.isStatic(method.getModifiers()) &&
                method.getDeclaringClass() != Object.class) {
                hasInterceptableMethod = true;
                break;
            }
        }

        if (!hasInterceptableMethod) {
            return "Class has no interceptable methods (all methods are final, static, or inherited from Object).";
        }

        return "Class can be proxied.";
    }

    /**
     * Determines the proxy strategy that would be used for a given type.
     */
    public static ProxyStrategy getProxyStrategy(Class<?> type) {
        if (type.isInterface()) {
            return ProxyStrategy.INTERFACE_PROXY;
        } else if (ConcreteClassProxyFactory.canCreateProxy(type)) {
            return ProxyStrategy.CGLIB_PROXY;
        } else {
            return ProxyStrategy.NOT_SUPPORTED;
        }
    }

    /**
     * Enum representing different proxy strategies.
     */
    public enum ProxyStrategy {
        INTERFACE_PROXY("JDK Dynamic Proxy"),
        CGLIB_PROXY("CGLib Proxy"),
        NOT_SUPPORTED("Cannot be proxied");

        private final String description;

        ProxyStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}