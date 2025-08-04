package com.specrec;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import java.util.*;

public class CallLoggerTest {

    @Test
    public void callLogger_ManualLogging_ShouldFormatCorrectly() {
        CallLogger logger = new CallLogger();

        logger.withArgument("value1", "firstParam")
              .withArgument("value2", "secondParam")
              .withNote("Manual logging test")
              .withReturn("success")
              .log("ManualMethod");

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLogger_ForInterface_ShouldUseCustomInterfaceName() {
        CallLogger logger = new CallLogger();

        logger.forInterface("ICustomService")
              .withArgument("test", "param1")
              .withReturn("result")
              .log("TestMethod");

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLogger_WithSharedSpecBook_ShouldAllowExternalWrites() {
        StringBuilder sharedSpecBook = new StringBuilder();
        CallLogger logger = new CallLogger(sharedSpecBook);
        TestService mockService = new TestService();
        
        sharedSpecBook.append("🧪 Test started\n");
        
        ITestService wrappedService = logger.wrap(mockService, "🔧");
        wrappedService.calculate(10, 20);
        
        sharedSpecBook.append("🧪 Test ended\n");
        
        Approvals.verify(sharedSpecBook.toString());
    }

    @Test
    public void wrap_ShouldLogAllMethodCalls() {
        CallLogger logger = new CallLogger();
        TestService mockService = new TestService();

        ITestService wrappedService = logger.wrap(mockService, "🧪");

        wrappedService.calculate(5, 10);
        wrappedService.processData("test input");

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void wrap_WithCallLogFormatter_ShouldRespectFormattingRules() {
        CallLogger logger = new CallLogger();
        FormattedTestService mockService = new FormattedTestService();

        ITestService wrappedService = logger.wrap(mockService, "📝");

        wrappedService.calculate(5, 10);
        wrappedService.processData("secret");

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void formatValue_ShouldHandleDifferentTypes() {
        CallLogger logger = new CallLogger();
        TypeTestService mockService = new TypeTestService();

        ITypeTestService wrappedService = logger.wrap(mockService, "🎯");

        Date dateTime = new Date(1703499045000L); // 2023-12-25 10:30:45 UTC
        double decimalValue = 123.45;
        double doubleValue = 67.89;
        float floatValue = 12.34f;
        int[] array = {1, 2, 3};

        wrappedService.processTypes(dateTime, decimalValue, doubleValue, floatValue, array, null);

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void formatValue_ShouldHandleCollections() {
        CallLogger logger = new CallLogger();
        CollectionTestService mockService = new CollectionTestService();

        ICollectionTestService wrappedService = logger.wrap(mockService, "📚");

        List<String> list = Arrays.asList("item1", "item2", "item3");
        List<Integer> emptyList = new ArrayList<>();
        wrappedService.processCollections(list, emptyList);

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLoggerProxy_WithMethodThatReturnsNull_ShouldLogCorrectly() {
        CallLogger logger = new CallLogger();
        TestService mockService = new TestService();

        ITestService wrappedService = logger.wrap(mockService, "⚡");

        wrappedService.processData("test");

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLoggerProxy_WithNullArguments_ShouldHandleGracefully() {
        CallLogger logger = new CallLogger();
        TestService mockService = new TestService();

        ITestService wrappedService = logger.wrap(mockService, "🌟");

        wrappedService.processData(null);

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLoggerProxy_WithExceptionInMethod_ShouldLogExceptionAndRethrow() {
        CallLogger logger = new CallLogger();
        ExceptionTestService mockService = new ExceptionTestService();

        IExceptionTestService wrappedService = logger.wrap(mockService, "💥");

        try {
            wrappedService.throwException("error");
        } catch (RuntimeException e) {
            // Expected exception
        }

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLogger_WithException_ShouldLogException() {
        CallLogger logger = new CallLogger();

        logger.withArgument("input", "param")
              .withNote("Exception: Something went wrong")
              .log("FailedMethod");

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLogFormatterContext_IgnoreArgument_ShouldHideSpecificArgument() {
        CallLogger logger = new CallLogger();
        FormatterContextTestService mockService = new FormatterContextTestService();

        IFormatterContextTestService wrappedService = logger.wrap(mockService, "🔒");

        wrappedService.methodWithIgnoredArgument("visible", "hidden");

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLogFormatterContext_IgnoreReturnValue_ShouldHideReturnValue() {
        CallLogger logger = new CallLogger();
        FormatterContextTestService mockService = new FormatterContextTestService();

        IFormatterContextTestService wrappedService = logger.wrap(mockService, "🙈");

        wrappedService.methodWithIgnoredReturn();

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLogFormatterContext_SetConstructorArgumentNames_ShouldUseCustomNames() {
        CallLogger logger = new CallLogger();
        
        CallLogFormatterContext.setConstructorArgumentNames("customName1", "customName2");
        
        logger.forInterface("ITestService")
              .withArgument("value1", "customName1")
              .withArgument("value2", "customName2")
              .log("constructorCalledWith");

        CallLogFormatterContext.clearCurrentLogger();

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void constructorCalledWith_ShouldLogConstructorCall() {
        StringBuilder sharedSpecBook = new StringBuilder();
        CallLogger logger = new CallLogger(sharedSpecBook);
        
        ConstructorParameterInfo[] parameters = {
            new ConstructorParameterInfo("param1", String.class, "value1"),
            new ConstructorParameterInfo("param2", Integer.class, 42)
        };
        
        TestService testService = new TestService();
        ITestService proxy = CallLoggerProxy.create(testService, logger, "🏗️");
        
        // Simulate constructor call
        ConstructorParameterInfo[] params = {
            new ConstructorParameterInfo("param1", String.class, "value1"),
            new ConstructorParameterInfo("param2", Integer.class, 42)
        };
        
        if (proxy instanceof IConstructorCalledWith) {
            ((IConstructorCalledWith) proxy).constructorCalledWith(parameters);
        }
        
        Approvals.verify(sharedSpecBook.toString());
    }

    @Test
    public void callLoggerProxy_IntegratedWithObjectFactory_ShouldLogConstructorCalls() {
        StringBuilder sharedSpecBook = new StringBuilder();
        CallLogger logger = new CallLogger(sharedSpecBook);
        ObjectFactory factory = new ObjectFactory();
        
        TestServiceWithConstructorLogging service = factory.create(TestServiceWithConstructorLogging.class)
                                                          .with("param1", 123);
        
        ITestService wrappedService = logger.wrap(service, "🔧");
        wrappedService.calculate(5, 10);
        
        Approvals.verify(sharedSpecBook.toString());
    }

    // Missing ParameterHandling tests

    @Test
    public void callLoggerProxy_WithComplexArgumentTypes_ShouldHandleAllTypes() {
        CallLogger logger = new CallLogger();
        ComplexArgumentService mockService = new ComplexArgumentService();

        IComplexArgumentService wrappedService = logger.wrap(mockService, "🧩");

        Map<String, Object> dict = new HashMap<>();
        dict.put("key", "value");
        Date complexDate = new Date(1720008131000L); // 2024-07-03 12:42:11 UTC
        wrappedService.complexMethod(dict, null, complexDate);

        Approvals.verify(logger.getSpecBook().toString());
    }

    // Missing ConstructorLogging tests
    @Test
    public void callLogger_LogConstructorCall_ShouldFormatCorrectly() {
        CallLogger logger = new CallLogger();

        logger.forInterface("ITestService")
              .withArgument("param1", "arg1")
              .withArgument("param2", "arg2")
              .log("constructorCalledWith");

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void constructorCalledWith_WithCustomArgumentNames_ShouldUseProvidedNames() {
        CallLogger logger = new CallLogger();
        DetailedConstructorService mockService = new DetailedConstructorService("database.db", 5432, true);

        IDetailedConstructorService wrappedService = logger.wrap(mockService, "🔧");

        wrappedService.detailedMethod();

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLoggerProxy_WithAutoParameterNames_ShouldUseActualParameterNames() {
        CallLogger logger = new CallLogger();
        ObjectFactory factory = new ObjectFactory();
        
        ConstructorTestService stubService = new ConstructorTestService("stub", 0);
        factory.setAlways(IConstructorTestService.class, logger.wrap(stubService, "🔧"));

        // Create object through factory to test parameter name extraction
        IConstructorTestService service = factory.create(IConstructorTestService.class).with("config", 42);
        service.testMethod();

        Approvals.verify(logger.getSpecBook().toString());
    }

    // Missing InterfaceDetection tests
    @Test
    public void callLoggerProxy_WithInterfaceImplementation_ShouldDetectInterface() {
        CallLogger logger = new CallLogger();
        InterfaceImplementationService mockService = new InterfaceImplementationService();

        IInterfaceImplementationService wrappedService = logger.wrap(mockService, "🔍");

        wrappedService.detectInterface();

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLoggerProxy_WithComplexInterfaceHierarchy_ShouldDetectCorrectInterface() {
        CallLogger logger = new CallLogger();
        ComplexHierarchyService mockService = new ComplexHierarchyService();

        IComplexHierarchyService wrappedService = logger.wrap(mockService, "🏗️");

        wrappedService.hierarchyMethod();

        Approvals.verify(logger.getSpecBook().toString());
    }

    @Test
    public void callLoggerProxy_WithNonInterfaceName_ShouldFallbackToInterfaceDetection() {
        CallLogger logger = new CallLogger();
        NonInterfaceNameService mockService = new NonInterfaceNameService();

        INonInterfaceNameService wrappedService = logger.wrap(mockService, "🔄");

        wrappedService.fallbackMethod();

        Approvals.verify(logger.getSpecBook().toString());
    }

    // Missing EdgeCases test
    @Test
    public void callLoggerProxy_WithObjectNotImplementingConstructorCalledWith_ShouldHandleGracefully() {
        CallLogger logger = new CallLogger();
        SimpleService mockService = new SimpleService();

        ISimpleService wrappedService = logger.wrap(mockService, "🤷");

        wrappedService.simpleMethod();

        Approvals.verify(logger.getSpecBook().toString());
    }

}

// Test interfaces and implementations
interface ITestService {
    int calculate(int a, int b);
    void processData(String input);
    boolean tryProcess(String input);
}

class TestService implements ITestService {
    @Override
    public int calculate(int a, int b) {
        return a + b;
    }

    @Override
    public void processData(String input) {
        // No-op for testing
    }

    @Override
    public boolean tryProcess(String input) {
        return input != null && !input.isEmpty();
    }
}

class FormattedTestService implements ITestService {
    @Override
    public int calculate(int a, int b) {
        CallLogFormatterContext.ignoreAllArguments();
        CallLogFormatterContext.addNote("This calculation ignores all arguments in logs");
        return a + b;
    }

    @Override
    public void processData(String input) {
        CallLogFormatterContext.ignoreArgument(0);
        CallLogFormatterContext.addNote("Secret data processing");
    }

    @Override
    public boolean tryProcess(String input) {
        return input != null && !input.isEmpty();
    }
}

interface ITypeTestService {
    void processTypes(Date dateTime, double decimal, double doubleVal, float floatVal, int[] array, Object nullValue);
}

class TypeTestService implements ITypeTestService {
    @Override
    public void processTypes(Date dateTime, double decimal, double doubleVal, float floatVal, int[] array, Object nullValue) {
        // This method demonstrates logging of various data types
    }
}

interface ICollectionTestService {
    void processCollections(List<String> list, List<Integer> emptyList);
}

class CollectionTestService implements ICollectionTestService {
    @Override
    public void processCollections(List<String> list, List<Integer> emptyList) {
        // This method demonstrates logging of collections
    }
}

interface IExceptionTestService {
    void throwException(String message);
}

class ExceptionTestService implements IExceptionTestService {
    @Override
    public void throwException(String message) {
        throw new RuntimeException("Test exception");
    }
}

interface IFormatterContextTestService {
    void methodWithIgnoredArgument(String arg0, String arg1);
    String methodWithIgnoredReturn();
}

class FormatterContextTestService implements IFormatterContextTestService {
    @Override
    public void methodWithIgnoredArgument(String arg0, String arg1) {
        CallLogFormatterContext.ignoreArgument(1); // Ignore second argument
    }

    @Override
    public String methodWithIgnoredReturn() {
        CallLogFormatterContext.ignoreReturnValue();
        return "hidden return value";
    }
}

class TestServiceWithConstructorLogging implements ITestService, IConstructorCalledWith {
    private final String param1;
    private final int param2;
    
    public TestServiceWithConstructorLogging(String param1, int param2) {
        this.param1 = param1;
        this.param2 = param2;
    }

    @Override
    public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
        // This will be called by ObjectFactory
    }

    @Override
    public int calculate(int a, int b) {
        return a + b;
    }

    @Override
    public void processData(String input) {
        // No-op
    }

    @Override
    public boolean tryProcess(String input) {
        return true;
    }
}

// Missing test interfaces and implementations

interface IComplexArgumentService {
    void complexMethod(Map<String, Object> dict, Object nullValue, Date complexDate);
}

class ComplexArgumentService implements IComplexArgumentService {
    @Override
    public void complexMethod(Map<String, Object> dict, Object nullValue, Date complexDate) {
        // This method demonstrates logging of complex argument types
    }
}

// ConstructorLogging interfaces
interface IConstructorTestService {
    void testMethod();
}

class ConstructorTestService implements IConstructorTestService, IConstructorCalledWith {
    private final String config;
    private final int port;
    
    public ConstructorTestService(String config, int port) {
        this.config = config;
        this.port = port;
    }

    @Override
    public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
        // Constructor logging will happen here
    }

    @Override
    public void testMethod() {
        // This method is for testing parameter name detection
    }
}

interface IDetailedConstructorService {
    void detailedMethod();
}

class DetailedConstructorService implements IDetailedConstructorService, IConstructorCalledWith {
    private final String database;
    private final int port;
    private final boolean enabled;
    
    public DetailedConstructorService(String database, int port, boolean enabled) {
        this.database = database;
        this.port = port;
        this.enabled = enabled;
    }

    @Override
    public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
        CallLogFormatterContext.setConstructorArgumentNames("databasePath", "portNumber", "isEnabled");
    }

    @Override
    public void detailedMethod() {
        // This method is for testing custom constructor argument names
    }
}

// InterfaceDetection interfaces
interface IInterfaceImplementationService {
    void detectInterface();
}

class InterfaceImplementationService implements IInterfaceImplementationService {
    @Override
    public void detectInterface() {
        // This method is for testing interface detection
    }
}

interface IComplexHierarchyService extends IBaseService {
    void hierarchyMethod();
}

interface IBaseService {
    void baseMethod();
}

class ComplexHierarchyService implements IComplexHierarchyService {
    @Override
    public void hierarchyMethod() {
        // This method is for testing complex interface hierarchy detection
    }

    @Override
    public void baseMethod() {
        // Base interface method
    }
}

interface INonInterfaceNameService {
    void fallbackMethod();
}

class NonInterfaceNameService implements INonInterfaceNameService {
    @Override
    public void fallbackMethod() {
        // This method is for testing interface name fallback detection
    }
}

// EdgeCases interfaces
interface ISimpleService {
    void simpleMethod();
}

class SimpleService implements ISimpleService {
    // Note: Does NOT implement IConstructorCalledWith

    @Override
    public void simpleMethod() {
        // This service tests graceful handling of objects without constructor logging
    }
}