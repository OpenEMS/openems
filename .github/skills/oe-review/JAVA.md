## Required References

Before applying this skill, read these files with the read_file tool:

- `AGENTS.md`
- `.github/skills/oe-review/CLEAN_CODE.md`

# Java Code Review Guide

Java review focus areas: Java 21/25 new features, concurrent programming (virtual threads), and code maintainability.

## Table of Contents

- Modern Java Features (21/25+)

- Stream API & Optional

- Concurrency & Virtual Threads

- Exception Handling

- Review Checklist

---

## Modern Java Features (21/25+)

### Record Classes

```java

// ❌ Traditional POJO/DTO: too much boilerplate

public class UserDto {

    private final String name;

    private final int age;
 
    public UserDto(String name, int age) {

        this.name = name;

        this.age = age;

    }

    // getters, equals, hashCode, toString...

}
 
// ✅ Use Record: concise, immutable, semantically clear

public record UserDto(String name, int age) {

    // Compact constructor for validation

    public UserDto {

        if (age < 0) throw new IllegalArgumentException("Age cannot be negative");

    }

}

```

### Switch Expressions & Pattern Matching

```java

// ❌ Traditional switch: easy to forget break, verbose and error-prone

String type = "";

switch (obj) {

    case Integer i: // Java 16+

        type = String.format("int %d", i);

        break;

    case String s:

        type = String.format("string %s", s);

        break;

    default:

        type = "unknown";

}
 
// ✅ Switch expression: no fall-through risk, enforces return value

String type = switch (obj) {

    case Integer i -> "int %d".formatted(i);

    case String s  -> "string %s".formatted(s);

    case null      -> "null value"; // Java 21 null handling

    default        -> "unknown";

};

```

### Text Blocks

```java

// ❌ Concatenating SQL/JSON strings

String json = "{\n" +

              "  \"name\": \"Alice\",\n" +

              "  \"age\": 20\n" +

              "}";
 
// ✅ Use text blocks: what you see is what you get

String json = """

    {

      "name": "Alice",

      "age": 20

    }

    """;

```
 
---

## Stream API & Optional

### Avoid Overusing Stream

```java

// ❌ Simple loops don't need Stream (performance overhead + reduced readability)

items.stream().forEach(item -> {

    process(item);

});
 
// ✅ Use for-each directly in simple scenarios

for (var item : items) {

    process(item);

}
 
// ❌ Overly complex Stream chains

List<Dto> result = list.stream()

    .filter(...)

    .map(...)

    .peek(...)

    .sorted(...)

    .collect(...); // hard to debug
 
// ✅ Split into meaningful steps

var filtered = list.stream().filter(...).toList();

// ...

```

### Correct Optional Usage

```java

// ❌ Using Optional as a parameter or field (serialization issues, increased call complexity)

public void process(Optional<String> name) { ... }

public class User {

    private Optional<String> email; // not recommended

}
 
// ✅ Optional only as a return type

public Optional<User> findUser(String id) { ... }
 
// ❌ Using isPresent() + get() defeats the purpose of Optional

Optional<User> userOpt = findUser(id);

if (userOpt.isPresent()) {

    return userOpt.get().getName();

} else {

    return "Unknown";

}
 
// ✅ Use the functional API

return findUser(id)

    .map(User::getName)

    .orElse("Unknown");

```
 
---

## Concurrency & Virtual Threads

### Virtual Threads (Java 21+)

```java

// ❌ Traditional thread pool for large numbers of I/O-blocking tasks (resource exhaustion)

ExecutorService executor = Executors.newFixedThreadPool(100);
 
// ✅ Use virtual threads for I/O-intensive tasks (high throughput)

ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
 
// With virtual threads, blocking operations (DB queries, HTTP requests) barely consume OS thread resources

```

### Thread Safety

```java

// ❌ SimpleDateFormat is not thread-safe

private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
 
// ✅ Use DateTimeFormatter (Java 8+)

private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
 
// ❌ HashMap in a multi-threaded environment causes data loss

// ✅ Use ConcurrentHashMap

Map<String, String> cache = new ConcurrentHashMap<>();

```
 
---

## Exception Handling

### Global Exception Handling

```java

// ❌ Scattering try-catch everywhere that swallows exceptions or just prints stack traces

try {
    userService.create(user);
} catch (Exception e) {
    e.printStackTrace(); // never use in production
    return null; // silences the exception, caller has no idea what happened
}

public class UserNotFoundException extends RuntimeException { ... }
 

public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleNotFound(UserNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

}

// ✅ Use the built-in OpenemsException or an appropriate OpenemsError for API errors.
// Propagate existing OpenEMS exceptions unchanged. When converting other exceptions,
// wrap them in OpenemsException or map them to an appropriate OpenemsError and throw
// its exception(...) result. Do not swallow exceptions and do only introduce custom 
// API exception types where needed.

```
 
---

## Review Checklist

### Basics & Standards

- [ ] Modern Java 21/25 features used where appropriate (switch expressions, records, text blocks)

- [ ] Deprecated classes avoided (`Date`, `Calendar`, `SimpleDateFormat`)

- [ ] Collection operations prefer Stream API or `Collections` utility methods

- [ ] `Optional` used only as a return type, not as a field or parameter

### Concurrency & Performance

- [ ] I/O-intensive tasks consider virtual threads

- [ ] Thread-safe classes used correctly (`ConcurrentHashMap` vs `HashMap`)

- [ ] Lock granularity is appropriate; no I/O inside locked sections

### Maintainability

- [ ] Critical business logic has sufficient unit test coverage

- [ ] Logging is appropriate (Slf4j used; no `System.out`)

- [ ] Magic values extracted into constants or enums
