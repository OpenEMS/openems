# TypeScript/JavaScript Code Review Guide

> TypeScript code review guide covering core topics such as the type system, generics, conditional types, strict mode, and async/await patterns.

## Table of Contents

- [Type Safety Basics](#type-safety-basics)
- [Generic Patterns](#generic-patterns)
- [Advanced Types](#advanced-types)
- [Strict Mode Configuration](#strict-mode-configuration)
- [Asynchronous Handling](#asynchronous-handling)
- [Immutability](#immutability)
- [ESLint Rules](#eslint-rules)
- [Testing](#testing)
- [Module Resolution](#module-resolution)
- [TS 4.9+ / 5.x New Features](#ts-49--5x-new-features)
- [Review Checklist](#review-checklist)

---

## Type Safety Basics

### Avoid using `any`

```typescript
// ❌ Using any defeats type safety
function processData(data: any) {
  return data.value; // No type checking; may crash at runtime
}

// ✅ Use proper types
interface DataPayload {
  value: string;
}
function processData(data: DataPayload) {
  return data.value;
}

// ✅ For unknown types, use unknown + type guards
function processUnknown(data: unknown) {
  if (typeof data === "object" && data !== null && "value" in data) {
    return (data as { value: string }).value;
  }
  throw new Error("Invalid data");
}
```

### Type narrowing

```typescript
// ❌ Unsafe type assertion
function getLength(value: string | string[]) {
  return (value as string[]).length; // Fails if value is string
}

// ✅ Use type guards
function getLength(value: string | string[]): number {
  if (Array.isArray(value)) {
    return value.length;
  }
  return value.length;
}

// ✅ Use the in operator
interface Dog {
  bark(): void;
}
interface Cat {
  meow(): void;
}

function speak(animal: Dog | Cat) {
  if ("bark" in animal) {
    animal.bark();
  } else {
    animal.meow();
  }
}
```

### Literal types and `as const`

```typescript
// ❌ Type is too broad
const config = {
  endpoint: '/api',
  method: 'GET'  // Type is string
};

// ✅ Use as const to get literal types
const config = {
  endpoint: '/api',
  method: 'GET'
} as const;  // method type is 'GET'

// ✅ Useful for function arguments
function request(method: 'GET' | 'POST', url: string) { ... }
request(config.method, config.endpoint);  // Correct!
```

### Avoid declaring `undefined` explicitly

```typescript
// ❌ `| undefined` on a property forces callers to always pass the key
// (with exactOptionalPropertyTypes, `{ timeout: undefined }` is required)
interface Config {
  timeout: number | undefined;
}

// ✅ Optional property: the key can simply be omitted
interface Config {
  timeout?: number;
}
const config: Config = {};
```

```typescript
// ❌ Optional parameter written as a union
function greet(name: string | undefined) { ... }

// ✅ Optional parameter
function greet(name?: string) { ... }
```

```typescript
// ❌ Redundant initializer (also flagged by ESLint `no-undef-init`)
let name: string | undefined = undefined;
const config = { retries: undefined };

// ✅ Let the type express it; omit the value entirely
let name: string | undefined;
const typedConfig: { retries?: number } = {};
```

```typescript
// ❌ Return type `undefined`
function log(message: string): undefined {
  console.log(message);
  return undefined;
}

// ✅ Use `void` for functions that return nothing
function log(message: string): void {
  console.log(message);
}
```

---

## Generic Patterns

### Basic generics

```typescript
// ❌ Duplicate code
function getFirstString(arr: string[]): string | undefined {
  return arr[0];
}
function getFirstNumber(arr: number[]): number | undefined {
  return arr[0];
}

// ✅ Use generics
function getFirst<T>(arr: T[]): T | undefined {
  return arr[0];
}
```

### Generic constraints

```typescript
// ❌ Generic has no constraint; property access is not safe
function getProperty<T>(obj: T, key: string) {
  return obj[key]; // Error: cannot index
}

// ✅ Use keyof constraint
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key];
}

const user = { name: "Alice", age: 30 };
getProperty(user, "name"); // Return type is string
getProperty(user, "age"); // Return type is number
getProperty(user, "foo"); // Error: 'foo' is not in keyof User
```

### Generic defaults

```typescript
// ✅ Provide sensible default types
interface ApiResponse<T = unknown> {
  data: T;
  status: number;
  message: string;
}

// Generic parameter can be omitted
const response: ApiResponse = { data: null, status: 200, message: 'OK' };
// Or specified explicitly
const userResponse: ApiResponse<User> = { ... };
```

### Common generic utility types

```typescript
// ✅ Use built-in utility types
interface User {
  id: number;
  name: string;
  email: string;
}

type PartialUser = Partial<User>; // All properties optional
type RequiredUser = Required<User>; // All properties required
type ReadonlyUser = Readonly<User>; // All properties readonly
type UserKeys = keyof User; // 'id' | 'name' | 'email'
type NameOnly = Pick<User, "name">; // { name: string }
type WithoutId = Omit<User, "id">; // { name: string; email: string }
type UserRecord = Record<string, User>; // { [key: string]: User }
```

---

## Advanced Types

### Conditional types

```typescript
// ✅ Return different types based on input type
type IsString<T> = T extends string ? true : false;

type A = IsString<string>; // true
type B = IsString<number>; // false

// ✅ Extract array element type
type ElementType<T> = T extends (infer U)[] ? U : never;

type Elem = ElementType<string[]>; // string

// ✅ Extract function return type (built-in ReturnType)
type MyReturnType<T> = T extends (...args: any[]) => infer R ? R : never;
```

### Mapped types

```typescript
// ✅ Transform all properties in an object type
type Nullable<T> = {
  [K in keyof T]: T[K] | null;
};

interface User {
  name: string;
  age: number;
}

type NullableUser = Nullable<User>;
// { name: string | null; age: number | null }

// ✅ Add prefixes
type Getters<T> = {
  [K in keyof T as `get${Capitalize<string & K>}`]: () => T[K];
};

type UserGetters = Getters<User>;
// { getName: () => string; getAge: () => number }
```

### Template literal types

```typescript
// ✅ Type-safe event names
type EventName = "click" | "focus" | "blur";
type HandlerName = `on${Capitalize<EventName>}`;
// 'onClick' | 'onFocus' | 'onBlur'

// ✅ API route types
type ApiRoute = `/api/${string}`;
const route: ApiRoute = "/api/users"; // OK
const badRoute: ApiRoute = "/users"; // Error
```

### Discriminated unions

```typescript
// ✅ Use a discriminant property for type safety
type Result<T, E> = { success: true; data: T } | { success: false; error: E };

function handleResult(result: Result<User, Error>) {
  if (result.success) {
    console.log(result.data.name); // TypeScript knows data exists
  } else {
    console.log(result.error.message); // TypeScript knows error exists
  }
}

// ✅ Redux Action pattern
type Action =
  | { type: "INCREMENT"; payload: number }
  | { type: "DECREMENT"; payload: number }
  | { type: "RESET" };

function reducer(state: number, action: Action): number {
  switch (action.type) {
    case "INCREMENT":
      return state + action.payload; // payload type is known
    case "DECREMENT":
      return state - action.payload;
    case "RESET":
      return 0; // no payload here
  }
}
```

---

## Strict Mode Configuration

### Recommended `tsconfig.json`

```json
{
  "compilerOptions": {
    // ✅ Strict options that should be enabled
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "strictFunctionTypes": true,
    "strictBindCallApply": true,
    "strictPropertyInitialization": true,
    "noImplicitThis": true,
    "useUnknownInCatchVariables": true,

    // ✅ Additional recommended options
    "noUncheckedIndexedAccess": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "exactOptionalPropertyTypes": true,
    "noPropertyAccessFromIndexSignature": true
  }
}
```

### Impact of `noUncheckedIndexedAccess`

```typescript
// tsconfig: "noUncheckedIndexedAccess": true

const arr = [1, 2, 3];
const first = arr[0]; // Type is number | undefined

// ❌ Direct usage may fail
console.log(first.toFixed(2)); // Error: possibly undefined

// ✅ Check first
if (first !== undefined) {
  console.log(first.toFixed(2));
}

// ✅ Or use non-null assertion (when you're certain)
console.log(arr[0]!.toFixed(2));
```

---

## Asynchronous Handling

### Promise error handling

```typescript
// ❌ Not handling async errors
async function fetchUser(id: string) {
  const response = await fetch(`/api/users/${id}`);
  return response.json(); // Network errors are not handled
}

// ✅ Handle errors properly
async function fetchUser(id: string): Promise<User> {
  try {
    const response = await fetch(`/api/users/${id}`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return await response.json();
  } catch (error) {
    if (error instanceof Error) {
      throw new Error(`Failed to fetch user: ${error.message}`);
    }
    throw error;
  }
}
```

### `Promise.all` vs `Promise.allSettled`

```typescript
// ❌ Promise.all: one failure fails everything
async function fetchAllUsers(ids: string[]) {
  const users = await Promise.all(ids.map(fetchUser));
  return users; // One failure causes total failure
}

// ✅ Promise.allSettled: collect all results
async function fetchAllUsers(ids: string[]) {
  const results = await Promise.allSettled(ids.map(fetchUser));

  const users: User[] = [];
  const errors: Error[] = [];

  for (const result of results) {
    if (result.status === "fulfilled") {
      users.push(result.value);
    } else {
      errors.push(result.reason);
    }
  }

  return { users, errors };
}
```

### Race condition handling

```typescript
// ❌ Race condition: older request may overwrite newer request
function useSearch() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);

  useEffect(() => {
    fetch(`/api/search?q=${query}`)
      .then((r) => r.json())
      .then(setResults); // Older request may return later!
  }, [query]);
}

// ✅ Use AbortController
function useSearch() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);

  useEffect(() => {
    const controller = new AbortController();

    fetch(`/api/search?q=${query}`, { signal: controller.signal })
      .then((r) => r.json())
      .then(setResults)
      .catch((e) => {
        if (e.name !== "AbortError") throw e;
      });

    return () => controller.abort();
  }, [query]);
}
```

---

## Immutability

### `Readonly` and `ReadonlyArray`

```typescript
// ❌ Mutable arguments can be modified accidentally
function processUsers(users: User[]) {
  users.sort((a, b) => a.name.localeCompare(b.name)); // Mutates original array!
  return users;
}

// ✅ Use readonly to prevent mutation
function processUsers(users: readonly User[]): User[] {
  return [...users].sort((a, b) => a.name.localeCompare(b.name));
}

// ✅ Deep readonly
type DeepReadonly<T> = {
  readonly [K in keyof T]: T[K] extends object ? DeepReadonly<T[K]> : T[K];
};
```

### Immutable function arguments

```typescript
// ✅ Use as const and readonly to protect data
function createConfig<T extends readonly string[]>(routes: T) {
  return routes;
}

const routes = createConfig(["home", "about", "contact"] as const);
// Type is readonly ['home', 'about', 'contact']
```

---

## ESLint Rules

### Recommended `@typescript-eslint` rules

```javascript
// eslint.config.js (flat config, typescript-eslint v8)
import eslint from "@eslint/js";
import tseslint from "typescript-eslint";

export default tseslint.config(
  eslint.configs.recommended,
  // Rule set requiring type info (equivalent to old recommended-requiring-type-checking)
  tseslint.configs.recommendedTypeChecked,
  tseslint.configs.strictTypeChecked,
  {
    languageOptions: {
      parserOptions: {
        // Let type-aware rules auto-discover the matching tsconfig
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      // ✅ Type safety
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/no-unsafe-assignment": "error",
      "@typescript-eslint/no-unsafe-member-access": "error",
      "@typescript-eslint/no-unsafe-call": "error",
      "@typescript-eslint/no-unsafe-return": "error",

      // ✅ Best practices
      "@typescript-eslint/explicit-function-return-type": "warn",
      "@typescript-eslint/no-floating-promises": "error",
      "@typescript-eslint/await-thenable": "error",
      "@typescript-eslint/no-misused-promises": "error",

      // ✅ Code style
      "@typescript-eslint/consistent-type-imports": "error",
      "@typescript-eslint/prefer-nullish-coalescing": "error",
      "@typescript-eslint/prefer-optional-chain": "error",
    },
  },
);
```

### Common ESLint issue fixes

```typescript
// ❌ no-floating-promises: Promise must be handled
async function save() { ... }
save();  // Error: unhandled Promise

// ✅ Handle it explicitly
await save();
// or
save().catch(console.error);
// or intentionally ignore
void save();

// ❌ no-misused-promises: avoid using Promise in non-async callback positions
const items = [1, 2, 3];
items.forEach(async (item) => {  // Error!
  await processItem(item);
});

// ✅ Use for...of
for (const item of items) {
  await processItem(item);
}
// or Promise.all
await Promise.all(items.map(processItem));
```

---

---

## Testing

This project (`ui/`) uses **Karma + Jasmine** via the Angular CLI builder `@angular/build:karma` (see the `test` target in `angular.json` and `karma.conf.js`). There is no Jest or Vitest in `ui/package.json` - do not introduce them in code reviews. (In the wider Angular ecosystem the state of the art is moving from Karma towards Vitest via `@angular/build:unit-test` (Angular 20+); migrating the test framework here would be a separate dedicated change, not part of a review.)

- Test files are `*.spec.ts` placed next to the sources under `ui/src/app/`.
- Run tests from `ui/`: `npm test -- --watch=false --browsers=ChromeHeadlessCI`.
- Follow `.github/skills/oe-ui-test/SKILL.md` for the detailed Angular/Ionic test patterns: `TestingUtils.sharedSetup()`, `DummyConfig` for `EdgeConfig`/`Edge` fixtures, and `OeFormlyViewTester`/`OeChartTester` for formly/chart views.

### Mocking with Jasmine spies

```typescript
// ✅ Spy on a service: methods as spies, signal/computed properties as plain values
const serviceSpy = jasmine.createSpyObj("MyService", ["doSomething"], { someSignal: signal(false) });
serviceSpy.doSomething.and.returnValue(42);

// ✅ Standalone spy for a callback
const onSave = jasmine.createSpy("onSave").and.resolveTo(undefined);
```

Prefer TestBed provider overrides (`{ provide: MyService, useValue: serviceSpy }`) - Jasmine has no `vi.mock`/`jest.mock` module mocking.

### Assertions (no snapshots)

Jasmine has no snapshot matchers (`toMatchSnapshot`/`toMatchInlineSnapshot` do not exist). Assert concrete expected values instead:

```typescript
it("renders the translated key", () => {
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain("MY.TRANSLATION_KEY");
});
```

### Type-level tests

There is no `expect-type`/`tsd` runner in this project. Validate types at compile time using `satisfies` and `// @ts-expect-error`; the TypeScript/lint checks in CI enforce them.

---

## Module Resolution

### ESM vs CJS differences and pitfalls

```typescript
// ❌ CJS style is not available in ESM
// package.json: "type": "module"
const fs = require("fs"); // Error: require is not defined
module.exports = { foo: "bar" }; // Error: module is not defined

// ✅ Correct ESM syntax
import fs from "node:fs";
export const foo = "bar";

// ✅ Get __dirname in ESM
import { fileURLToPath } from "node:url";
import { dirname } from "node:path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// ❌ Dynamic require in ESM
const moduleName = "lodash";
const _ = require(moduleName); // Error!

// ✅ Dynamic import in ESM
const _ = await import(moduleName);
```

### `tsconfig` paths and path aliases

```json
// tsconfig.json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"],
      "@components/*": ["./src/components/*"],
      "@utils/*": ["./src/utils/*"]
    }
  }
}
```

```typescript
// ✅ Before aliases
import { Button } from "../../components/ui/Button";
import { formatDate } from "../../../utils/date";

// ✅ After aliases—clearer and less fragile on file moves
import { Button } from "@components/ui/Button";
import { formatDate } from "@utils/date";
```

```typescript
// ⚠️ tsconfig paths only affect TS compilation, not runtime
// Pair with bundler aliasing (Vite/webpack) or tsx alias resolution

// vite.config.ts
import { resolve } from "node:path";

export default defineConfig({
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
    },
  },
});

// ⚠️ When publishing npm packages, tsconfig paths are not resolved automatically
// Use tsc-alias or tsconfig-paths
```

### `package.json` exports field

```json
// package.json
{
  "name": "my-library",
  "exports": {
    ".": {
      "import": "./dist/index.mjs",
      "require": "./dist/index.cjs",
      "types": "./dist/index.d.ts"
    },
    "./utils": {
      "import": "./dist/utils.mjs",
      "require": "./dist/utils.cjs",
      "types": "./dist/utils.d.ts"
    },
    "./*": "./dist/*"
  }
}
```

```typescript
// ✅ Consumer usage
import { foo } from "my-library"; // Resolves to "." condition
import { bar } from "my-library/utils"; // Resolves to "./utils" condition

// ❌ Paths not mapped in exports are inaccessible
import { secret } from "my-library/internal"; // Error!
```

### Dynamic `import()` and code splitting

```typescript
// ✅ Conditional module loading
async function loadChartLibrary() {
  if (typeof window === 'undefined') return null; // Skip during SSR
  const { Chart } = await import('chart.js');
  return Chart;
}

// ✅ React lazy-loaded component
const AdminPanel = lazy(() => import('./AdminPanel'));
// Use with Suspense
<Suspense fallback={<Loading />}>
  <AdminPanel />
</Suspense>

// ✅ With error handling
const AdminPanel = lazy(() =>
  import('./AdminPanel').catch(() => ({
    default: () => <ErrorFallback />,
  }))
);
```

---

## TS 4.9+ / 5.x New Features

> Note: this repository uses TypeScript 6.x (see `ui/package.json`). The features below are the baseline; usage of newer TypeScript features is allowed and encouraged where it improves the code.

### `satisfies` keyword (TS 4.9+)

```typescript
// ❌ Without satisfies: type is too broad
const palette = {
  red: "#ff0000",
  green: "#00ff00",
  blue: "#0000ff",
};
// palette.red type is string, losing the precise '#ff0000' value

// ✅ satisfies preserves literal types while validating structure
const palette = {
  red: "#ff0000",
  green: "#00ff00",
  blue: "#0000ff",
} satisfies Record<string, `#${string}`>;

// palette.red type is '#ff0000' (not string)
// New properties are still format-validated
```

```typescript
// ✅ satisfies to validate object shape against interface
interface UserConfig {
  theme: "light" | "dark";
  locale: string;
}

const config = {
  theme: "dark",
  locale: "en-US",
} satisfies UserConfig;
// config.theme type is 'dark' (not 'light' | 'dark')
// All fields pass satisfies type checking
```

### `const` type parameters (TS 5.0+)

```typescript
// ❌ Previously: needed as const assertion
function getRoutes<T extends readonly string[]>(routes: T) {
  return routes;
}
const routes = getRoutes(["home", "about"] as const);

// ✅ TS 5.0+: const type parameter
function getRoutes<const T extends readonly string[]>(routes: T) {
  return routes;
}
const routes = getRoutes(["home", "about"]);
// routes type is readonly ['home', 'about']
```

```typescript
// ✅ Real-world use case: type-safe configuration object
declare function createConfig<const T extends Record<string, unknown>>(
  config: T,
): T;

const config = createConfig({
  api: { url: "https://api.example.com", version: 2 },
  features: { newDashboard: true },
});
// config.api.url type is 'https://api.example.com' (literal)
```

### Decorators (Stage 3 Decorators, TS 5.0+)

```typescript
// ✅ Stage 3 decorators (TS 5.0+, no longer requires experimentalDecorators)
function logged<This, Args extends unknown[], Return>(
  target: (this: This, ...args: Args) => Return,
  context: ClassMethodDecoratorContext,
) {
  return function (this: This, ...args: Args): Return {
    console.log(`Calling ${String(context.name)} with`, args);
    return target.apply(this, args);
  };
}

class Calculator {
  @logged
  add(a: number, b: number): number {
    return a + b;
  }
}

// Output: Calling add with [1, 2]
new Calculator().add(1, 2);
```

```typescript
// ⚠️ Stage 3 decorators differ from legacy experimentalDecorators
// Legacy: requires "experimentalDecorators": true in tsconfig
// New (TS 5.0+): supported by default, no extra config needed

// ❌ Legacy decorator signature (still supported but marked as legacy)
function deprecated<T extends { new (...args: any[]): {} }>(constructor: T) {
  return class extends constructor {
    /* ... */
  };
}

// ✅ New decorators use typed context by target kind
function sealed<T extends { new (...args: any[]): {} }>(
  target: T,
  context: ClassDecoratorContext,
) {
  // context.kind === 'class'
}
```

### `using` declarations (explicit resource management, TS 5.2+)

```typescript
// ✅ Implement Symbol.dispose for automatic cleanup
class TempFile implements Disposable {
  private path: string;

  constructor() {
    this.path = `/tmp/file-${Date.now()}`;
  }

  write(data: string) {
    /* ... */
  }

  [Symbol.dispose]() {
    // Automatic cleanup no matter how the function exits (normal/error)
    fs.unlinkSync(this.path);
    console.log(`Cleaned up: ${this.path}`);
  }
}

function processFile() {
  using file = new TempFile(); // using declaration
  file.write("data");
  // file[Symbol.dispose]() is called automatically at end of scope
}
```

```typescript
// ✅ AsyncDisposable for async resources (TS 5.2+)
class DatabaseConnection implements AsyncDisposable {
  private db: sqlite3.Database;

  async connect() {
    this.db = new sqlite3.Database(":memory:");
  }

  async [Symbol.asyncDispose]() {
    await this.db.close();
  }
}

async function query() {
  await using conn = new DatabaseConnection(); // await using
  await conn.connect();
  // await conn[Symbol.asyncDispose]() is called automatically at end of scope
}
```

### Enum improvements (TS 5.0+)

```typescript
// ✅ All enums are now union enums (TS 5.0+)
enum Color {
  Red = "RED",
  Green = "GREEN",
}

// Previously: enum behavior as a type was inconsistent
// Now: Color behaves more consistently like a string literal union
const color: Color = Color.Red; // Better inference for Color type
```

## Review Checklist

### Type system

- [ ] No use of `any` (use `unknown` + type guards instead)
- [ ] No explicit `undefined` declarations (use optional `?` instead of `| undefined`; no `= undefined` initializers; `void` instead of `: undefined` return types)
- [ ] Interfaces and type definitions are complete and meaningfully named
- [ ] Generics are used to improve code reuse
- [ ] Union types have proper narrowing
- [ ] Utility types (Partial, Pick, Omit, etc.) are used effectively

### Generics

- [ ] Generic constraints are appropriate (`extends`)
- [ ] Generic parameters have sensible defaults
- [ ] Avoid over-generalization (KISS principle)

### Strict mode

- [ ] `tsconfig.json` enables `strict: true`
- [ ] `noUncheckedIndexedAccess` is enabled
- [ ] No use of `@ts-ignore` (use `@ts-expect-error` instead)

### Async code

- [ ] Async functions have error handling
- [ ] Promise rejections are handled correctly
- [ ] No floating promises (unhandled Promises)
- [ ] Concurrent requests use `Promise.all` or `Promise.allSettled`
- [ ] Race conditions are handled with `AbortController`

### Immutability

- [ ] Function parameters are not modified directly
- [ ] Spread syntax is used to create new objects/arrays
- [ ] Consider using `readonly` modifiers

### ESLint

- [ ] Use `@typescript-eslint/recommended`
- [ ] No ESLint warnings or errors
- [ ] Use `consistent-type-imports`

### UI (Angular/Ionic)

- [ ] Every changed file under `ui/` complies with `ui/AGENTS.md` (read it first; it prescribes the format check and member-scope rules)
