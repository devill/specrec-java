package com.specrec;

/**
 * Interface for test doubles that need to log constructor arguments to the storybook.
 * When a fake implements this interface, ObjectFactory will call 
 * constructorCalledWith with the exact arguments passed to create.
 */
public interface IConstructorCalledWith {
    /**
     * Called by ObjectFactory with the constructor arguments before object creation.
     * Implement this method to log constructor arguments to your storybook for test verification.
     * 
     * @param args The constructor arguments that will be passed to the real implementation
     */
    void constructorCalledWith(Object... args);
}