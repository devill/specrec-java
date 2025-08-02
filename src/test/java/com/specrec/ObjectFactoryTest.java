package com.specrec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.specrec.GlobalObjectFactory.*;

public class ObjectFactoryTest {

    @Nested
    public static class InstanceManagement {
        @Test
        void getInstance_ReturnsSameInstance() {
            ObjectFactory instance1 = ObjectFactory.getInstance();
            ObjectFactory instance2 = ObjectFactory.getInstance();
            
            assertSame(instance1, instance2);
        }
    }

    @Nested
    public static class BasicCreateOperations {
        private ObjectFactory factory;

        @BeforeEach
        void setUp() {
            factory = new ObjectFactory();
        }

        @Test
        void create_WithoutSetup_CreatesDefaultInstance() {
            TestClass result = factory.create(TestClass.class).with();
            
            assertNotNull(result);
            assertInstanceOf(TestClass.class, result);
        }

        @Test
        void create_WithConstructorArgs_PassesArgsToConstructor() {
            TestClassWithConstructor result = factory.create(TestClassWithConstructor.class).with("test", 42);
            
            assertEquals("test", result.getName());
            assertEquals(42, result.getValue());
        }

        @Test
        void createGeneric_WithInterface_CreatesConcreteType() {
            ITestInterface result = factory.create(ITestInterface.class, TestImplementation.class).with();
            
            assertInstanceOf(TestImplementation.class, result);
            assertInstanceOf(ITestInterface.class, result);
        }
    }

    @Nested
    public static class SetOneBehavior {
        private ObjectFactory factory;

        @BeforeEach
        void setUp() {
            factory = new ObjectFactory();
        }

        @Test
        void setOne_SetsQueuedObject_ReturnedByCreate() {
            TestClass testObj = new TestClass();
            
            factory.setOne(TestClass.class, testObj);
            TestClass result = factory.create(TestClass.class).with();
            
            assertSame(testObj, result);
        }

        @Test
        void setOne_MultipleObjects_ReturnedInOrder() {
            TestClass obj1 = new TestClass();
            TestClass obj2 = new TestClass();
            
            factory.setOne(TestClass.class, obj1);
            factory.setOne(TestClass.class, obj2);
            
            TestClass result1 = factory.create(TestClass.class).with();
            TestClass result2 = factory.create(TestClass.class).with();
            
            assertSame(obj1, result1);
            assertSame(obj2, result2);
        }

        @Test
        void setOne_AfterQueueEmpty_CreatesDefault() {
            TestClass testObj = new TestClass();
            
            factory.setOne(TestClass.class, testObj);
            factory.create(TestClass.class).with(); // Consume queued object
            
            TestClass result = factory.create(TestClass.class).with();
            
            assertNotSame(testObj, result);
            assertInstanceOf(TestClass.class, result);
        }

        @Test
        void createGeneric_WithSetOne_ReturnsQueuedObject() {
            MockTestImplementation mockObj = new MockTestImplementation();
            
            factory.setOne(ITestInterface.class, mockObj);
            
            ITestInterface result = factory.create(ITestInterface.class, TestImplementation.class).with();
            
            assertSame(mockObj, result);
        }
    }

    @Nested
    public static class SetAlwaysBehavior {
        private ObjectFactory factory;

        @BeforeEach
        void setUp() {
            factory = new ObjectFactory();
        }

        @Test
        void setAlways_SetsAlwaysObject_AlwaysReturned() {
            TestClass testObj = new TestClass();
            
            factory.setAlways(TestClass.class, testObj);
            
            TestClass result1 = factory.create(TestClass.class).with();
            TestClass result2 = factory.create(TestClass.class).with();
            
            assertSame(testObj, result1);
            assertSame(testObj, result2);
        }

        @Test
        void setOne_OverridesSetAlways() {
            TestClass alwaysObj = new TestClass();
            TestClass queuedObj = new TestClass();
            
            factory.setAlways(TestClass.class, alwaysObj);
            factory.setOne(TestClass.class, queuedObj);
            
            TestClass result = factory.create(TestClass.class).with();
            
            assertSame(queuedObj, result);
        }
    }

    @Nested
    public static class ClearOperations {
        private ObjectFactory factory;

        @BeforeEach
        void setUp() {
            factory = new ObjectFactory();
        }

        @Test
        void clear_RemovesAlwaysAndQueuedObjects() {
            TestClass alwaysObj = new TestClass();
            TestClass queuedObj = new TestClass();
            
            factory.setAlways(TestClass.class, alwaysObj);
            factory.setOne(TestClass.class, queuedObj);
            
            factory.clear(TestClass.class);
            
            TestClass result = factory.create(TestClass.class).with();
            
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
            
            TestClass result1 = factory.create(TestClass.class).with();
            AnotherTestClass result2 = factory.create(AnotherTestClass.class).with();
            
            assertNotSame(testObj1, result1);
            assertNotSame(testObj2, result2);
        }
    }

    @Nested
    public static class ConstructorCalledWithTests {
        private ObjectFactory factory;

        @BeforeEach
        void setUp() {
            factory = new ObjectFactory();
        }

        @Test
        void createGeneric_WithConstructorCalledWith_CallsMethod() {
            MockTestImplementation mockObj = new MockTestImplementation();
            
            factory.setOne(ITestInterface.class, mockObj);
            
            factory.create(ITestInterface.class, TestImplementation.class).with("arg1", 123);
            
            assertNotNull(mockObj.getLastConstructorArgs());
            assertEquals(2, mockObj.getLastConstructorArgs().length);
            assertEquals("arg1", mockObj.getLastConstructorArgs()[0]);
            assertEquals(123, mockObj.getLastConstructorArgs()[1]);
        }

        @Test
        void createGeneric_WithSetAlwaysAndConstructorCalledWith_CallsMethod() {
            MockTestImplementation mockObj = new MockTestImplementation();
            
            factory.setAlways(ITestInterface.class, mockObj);
            
            factory.create(ITestInterface.class, TestImplementation.class).with("arg1", 123);
            
            assertNotNull(mockObj.getLastConstructorArgs());
            assertEquals(2, mockObj.getLastConstructorArgs().length);
            assertEquals("arg1", mockObj.getLastConstructorArgs()[0]);
            assertEquals(123, mockObj.getLastConstructorArgs()[1]);
        }

        @Test
        void create_WithConstructorCalledWith_CallsMethodViaDelegation() {
            MockTestImplementation mockObj = new MockTestImplementation();
            
            factory.setOne(MockTestImplementation.class, mockObj);
            
            factory.create(MockTestImplementation.class).with("arg1", 123);
            
            assertNotNull(mockObj.getLastConstructorArgs());
            assertEquals(2, mockObj.getLastConstructorArgs().length);
            assertEquals("arg1", mockObj.getLastConstructorArgs()[0]);
            assertEquals(123, mockObj.getLastConstructorArgs()[1]);
        }
        
        @Test
        void create_WithTestClassWithConstructor_ExtractsParameterNames() {
            MockTestClassWithConstructor mockObj = new MockTestClassWithConstructor();
            
            factory.setOne(TestClassWithConstructor.class, mockObj);
            
            factory.create(TestClassWithConstructor.class).with("paramName", 99);
            
            assertNotNull(mockObj.getLastParameterDetails());
            assertEquals(2, mockObj.getLastParameterDetails().length);
            
            assertEquals("name: String = paramName", mockObj.getLastParameterDetails()[0].toString());
            assertEquals("value: int = 99", mockObj.getLastParameterDetails()[1].toString());
        }
        
        @Test
        void create_WithNoMatchingConstructor_UsesGenericParameterNames() {
            MockTestImplementation mockObj = new MockTestImplementation();
            
            factory.setOne(ITestInterface.class, mockObj);
            
            factory.create(ITestInterface.class, TestImplementation.class).with("unexpected", 42);
            
            assertNotNull(mockObj.getLastParameterDetails());
            assertEquals(2, mockObj.getLastParameterDetails().length);
            
            assertEquals("arg0: String = unexpected", mockObj.getLastParameterDetails()[0].toString());
            assertEquals("arg1: Integer = 42", mockObj.getLastParameterDetails()[1].toString());
        }
    }

    @Nested
    public static class InheritanceScenarios {
        private ObjectFactory factory;

        @BeforeEach
        void setUp() {
            factory = new ObjectFactory();
        }

        @Test
        void create_WithParentType_SetChildInstance_ReturnsChildAsParent() {
            ChildClass childInstance = new ChildClass("child value");
            
            factory.setOne(ParentClass.class, childInstance);
            
            ParentClass result = factory.create(ParentClass.class).with();
            
            assertSame(childInstance, result);
            assertInstanceOf(ChildClass.class, result);
            assertEquals("child value", ((ChildClass) result).getChildProperty());
        }

        @Test
        void create_WithParentType_SetAlwaysChild_AlwaysReturnsChild() {
            ChildClass childInstance = new ChildClass("always child");
            
            factory.setAlways(ParentClass.class, childInstance);
            
            ParentClass result1 = factory.create(ParentClass.class).with();
            ParentClass result2 = factory.create(ParentClass.class).with();
            
            assertSame(childInstance, result1);
            assertSame(childInstance, result2);
            assertInstanceOf(ChildClass.class, result1);
            assertEquals("always child", ((ChildClass) result1).getChildProperty());
        }

        @Test
        void create_WithParentType_NoSetup_CreatesParentDirectly() {
            ParentClass result = factory.create(ParentClass.class).with();
            
            assertNotNull(result);
            assertInstanceOf(ParentClass.class, result);
            // Should not be ChildClass since no constructor arguments provided
            assertEquals(ParentClass.class, result.getClass());
        }

        @Test
        void create_WithParentType_ChildWithConstructorArgs_LogsCorrectly() {
            MockChildClass mockChild = new MockChildClass();
            
            factory.setOne(ParentClass.class, mockChild);
            
            factory.create(ParentClass.class).with("parent arg", 42);
            
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
            
            ParentClass result1 = factory.create(ParentClass.class).with();
            ParentClass result2 = factory.create(ParentClass.class).with();
            
            // First call should return queued child
            assertSame(queuedChild, result1);
            assertEquals("queued", ((ChildClass) result1).getChildProperty());
            
            // Second call should return always child (queue is empty)
            assertSame(alwaysChild, result2);
            assertEquals("always", ((ChildClass) result2).getChildProperty());
        }
    }

    @Nested
    public static class GlobalObjectFactoryTests {
        @Test
        void globalObjectFactory_Create_UsesObjectFactoryInstance() {
            TestClass testObj = new TestClass();
            ObjectFactory.getInstance().setOne(TestClass.class, testObj);
            
            TestClass result = GlobalObjectFactory.create(TestClass.class).with();
            
            assertSame(testObj, result);
            
            // Cleanup
            ObjectFactory.getInstance().clearAll();
        }

        @Test
        void globalObjectFactory_CreateGeneric_UsesObjectFactoryInstance() {
            MockTestImplementation mockObj = new MockTestImplementation();
            ObjectFactory.getInstance().setOne(ITestInterface.class, mockObj);
            
            ITestInterface result = GlobalObjectFactory.create(ITestInterface.class, TestImplementation.class).with();
            
            assertSame(mockObj, result);
            
            // Cleanup
            ObjectFactory.getInstance().clearAll();
        }

        @Test
        void globalObjectFactory_Create_PassesConstructorArgs() {
            TestClassWithConstructor result = GlobalObjectFactory.create(TestClassWithConstructor.class).with("test", 42);
            
            assertEquals("test", result.getName());
            assertEquals(42, result.getValue());
        }

        @Test
        void directCreate_WithStaticImport_WorksWithoutClassPrefix() {
            TestClassWithConstructor result = create(TestClassWithConstructor.class).with("direct", 99);
            
            assertEquals("direct", result.getName());
            assertEquals(99, result.getValue());
        }
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

    public static class TestImplementationWithConstructor implements ITestInterface {
        private final String name;
        private final int value;

        public TestImplementationWithConstructor(String name, int value) {
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

    public static class MockTestImplementation implements ITestInterface, IConstructorCalledWith {
        private Object[] lastConstructorArgs;
        private ConstructorParameterInfo[] lastParameterDetails;

        @Override
        public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
            this.lastParameterDetails = parameters;
            this.lastConstructorArgs = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                this.lastConstructorArgs[i] = parameters[i].getValue();
            }
        }

        public Object[] getLastConstructorArgs() {
            return lastConstructorArgs;
        }

        public ConstructorParameterInfo[] getLastParameterDetails() {
            return lastParameterDetails;
        }
    }

    public static class AnotherTestClass {
    }

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
        private ConstructorParameterInfo[] lastParameterDetails;

        @Override
        public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
            this.lastParameterDetails = parameters;
            this.lastConstructorArgs = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                this.lastConstructorArgs[i] = parameters[i].getValue();
            }
        }

        public Object[] getLastConstructorArgs() {
            return lastConstructorArgs;
        }

        public ConstructorParameterInfo[] getLastParameterDetails() {
            return lastParameterDetails;
        }
    }

    public static class MockTestClassWithConstructor extends TestClassWithConstructor implements IConstructorCalledWith {
        private Object[] lastConstructorArgs;
        private ConstructorParameterInfo[] lastParameterDetails;

        public MockTestClassWithConstructor() {
            super("default", 0);
        }

        @Override
        public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
            this.lastParameterDetails = parameters;
            this.lastConstructorArgs = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                this.lastConstructorArgs[i] = parameters[i].getValue();
            }
        }

        public Object[] getLastConstructorArgs() {
            return lastConstructorArgs;
        }

        public ConstructorParameterInfo[] getLastParameterDetails() {
            return lastParameterDetails;
        }
    }
}