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
