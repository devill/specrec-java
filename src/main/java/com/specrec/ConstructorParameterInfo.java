package com.specrec;

/**
 * Contains information about a constructor parameter including its name, type, and value.
 */
public class ConstructorParameterInfo {
    private final String name;
    private final Class<?> type;
    private final Object value;

    /**
     * Creates a new instance of ConstructorParameterInfo.
     * 
     * @param name The name of the parameter as defined in the constructor
     * @param type The type of the parameter
     * @param value The actual value passed for this parameter
     */
    public ConstructorParameterInfo(String name, Class<?> type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /**
     * @return The name of the parameter as defined in the constructor
     */
    public String getName() {
        return name;
    }

    /**
     * @return The type of the parameter
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @return The actual value passed for this parameter
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + ": " + type.getSimpleName() + " = " + value;
    }
}