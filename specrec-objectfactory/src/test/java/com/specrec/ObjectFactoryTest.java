package com.specrec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ObjectFactoryTest {

    private ObjectFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ObjectFactory();
    }

    @Test
    void getInstance_ReturnsSameInstance() {
        ObjectFactory instance1 = ObjectFactory.getInstance();
        ObjectFactory instance2 = ObjectFactory.getInstance();
        
        assertSame(instance1, instance2);
    }

    @Test
    void create_WithoutSetup_CreatesDefaultInstance() {
        TestClass result = factory.create(TestClass.class);
        
        assertNotNull(result);
        assertInstanceOf(TestClass.class, result);
    }

    @Test
    void create_WithConstructorArgs_PassesArgsToConstructor() {
        TestClassWithConstructor result = factory.create(TestClassWithConstructor.class, "test", 42);
        
        assertEquals("test", result.getName());
        assertEquals(42, result.getValue());
    }

    @Test
    void setOne_SetsQueuedObject_ReturnedByCreate() {
        TestClass testObj = new TestClass();
        
        factory.setOne(TestClass.class, testObj);
        TestClass result = factory.create(TestClass.class);
        
        assertSame(testObj, result);
    }

    @Test
    void setOne_MultipleObjects_ReturnedInOrder() {
        TestClass obj1 = new TestClass();
        TestClass obj2 = new TestClass();
        
        factory.setOne(TestClass.class, obj1);
        factory.setOne(TestClass.class, obj2);
        
        TestClass result1 = factory.create(TestClass.class);
        TestClass result2 = factory.create(TestClass.class);
        
        assertSame(obj1, result1);
        assertSame(obj2, result2);
    }

    @Test
    void setOne_AfterQueueEmpty_CreatesDefault() {
        TestClass testObj = new TestClass();
        
        factory.setOne(TestClass.class, testObj);
        factory.create(TestClass.class); // Consume queued object
        
        TestClass result = factory.create(TestClass.class);
        
        assertNotSame(testObj, result);
        assertInstanceOf(TestClass.class, result);
    }

    @Test
    void setAlways_SetsAlwaysObject_AlwaysReturned() {
        TestClass testObj = new TestClass();
        
        factory.setAlways(TestClass.class, testObj);
        
        TestClass result1 = factory.create(TestClass.class);
        TestClass result2 = factory.create(TestClass.class);
        
        assertSame(testObj, result1);
        assertSame(testObj, result2);
    }

    @Test
    void setOne_OverridesSetAlways() {
        TestClass alwaysObj = new TestClass();
        TestClass queuedObj = new TestClass();
        
        factory.setAlways(TestClass.class, alwaysObj);
        factory.setOne(TestClass.class, queuedObj);
        
        TestClass result = factory.create(TestClass.class);
        
        assertSame(queuedObj, result);
    }

    @Test
    void createGeneric_WithInterface_CreatesConcreteType() {
        ITestInterface result = factory.create(ITestInterface.class, TestImplementation.class);
        
        assertInstanceOf(TestImplementation.class, result);
        assertInstanceOf(ITestInterface.class, result);
    }

    @Test
    void createGeneric_WithSetOne_ReturnsQueuedObject() {
        MockTestImplementation mockObj = new MockTestImplementation();
        
        factory.setOne(ITestInterface.class, mockObj);
        
        ITestInterface result = factory.create(ITestInterface.class, TestImplementation.class);
        
        assertSame(mockObj, result);
    }

    @Test
    void createGeneric_WithConstructorCalledWith_CallsMethod() {
        MockTestImplementation mockObj = new MockTestImplementation();
        
        factory.setOne(ITestInterface.class, mockObj);
        
        factory.create(ITestInterface.class, TestImplementation.class, "arg1", 123);
        
        assertNotNull(mockObj.getLastConstructorArgs());
        assertEquals(2, mockObj.getLastConstructorArgs().length);
        assertEquals("arg1", mockObj.getLastConstructorArgs()[0]);
        assertEquals(123, mockObj.getLastConstructorArgs()[1]);
    }

    @Test
    void createGeneric_WithSetAlwaysAndConstructorCalledWith_CallsMethod() {
        MockTestImplementation mockObj = new MockTestImplementation();
        
        factory.setAlways(ITestInterface.class, mockObj);
        
        factory.create(ITestInterface.class, TestImplementation.class, "arg1", 123);
        
        assertNotNull(mockObj.getLastConstructorArgs());
        assertEquals(2, mockObj.getLastConstructorArgs().length);
        assertEquals("arg1", mockObj.getLastConstructorArgs()[0]);
        assertEquals(123, mockObj.getLastConstructorArgs()[1]);
    }

    @Test
    void clear_RemovesAlwaysAndQueuedObjects() {
        TestClass alwaysObj = new TestClass();
        TestClass queuedObj = new TestClass();
        
        factory.setAlways(TestClass.class, alwaysObj);
        factory.setOne(TestClass.class, queuedObj);
        
        factory.clear(TestClass.class);
        
        TestClass result = factory.create(TestClass.class);
        
        assertNotSame(alwaysObj, result);
        assertNotSame(queuedObj, result);
        assertInstanceOf(TestClass.class, result);
    }

    @Test
    void clearAll_RemovesAllObjects() {
        TestClass testObj1 = new TestClass();
        AnotherTestClass testObj2 = new AnotherTestClass();
        
        factory.setAlways(TestClass.class, testObj1);
        factory.setAlways(AnotherTestClass.class, testObj2);
        
        factory.clearAll();
        
        TestClass result1 = factory.create(TestClass.class);
        AnotherTestClass result2 = factory.create(AnotherTestClass.class);
        
        assertNotSame(testObj1, result1);
        assertNotSame(testObj2, result2);
    }

    // Tests for ObjectCreation static convenience class
    @Test
    void objectCreation_Create_UsesObjectFactoryInstance() {
        TestClass testObj = new TestClass();
        ObjectFactory.getInstance().setOne(TestClass.class, testObj);
        
        TestClass result = ObjectCreation.create(TestClass.class);
        
        assertSame(testObj, result);
        
        // Cleanup
        ObjectFactory.getInstance().clearAll();
    }

    @Test
    void objectCreation_CreateGeneric_UsesObjectFactoryInstance() {
        TestServiceMockForObjectCreation mockObj = new TestServiceMockForObjectCreation();
        ObjectFactory.getInstance().setOne(ITestServiceForObjectCreation.class, mockObj);
        
        ITestServiceForObjectCreation result = ObjectCreation.create(ITestServiceForObjectCreation.class, TestServiceImplForObjectCreation.class);
        
        assertSame(mockObj, result);
        
        // Cleanup
        ObjectFactory.getInstance().clearAll();
    }

    @Test
    void objectCreation_Create_PassesConstructorArgs() {
        TestClassWithConstructor result = ObjectCreation.create(TestClassWithConstructor.class, "test", 42);
        
        assertEquals("test", result.getName());
        assertEquals(42, result.getValue());
    }

    // Test helper classes
    public static class TestClass {
    }

    public static class TestClassWithConstructor {
        private final String name;
        private final int value;

        public TestClassWithConstructor(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    public interface ITestInterface {
    }

    public static class TestImplementation implements ITestInterface {
    }

    public static class MockTestImplementation implements ITestInterface, IConstructorCalledWith {
        private Object[] lastConstructorArgs;

        @Override
        public void constructorCalledWith(Object... args) {
            this.lastConstructorArgs = args;
        }

        public Object[] getLastConstructorArgs() {
            return lastConstructorArgs;
        }
    }

    public static class AnotherTestClass {
    }

    // Additional test helper classes for ObjectCreation tests
    public interface ITestServiceForObjectCreation {
    }

    public static class TestServiceImplForObjectCreation implements ITestServiceForObjectCreation {
    }

    public static class TestServiceMockForObjectCreation implements ITestServiceForObjectCreation {
    }
}