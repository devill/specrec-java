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

    // Test helper classes for inheritance scenarios
    public static class ParentClass {
        private String parentProperty = "parent";

        public String getParentProperty() {
            return parentProperty;
        }

        public void setParentProperty(String parentProperty) {
            this.parentProperty = parentProperty;
        }
    }

    public static class ChildClass extends ParentClass {
        private final String childProperty;

        public ChildClass(String childValue) {
            this.childProperty = childValue;
        }

        public String getChildProperty() {
            return childProperty;
        }
    }

    public static class MockChildClass extends ParentClass implements IConstructorCalledWith {
        private Object[] lastConstructorArgs;

        @Override
        public void constructorCalledWith(Object... args) {
            this.lastConstructorArgs = args;
        }

        public Object[] getLastConstructorArgs() {
            return lastConstructorArgs;
        }
    }

    // Test for constructor logging via delegation
    @Test
    void create_WithConstructorCalledWith_CallsMethodViaDelegation() {
        MockTestImplementation mockObj = new MockTestImplementation();
        
        factory.setOne(MockTestImplementation.class, mockObj);
        
        factory.create(MockTestImplementation.class, "arg1", 123);
        
        assertNotNull(mockObj.getLastConstructorArgs());
        assertEquals(2, mockObj.getLastConstructorArgs().length);
        assertEquals("arg1", mockObj.getLastConstructorArgs()[0]);
        assertEquals(123, mockObj.getLastConstructorArgs()[1]);
    }

    // Inheritance tests for Create<T> scenarios
    @Test
    void create_WithParentType_SetChildInstance_ReturnsChildAsParent() {
        ChildClass childInstance = new ChildClass("child value");
        
        factory.setOne(ParentClass.class, childInstance);
        
        ParentClass result = factory.create(ParentClass.class);
        
        assertSame(childInstance, result);
        assertInstanceOf(ChildClass.class, result);
        assertEquals("child value", ((ChildClass) result).getChildProperty());
    }

    @Test
    void create_WithParentType_SetAlwaysChild_AlwaysReturnsChild() {
        ChildClass childInstance = new ChildClass("always child");
        
        factory.setAlways(ParentClass.class, childInstance);
        
        ParentClass result1 = factory.create(ParentClass.class);
        ParentClass result2 = factory.create(ParentClass.class);
        
        assertSame(childInstance, result1);
        assertSame(childInstance, result2);
        assertInstanceOf(ChildClass.class, result1);
        assertEquals("always child", ((ChildClass) result1).getChildProperty());
    }

    @Test
    void create_WithParentType_NoSetup_CreatesParentDirectly() {
        ParentClass result = factory.create(ParentClass.class);
        
        assertNotNull(result);
        assertInstanceOf(ParentClass.class, result);
        // Should not be ChildClass since no constructor arguments provided
        assertEquals(ParentClass.class, result.getClass());
    }

    @Test
    void create_WithParentType_ChildWithConstructorArgs_LogsCorrectly() {
        MockChildClass mockChild = new MockChildClass();
        
        factory.setOne(ParentClass.class, mockChild);
        
        factory.create(ParentClass.class, "parent arg", 42);
        
        assertNotNull(mockChild.getLastConstructorArgs());
        assertEquals(2, mockChild.getLastConstructorArgs().length);
        assertEquals("parent arg", mockChild.getLastConstructorArgs()[0]);
        assertEquals(42, mockChild.getLastConstructorArgs()[1]);
    }

    @Test
    void create_WithParentType_SetOnePriorityOverSetAlways() {
        ChildClass alwaysChild = new ChildClass("always");
        ChildClass queuedChild = new ChildClass("queued");
        
        factory.setAlways(ParentClass.class, alwaysChild);
        factory.setOne(ParentClass.class, queuedChild);
        
        ParentClass result1 = factory.create(ParentClass.class);
        ParentClass result2 = factory.create(ParentClass.class);
        
        // First call should return queued child
        assertSame(queuedChild, result1);
        assertEquals("queued", ((ChildClass) result1).getChildProperty());
        
        // Second call should return always child (queue is empty)
        assertSame(alwaysChild, result2);
        assertEquals("always", ((ChildClass) result2).getChildProperty());
    }
}