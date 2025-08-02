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
