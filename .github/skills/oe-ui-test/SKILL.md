---
name: oe-ui-test
description: Use when writing or fixing Angular/Ionic UI tests in the FEMS/OpenEMS UI (Karma/Jasmine), including formly view tests, chart tests, and component unit tests.
---

# FEMS UI Test Skill

Use this skill when writing or fixing Angular/Ionic tests under `ui/src/app/`.

## Framework

- Use Karma + Jasmine.
- Run tests from `ui/`.
- Use `// @ts-strict-ignore` only when unavoidable and consistent with nearby specs.

## Core Utilities

Testing helpers live in `ui/src/app/shared/components/shared/testing/`:

- `utils.spec.ts`: `TestContext`, `TestingUtils`
- `tester.ts`: `OeFormlyViewTester`, `OeChartTester`
- `common.ts`: `OeTester.ChartOptions`
- `channels.spec.ts`: reusable channel fixtures

Use `DummyConfig` from `src/app/shared/components/edge/edgeconfig.spec.ts` for `EdgeConfig` and `Edge` test data. Do not manually construct `EdgeConfig` or `Edge`.

## Setup Patterns

```typescript
let testContext: TestContext;

beforeEach(async () => {
    testContext = await TestingUtils.sharedSetup();
});
```

For routed components with a `componentId` route parameter:

```typescript
let testContext: TestContext & { route: ActivatedRoute };

beforeEach(async () => {
    testContext = await TestingUtils.setupWithActivatedRoute("meter0");
});
```

For extra providers/imports, use `TestingUtils.mergeSetup(...)` and follow nearby specs.

## DummyConfig

```typescript
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";

const config = DummyConfig.from(
    DummyConfig.Component.SOCOMEC_CONSUMPTION_METER("meter0"),
);

const edge = DummyConfig.dummyEdge({ role: Role.OWNER });
```

Inspect `DummyConfig.Component` in `edgeconfig.spec.ts` for available component factories before adding new ones. For chart tests, convert dummy configs before use:

```typescript
const realConfig = DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config);
```

## Formly View Tests

Use `OeFormlyViewTester` for components exposing `generateView()`:

```typescript
expect(OeFormlyViewTester.apply(
    MyComponent.generateView(testContext.translate, component, edge),
    { "component0/ActivePower": 3000 },
)).toEqual({
    title: "My Component",
    lines: [
        CHANNEL_LINE("Power", "3.000 W"),
    ],
});
```

Common line helpers include `CHANNEL_LINE`, `VALUE_FROM_CHANNELS_LINE`, `LINE_INFO`, `LINE_HORIZONTAL`, `PHASE_ADMIN`, and `PHASE_GUEST`. Inspect `edgeconfig.spec.ts` for the full helper list.

## Chart Tests

Use `OeChartTester` for history chart components exposing `getChartData()`. Always wrap both sides of chart comparisons in `TestingUtils.removeFunctions()`.

```typescript
expect(TestingUtils.removeFunctions(
    OeChartTester.apply(
        MyChartComponent.getChartData(
            DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config),
            testContext.route,
            testContext.translate,
        ),
        "line",
        History.DAY,
        testContext,
        config,
    ),
)).toEqual(TestingUtils.removeFunctions(expectedView));
```

Use `DATA` and `LABELS` from chart test constants where nearby specs do. Use `OeTester.ChartOptions` helpers for chart options; inspect `common.ts` for the full list.

## Conventions

- Set `testContext.translate.use("de")` or `"en"` when asserting translated strings.
- Prefer separate `it()` blocks per scenario.
- Keep channel fixtures such as `History.DAY` and `History.MONTH` in a nearby `channels.spec.ts`.
- Do not declare components in `TestBed` unless the component under test requires it.

## Validation

Run from `ui/`:

```powershell
npm test
npm run lint
```
