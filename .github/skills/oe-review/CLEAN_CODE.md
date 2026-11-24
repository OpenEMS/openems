# What Is Clean Code for Us?

> Note: the repository `AGENTS.md` (section "Working Style") already defines the baseline - self-explanatory code with clear names, comments only when they explain non-obvious intent (in English), reuse of existing utilities instead of hand-rolled helpers, and small focused changes. Apply those as well; they are not repeated here. This guide adds the review-specific depth on top.

From the notes of the last meeting, several recurring ideas become visible:

1. **Code should explain itself as much as possible**

Several points go in this direction: self-explanatory variable names, understandable code, code that explains itself, and comments only where they are truly necessary.

2. **Clean Code should be maintainable and extensible**

It was repeatedly mentioned that code should be easy to extend, but not overengineered. Future development should also be considered, without adding unnecessary complexity.

3. **Clean Code should be stable and testable**

Good code is not only pleasant to read, but also easy to test and stable in its behavior.

4. **Reuse and avoiding boilerplate are important**

Code reuse and less boilerplate were also mentioned. This matches topics like DRY, KISS, and clean abstractions.

5. **Refactoring is part of it**

Clean Code is not created only when writing new code, but also through small improvements to existing code. This fits well with the Boy Scout Rule.

---

## The Boy Scout Rule

Always leave code a little cleaner than you found it.

This means small, pragmatic day-to-day improvements - not major rewrites.

---

## Clean Code: Naming - Most Important Points

1. **Do not use unnecessary abbreviations**

Names should be directly understandable and pronounceable.

Example: `movieRelationScore` instead of `relScore`, `generationTimestamp` instead of `genymdhms`.

2. **Make meaningful distinctions**

Names like `accountData`, `accountInfo`, or `moneyAmount` are often too vague if they do not explain a real difference.

Better: choose names so it is clear what is actually meant.

3. **Include units in the name when the type does not express them**

Especially important for energy, power, time, percentages, etc.

Example: `gridBuyEnergyWh` instead of `gridBuyEnergy`, `socPercentage` instead of just `soc`.

4. **Names should express domain meaning, not technical details**

Prefixes like `I`, `Base`, or `Abstract` often say more about implementation than meaning.

Better: `Movable` instead of `IMovable` or `AbstractMovable`.

5. **Be careful with utility classes**

If a class is called `Utils`, it often lacks a clear responsibility. In many cases, it is better to move methods into suitable domain objects.

Example: `movie.relationScore(otherMovie)` instead of `Utils.relationScore(movie1, movie2)`.

## Comments

1. **Comments are often a hint that the code is not clear enough**

Before writing a comment, check whether the intent can be expressed better in code.

Example:

```java
// Check to see if the employee is eligible for full benefits
if ((employee.flags & HOURLY_FLAG) && (employee.age > 65)) {
...
}
```

Better:

```java
if (employee.isEligibleForFullBenefits()) {
...
}
```

The comment becomes unnecessary because the method name directly expresses the intent.

2. **Comments can become outdated or wrong**

Code changes; comments often do not. Over time, comments can lie, even if this is not intended.

3. **Good comments explain the "why," not the "what"**

Comments are useful when they explain technical constraints, domain reasons, or unusual behavior.

Example:

```java
// Battery sends 0x00 byte at the beginning of F42 responses - skip it
```

4. **Use JavaDoc selectively for real public APIs**

JavaDoc is especially useful for APIs used by other modules/bundles.

Trivial getters/setters or self-explanatory methods usually do not need JavaDoc.

## Functions

1. **Make them small**

Functions should be as short as possible. Ideally, they consist of fewer than 20 lines of code.

2. **Do one thing (SRP)**

A function should have exactly one responsibility.

It should do that one thing well - and only that.

3. **One level of abstraction**

Mixed abstraction levels make code hard to understand.

Within a function, only one abstraction level should be used.

4. **Function arguments**

| Term    | Count | Recommendation                                                             |
|---------|-------|----------------------------------------------------------------------------|
| Niladic | 0     | Ideal. Easiest to read and test.                                           |
| Monadic | 1     | Very good for transformations. A boolean should never be the only argument. |
| Dyadic  | 2     | Useful for logically related values (e.g., x and y).                       |
| Triadic | 3     | Avoid whenever possible. Increases mental complexity.                      |
| Polyadic| 4+    | Use an object or class instead.                                            |

5. **Side effects & CQS**

Functions should do exactly what their name promises.

A function like `checkPassword()` should not also initialize a session. Hidden side effects often lead to hard-to-trace bugs.

**Command Query Separation (CQS)**
A function should either:

- perform an action (Command), or
- return a value (Query),

but never both at the same time.

## Objects and Data Structures

1. **When to use objects and data structures**

Use **objects** when:

- data should be hidden and behavior exposed,
- adding new types in the future is likely.

Use **data structures** when:

- data should be provided directly,
- adding new operations in the future is likely.

2. **Hybrids**

Hybrids are a mix of objects and data structures. They should be avoided as much as possible.

3. **The Law of Demeter**

Objects should communicate only with their immediate surroundings.

Methods should only call methods of:

- their own class,
- an object created within the method,
- an object passed as a parameter,
- an instance variable of their class.

4. **Train wrecks**

Train wrecks are chains of instance method calls where each call returns another object. They should be avoided as much as possible.

Example:

```java
a.getB()
 .getC()
 .getD()
 .doSomething();
```

