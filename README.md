# SpecRec for Java

**Automated Legacy Testing Tools for Java**

![Spec Rec Logo](./SpecRecLogo.png)

## Overview

SpecRec makes legacy code testable through automated instrumentation and transparent record-replay capabilities. By replacing direct object instantiation with controllable factories and wrapping dependencies with recording proxies, SpecRec eliminates the manual effort required to characterize and test existing systems.

**⚠️ This library is incomplete and under active development. Currently the ObjectFactory and CallLogger components are implemented.**

## Core Principles

- **Prioritize ease of use** - Hide complexity behind well-designed interfaces
- **Focus on public interactions** - Test behavior, not implementation details  
- **Minimal code changes** - Validate changes at a glance

## Current Components

### ObjectFactory

Replaces `new` keyword with controllable dependency injection for testing.

#### Usage example

Suppose you have an inconvenient `Repository` dependency that implements `IRepository`.

```java
class MyService {
    public void complexOperation() {
        // Long and gnarly code
        Repository repository = new Repository("rcon://user:pwd@example.com/");
        Item item = repository.fetchById(id);
        // More code using the repository
    }
}
```

In many cases it is easy to break the dependency, but it can prove challenging if the call is several layers in.
In such situations you can use ObjectFactory to break the dependency with minimal change:

```java
import static com.specrec.GlobalObjectFactory.*;

class MyService {
    public void complexOperation() {
        // Long and gnarly code
        IRepository repository = create(IRepository.class, Repository.class).with("rcon://user:pwd@example.com/");
        Item item = repository.fetchById(id);
        // More code using the repository
    }
}
```

Now you can easily inject a test double:

```java
public class MyServiceTests {
    private final ObjectFactory factory = new ObjectFactory();
    
    @Test
    public void testWithTestDouble() {
        // Arrange
        FakeRepository fakeRepo = new FakeRepository();
        factory.setOne(IRepository.class, fakeRepo);
        
        // Act
        MyService.complexOperation();
        
        // Assert
        // ...
    }
}
```

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
MyMockClass mockObj = new MyMockClass();
factory.setOne(MyClass.class, mockObj);

MyClass result = factory.create(MyClass.class).with();
// result == mockObj

// Always return the same object
factory.setAlways(MyClass.class, mockObj);
```

#### Global Factory (Static Access)

```java
import static com.specrec.GlobalObjectFactory.*;

// Use anywhere without creating factory instances
MyClass obj = create(MyClass.class).with("arg1", 42);
IMyInterface obj = create(IMyInterface.class, MyImplementation.class).with();
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


### CallLogger

Records method calls and constructor invocations to generate human-readable specifications for testing and documentation.

#### Usage example

When you have an untested legacy system it can become tedious to manually create tests. Part of that complexity
comes from setting up mocks/spies manually. 

The CallLogger solves this by creating a SpecBook that contains calls to specific outside collaborators that can
then be approved using an approval testing framework. 

First wrap the dependencies to automatically log all interactions:

```java
@Test
public void myServiceTest() {
    CallLogger logger = new CallLogger();
    
    // Create a dummy, fake or stub
    FakeMessenger fakeMessenger = new FakeMessenger();

    // Wrap it with the logger
    IMessenger loggedMessenger = logger.wrap(fakeMessenger, "📩");
    
    // Set up factory to return logged objects
    factory.setOne(IMessenger.class, loggedMessenger);
    
    // Execute the operation
    new MyService().process();
    
    // Get human-readable specification
    String expectedLog = logger.getSpecBook().toString();
    
    // Verify the result
    Approvals.verify(logger.getSpecBook().toString());
}
```

The resulting SpecBook will look something like this:

```
📩 sendMessage:
  🔸 recipient: user@example.com
  🔸 subject: Welcome
  🔸 body: Hello and welcome!
  🔹 Returns: true
```

#### Specbook Format

Method call format:
```
📩 methodName:
  🔸 parameter_name: parameter_value
  🔹 Returns: return_value
```

Constructor call format:
```
📩 IInterfaceName constructor called with:
  🔸 parameter_name: parameter_value
  🔸 parameter_name2: parameter_value2
```

#### Shared SpecBook

Sometimes you may want to add your own logs to the SpecBook. Just create a string builder and pass it in:

```java
StringBuilder sharedSpecBook = new StringBuilder();
CallLogger logger = new CallLogger(sharedSpecBook);

sharedSpecBook.append("🧪 Test: User Authentication Flow\n");

IAuthService wrappedAuth = logger.wrap(authService, "🔐");
IUserService wrappedUser = logger.wrap(userService, "👤");

// Both services log to the same specification
wrappedAuth.login("user", "pass");
wrappedUser.getProfile(userId);

sharedSpecBook.append("✅ Authentication completed\n");
```

#### Constructor Logging

When used with the ObjectFactory, Objects implementing `IConstructorCalledWith` will have their constructor calls
logged as well. 

In some cases if a matching constructor is not found then the default `arg0`, `arg1` etc. names are used. 
If you want you can customize constructor parameter names for such constructor calls:

```java
public class DatabaseService implements IDatabaseService, IConstructorCalledWith {
    public DatabaseService(String connectionString, int timeout) { }
    
    @Override
    public void constructorCalledWith(ConstructorParameterInfo[] parameters) {
        CallLogFormatterContext.setConstructorArgumentNames("dbConnection", "timeoutSeconds");
    }
}
```

#### Controlling Log Output

Use `CallLogFormatterContext` within methods to control what gets logged:

```java
public void processSecretData(String publicData, String secretKey) {
    CallLogFormatterContext.ignoreArgument(1); // Hide secretKey
    CallLogFormatterContext.addNote("Processing with security protocols");
    // Method logic here
}

public String getAuthToken() {
    CallLogFormatterContext.ignoreReturnValue(); // Hide sensitive return value
    return "secret-token";
}

public void internalMethod() {
    CallLogFormatterContext.ignoreCall(); // Skip logging this call entirely
}
```

#### Manual Logging

Although not advised, you have the option to log calls manually. 

```java
CallLogger logger = new CallLogger();

// Build detailed call logs manually
logger.withArgument("user123", "userId")
    .withArgument(true, "isActive")
    .withNote("Validates user permissions")
    .withReturn("authorized")
    .log("checkUserAccess");

String spec = logger.getSpecBook().toString();
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

For approval testing with CallLogger, also add:

```xml
<dependency>
    <groupId>com.approvaltests</groupId>
    <artifactId>approvaltests</artifactId>
    <version>22.3.3</version>
    <scope>test</scope>
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
