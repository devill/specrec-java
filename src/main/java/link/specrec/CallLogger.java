package link.specrec;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.*;
import java.util.stream.Collectors;

public class CallLogger {
    final StringBuilder specBook;
    private final String emoji;
    private Object returnValue;
    private String note;
    private final List<Parameter> parameters = new ArrayList<>();
    private String methodName;
    private String forcedInterfaceName;

    // Package-private fields for ignored calls/arguments/returns (used by CallLogFormatterContext)
    final Map<String, Set<Integer>> ignoredArguments = new HashMap<>();
    final Set<String> ignoredCalls = new HashSet<>();
    final Set<String> ignoredAllArguments = new HashSet<>();
    final Set<String> ignoredReturnValues = new HashSet<>();

    public CallLogger() {
        this(null, "");
    }

    public CallLogger(StringBuilder specBook) {
        this(specBook, "");
    }

    public CallLogger(StringBuilder specBook, String emoji) {
        this.specBook = specBook != null ? specBook : new StringBuilder();
        this.emoji = emoji != null ? emoji : "";
    }

    public StringBuilder getSpecBook() {
        return specBook;
    }

    public <I> I wrap(Class<I> type, I target, String emoji) {
        return UnifiedProxyFactory.createLoggingProxy(type, target, this, emoji);
    }

    public <I> I wrap(Class<I> type, I target) {
        return wrap(type, target, "🔧");
    }

    public CallLogger withReturn(Object returnValue, String description) {
        this.returnValue = returnValue;
        return this;
    }

    public CallLogger withReturn(Object returnValue) {
        return withReturn(returnValue, null);
    }

    public CallLogger withNote(String note) {
        this.note = note;
        return this;
    }

    public CallLogger withArgument(Object value, String name) {
        String paramName = name != null ? name : "Arg" + parameters.size();
        parameters.add(new Parameter(paramName, value, "🔸"));
        return this;
    }

    public CallLogger withArgument(Object value) {
        return withArgument(value, null);
    }

    public CallLogger withOut(Object value, String name) {
        String paramName = name != null ? name : "Out" + parameters.size();
        parameters.add(new Parameter(paramName, value, "♦️"));
        return this;
    }

    public CallLogger withOut(Object value) {
        return withOut(value, null);
    }

    public CallLogger forInterface(String interfaceName) {
        this.forcedInterfaceName = interfaceName;
        return this;
    }

    public void log(String methodName) {
        this.methodName = methodName;

        if ("constructorCalledWith".equals(methodName)) {
            logConstructorCall();
        } else {
            logMethodCall();
        }

        // Clear parameters for next call
        parameters.clear();
        returnValue = null;
        note = null;
        forcedInterfaceName = null;
    }

    public void log() {
        // Get caller method name using stack trace
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String callerMethodName = stack.length > 2 ? stack[2].getMethodName() : "UnknownMethod";
        log(callerMethodName);
    }

    private void logConstructorCall() {
        String interfaceName = forcedInterfaceName != null ? forcedInterfaceName : getInterfaceName();
        specBook.append(emoji).append(" ").append(interfaceName).append(" constructor called with:\n");

        for (Parameter param : parameters) {
            specBook.append("  ").append(param.emoji).append(" ")
                    .append(param.name).append(": ").append(param.value).append("\n");
        }

        specBook.append("\n");
    }

    private void logMethodCall() {
        specBook.append(emoji).append(" ").append(methodName).append(":\n");

        for (Parameter param : parameters) {
            String formattedValue = formatValue(param.value);
            specBook.append("  ").append(param.emoji).append(" ")
                    .append(param.name).append(": ").append(formattedValue).append("\n");
        }

        if (note != null && !note.isEmpty()) {
            specBook.append("  🗒️ ").append(note).append("\n");
        }

        if (returnValue != null) {
            String formattedReturn = formatValue(returnValue);
            specBook.append("  🔹 Returns: ").append(formattedReturn).append("\n");
        }

        specBook.append("\n");
    }

    private String getInterfaceName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (stack.length > 4) {
            try {
                String className = stack[4].getClassName();
                Class<?> clazz = Class.forName(className);
                return CallLoggerProxy.findMainInterfaceFromClass(clazz);
            } catch (ClassNotFoundException e) {
                // Fall back to class name from stack trace
                String className = stack[4].getClassName();
                return className.substring(className.lastIndexOf('.') + 1);
            }
        }
        return "Unknown";
    }

    private String formatValue(Object value) {
        if (value == null) return "null";

        if (tryFormatCollection(value)) {
            return formatCollection(value);
        }

        if (tryFormatNumericType(value)) {
            return formatNumericType(value);
        }

        if (tryFormatDateTime(value)) {
            return formatDateTime(value);
        }

        return value.toString();
    }

    private boolean tryFormatCollection(Object value) {
        return (value instanceof Iterable && !(value instanceof String)) || value.getClass().isArray();
    }

    private String formatCollection(Object value) {
        if (value.getClass().isArray()) {
            return formatArray(value);
        }
        
        if (value instanceof Iterable) {
            List<String> items = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                items.add(formatValue(item));
            }
            return String.join(",", items);
        }
        return value.toString();
    }
    
    private String formatArray(Object array) {
        if (array instanceof int[]) {
            int[] intArray = (int[]) array;
            List<String> items = new ArrayList<>();
            for (int item : intArray) {
                items.add(String.valueOf(item));
            }
            return String.join(",", items);
        } else if (array instanceof Object[]) {
            Object[] objArray = (Object[]) array;
            List<String> items = new ArrayList<>();
            for (Object item : objArray) {
                items.add(formatValue(item));
            }
            return String.join(",", items);
        } else if (array instanceof double[]) {
            double[] doubleArray = (double[]) array;
            List<String> items = new ArrayList<>();
            for (double item : doubleArray) {
                items.add(formatValue(item));
            }
            return String.join(",", items);
        } else if (array instanceof float[]) {
            float[] floatArray = (float[]) array;
            List<String> items = new ArrayList<>();
            for (float item : floatArray) {
                items.add(formatValue(item));
            }
            return String.join(",", items);
        }
        // Add more array types as needed
        return array.toString();
    }

    private boolean tryFormatNumericType(Object value) {
        return value instanceof Number;
    }

    private String formatNumericType(Object value) {
        if (value instanceof Float) {
            // Use DecimalFormat to match expected precision
            DecimalFormat df = new DecimalFormat("#.##");
            return df.format(value);
        } else if (value instanceof Double) {
            DecimalFormat df = new DecimalFormat("#.##########");
            return df.format(value);
        }
        return value.toString();
    }

    private boolean tryFormatDateTime(Object value) {
        return value instanceof Date;
    }

    private String formatDateTime(Object value) {
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format((Date) value);
        }
        return value.toString();
    }

    private static class Parameter {
        final String name;
        final Object value;
        final String emoji;

        Parameter(String name, Object value, String emoji) {
            this.name = name;
            this.value = value;
            this.emoji = emoji;
        }
    }
}