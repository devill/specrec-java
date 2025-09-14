# Concrete Class Support in SpecRec Java

SpecRec Java now supports wrapping both interfaces and concrete classes for call logging.

## Overview

The Java version of SpecRec previously only supported interface-based proxying using JDK Dynamic Proxy. This limitation meant that concrete classes like `java.util.Random` could not be wrapped for logging.

Version 0.0.3+ adds support for concrete class wrapping using **CGLib**, bringing the Java implementation closer to feature parity with the C# version which uses Castle DynamicProxy.

## How It Works

The `UnifiedProxyFactory` automatically chooses the appropriate proxy strategy:

1. **Interface Types**: Uses JDK Dynamic Proxy (existing behavior)
2. **Concrete Classes with Interfaces**: Uses JDK Dynamic Proxy for backward compatibility
3. **Concrete Classes without Interfaces**: Uses CGLib proxy

## Usage

No changes are required to existing code. The `CallLogger.wrap()` method now automatically supports both interfaces and concrete classes:

```java
// Works with interfaces (as before)
IEmailService emailService = logger.wrap(IEmailService.class, new EmailServiceImpl(), "📧");

// Now also works with concrete classes!
Random random = logger.wrap(Random.class, new Random(), "🎲");
```

## Requirements for Concrete Classes

For a concrete class to be wrappable with CGLib, it must meet these criteria:

1. **Not final**: The class cannot be declared `final`
2. **Has public constructor**: At least one public constructor must be available
3. **Has interceptable methods**: At least one public, non-final, non-static method (excluding Object methods)

### Checking Compatibility

You can check if a class can be proxied:

```java
// Check if a type can be proxied
boolean canProxy = UnifiedProxyFactory.canCreateProxy(Random.class); // true

// Get detailed explanation
String limitation = UnifiedProxyFactory.getProxyLimitation(String.class);
// Returns: "Final classes cannot be proxied. Consider wrapping in an interface."

// Get the proxy strategy that would be used
ProxyStrategy strategy = UnifiedProxyFactory.getProxyStrategy(Random.class);
// Returns: ProxyStrategy.CGLIB_PROXY
```

## Examples

### Working Examples

```java
// JDK classes that can be proxied
Random random = logger.wrap(Random.class, new Random(), "🎲");
ArrayList<String> list = logger.wrap(ArrayList.class, new ArrayList<>(), "📋");
StringBuilder sb = logger.wrap(StringBuilder.class, new StringBuilder(), "🔤");

// Custom classes
public class MyService {
    public void doWork() { ... }
    public String getName() { ... }
}

MyService service = logger.wrap(MyService.class, new MyService(), "🔧");
```

### Classes That Cannot Be Proxied

```java
// Final classes
String str = ...; // Cannot proxy String (final class)
Integer num = ...; // Cannot proxy Integer (final class)

// Classes with all final methods
// Most immutable classes fall into this category
```

## Java Module System Compatibility

When running on Java 9+ with the module system, you may need to add JVM arguments to allow CGLib to work:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.1.2</version>
    <configuration>
        <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED</argLine>
    </configuration>
</plugin>
```

## Limitations

### Current Limitations

1. **Parrot Mode**: Parrot mode (replay without target) is not yet implemented for concrete classes
2. **Final Methods**: CGLib cannot intercept final methods
3. **Static Methods**: Static methods are not intercepted
4. **Private/Protected Methods**: Only public methods are intercepted

### Workarounds

For classes that cannot be proxied:

1. **Create Interface Wrapper**: Wrap the class in an interface you control
2. **Manual Stubs**: Create manual implementations using `CallLogFormatterContext.LoggedReturnValue<T>()`
3. **Composition Pattern**: Use composition instead of inheritance

Example interface wrapper:
```java
public interface IRandomService {
    int nextInt(int bound);
    void setSeed(long seed);
}

public class RandomServiceWrapper implements IRandomService {
    private final Random random;

    public RandomServiceWrapper(Random random) {
        this.random = random;
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public void setSeed(long seed) {
        random.setSeed(seed);
    }
}
```

## Implementation Details

### Architecture

- `UnifiedProxyFactory`: Chooses appropriate proxy strategy
- `ConcreteClassProxyFactory`: Handles CGLib-based concrete class proxying
- `CallLoggerProxy`: Handles JDK Dynamic Proxy for interfaces (existing)
- `CallLogger.wrap()`: Updated to use UnifiedProxyFactory

### Dependencies Added

- **CGLib 3.3.0**: For concrete class proxy generation

### Backward Compatibility

All existing interface-based code continues to work without changes. The implementation prefers interface proxying when possible for maximum compatibility.

## Troubleshooting

### Common Issues

**ExceptionInInitializerError with CGLib**
- Cause: Java module system restrictions
- Solution: Add `--add-opens` JVM arguments (see Java Module System section)

**IllegalArgumentException: "Cannot create proxy for class X"**
- Cause: Class doesn't meet CGLib requirements (final, no public methods, etc.)
- Solution: Check requirements or use interface wrapper pattern

**NoClassDefFoundError: CGLib classes**
- Cause: CGLib dependency not available
- Solution: Ensure CGLib 3.3.0+ is in classpath

### Getting Help

1. Use `UnifiedProxyFactory.getProxyLimitation(YourClass.class)` for specific error messages
2. Check that your class meets the requirements listed above
3. Consider using interface wrappers for problematic classes