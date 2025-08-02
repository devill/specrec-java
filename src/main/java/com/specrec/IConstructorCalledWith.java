package com.specrec;

/**
 * Interface for test doubles that need to log constructor arguments to the storybook.
 * When a fake implements this interface, ObjectFactory will call 
 * constructorCalledWith with detailed parameter information including names, types, and values.
 */
public interface IConstructorCalledWith {
    /**
     * Called by ObjectFactory with detailed constructor parameter information before object creation.
     * This provides parameter names, types, and values for each constructor parameter.
     * 
     * @param parameters Array of parameter information including names, types, and values
     */
    void constructorCalledWith(ConstructorParameterInfo[] parameters);
}