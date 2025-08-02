# SpecRec for Java

**Automated Legacy Testing Tools for Java**

## Overview

SpecRec makes legacy code testable through automated instrumentation and transparent record-replay capabilities. By replacing direct object instantiation with controllable factories and wrapping dependencies with recording proxies, SpecRec eliminates the manual effort required to characterize and test existing systems.

**⚠️ This library is incomplete and under active development. Currently only the ObjectFactory component is implemented.**

## Core Principles

- **Prioritize ease of use** - Hide complexity behind well-designed interfaces
- **Focus on public interactions** - Test behavior, not implementation details  
- **Minimal code changes** - Validate changes at a glance

## Current Components

### ObjectFactory

Replaces `new` keyword with controllable dependency injection for testing.

#### Basic Usage

```java
// Create objects normally
ObjectFactory factory = new ObjectFactory();
MyClass obj = factory.create(MyClass.class).with();

// With constructor arguments
MyClass obj = factory.create(MyClass.class).with("arg1", 42);

// Interface/implementation pattern
IMyInterface obj = factory.create(IMyInterface.class, MyImplementation.class).with();
```

#### Test Object Injection

```java
// Queue specific objects for testing
MyClass mockObj = new MyMockClass();
factory.setOne(MyClass.class, mockObj);

MyClass result = factory.create(MyClass.class).with();
// result == mockObj

// Always return the same object
factory.setAlways(MyClass.class, mockObj);
```

#### Production Code Usage (Static Access)

```java
import static com.specrec.GlobalObjectFactory.*;

public class MyService {
    public void processUser(String username, int age) {
        // Replace 'new UserProcessor(username, age)' with factory call
        UserProcessor processor = create(UserProcessor.class).with(username, age);
        processor.process();
        
        // Interface/implementation pattern
        IEmailService emailService = create(IEmailService.class, SmtpEmailService.class).with();
        emailService.sendWelcomeEmail(username);
    }
}
```

#### Constructor Parameter Logging

Objects implementing `IConstructorCalledWith` receive detailed parameter information:

```java
public class MyMock implements IMyInterface, IConstructorCalledWith {
    private ConstructorParameterInfo[] lastParams;
    
    @Override
    public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
        this.lastParams = parameters;
        // parameters[0].getName() == "username"
        // parameters[0].getType() == String.class  
        // parameters[0].getValue() == "john"
    }
}
```

#### Management Operations

```java
// Clear specific type
factory.clear(MyClass.class);

// Clear all registered objects
factory.clearAll();
```

#### Example Test with Constructor Tracking

```java
@Test
public void testUserServiceWithMockedDependencies() {
    // Arrange
    ObjectFactory factory = new ObjectFactory();
    
    // Create mock that tracks constructor calls
    MockEmailService mockEmailService = new MockEmailService();
    factory.setOne(IEmailService.class, mockEmailService);
    
    MockUserRepository mockUserRepo = new MockUserRepository();
    factory.setOne(IUserRepository.class, mockUserRepo);
    
    // Act - create service with mocked dependencies
    UserService service = factory.create(UserService.class).with("smtp.example.com", 587);
    service.registerUser("john", "john@example.com", 25);
    
    // Assert - verify constructor parameters were tracked
    assertNotNull(mockEmailService.getLastParameterDetails());
    assertEquals(2, mockEmailService.getLastParameterDetails().length);
    assertEquals("host: String = smtp.example.com", 
                 mockEmailService.getLastParameterDetails()[0].toString());
    assertEquals("port: int = 587", 
                 mockEmailService.getLastParameterDetails()[1].toString());
    
    // Verify interactions
    assertEquals("john@example.com", mockEmailService.getLastEmailSent());
    assertEquals("john", mockUserRepo.getLastUserSaved().getName());
}

// Mock implementations
public class MockEmailService implements IEmailService, IConstructorCalledWith {
    private ConstructorParameterInfo[] lastParams;
    private String lastEmailSent;
    
    @Override
    public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
        this.lastParams = parameters;
    }
    
    @Override
    public void sendWelcomeEmail(String email) {
        this.lastEmailSent = email;
    }
    
    public ConstructorParameterInfo[] getLastParameterDetails() { return lastParams; }
    public String getLastEmailSent() { return lastEmailSent; }
}
```

## Maven Configuration

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.specrec</groupId>
    <artifactId>specrec</artifactId>
    <version>0.0.1</version>
</dependency>
```

**Note**: For constructor parameter name extraction, ensure your Maven compiler preserves parameter names:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

## Planned Components

- **Surveyor**: Transparent spy/mock that logs all interactions
- **SpecReplay**: Replay recorded specifications  
- **SpecBook**: Human-readable test recording format
- **Instrumentation Interface**: Language-specific automation tools

## Requirements

- Java 11+
- Maven 3.6+

## License

PolyForm Noncommercial License 1.0.0
