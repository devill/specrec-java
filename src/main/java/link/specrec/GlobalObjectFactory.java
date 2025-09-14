package link.specrec;

/**
 * Global convenience functions for creating objects via ObjectFactory.
 * This class provides static methods that can be statically imported for cleaner syntax.
 *
 * Example usage:
 * <pre>
 * import static link.specrec.GlobalObjectFactory.*;
 *
 * // Create objects without explicitly referencing ObjectFactory
 * MyClass obj = create(MyClass.class).with("arg1", 42);
 * IMyInterface obj = create(IMyInterface.class, MyImplementation.class).with();
 * </pre>
 */
public class GlobalObjectFactory {
    /**
     * Creates a builder for the specified type.
     * @param type The class to create an instance of
     * @return A CreateBuilder for the specified type
     */
    public static <T> ObjectFactory.CreateBuilder<T> create(Class<T> type) {
        return ObjectFactory.getInstance().create(type);
    }

    /**
     * Creates a builder for the specified interface type with a concrete implementation.
     * @param interfaceType The interface type to return
     * @param implementationType The concrete implementation class
     * @return A CreateBuilder for the specified interface type
     */
    public static <I, T extends I> ObjectFactory.CreateBuilder<I> create(Class<I> interfaceType, Class<T> implementationType) {
        return ObjectFactory.getInstance().create(interfaceType, implementationType);
    }
}