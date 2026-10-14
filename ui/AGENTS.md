# AGENTS.md

## UI Scope

This directory contains the Angular/Ionic frontend for OpenEMS/FEMS.

## Structure

- Application code lives under `src/app/`.
- Shared frontend code belongs under `src/app/shared/`, including shared components, services, pipes, utilities, JSON-RPC types, and common infrastructure.
- Theme-specific branding, assets, styles, icons, and environment files belong under `src/themes/`.
- `angular.json` is the source of truth for build/serve configurations and file replacements.

## Conventions

- Follow `.editorconfig` and `eslint.config.mjs`.
- Use Angular control-flow syntax (`@if`, `@for`, `@switch`) in templates; do not add new `*ngIf`, `*ngFor`, or `*ngSwitch` structural directives.
- Format every file you change with Prettier (`npx prettier --write <file>`); changed files must be Prettier-clean (`npx prettier --check <file>`), even if that reformats pre-existing lines in the file.
- New TypeScript and template code must be written so it compiles cleanly under Angular's `strictTemplates` template type checking and under the `typescript-strict-plugin` strict mode. `strictTemplates` is currently disabled globally in `tsconfig.json` for legacy files — do not rely on that; do not add or widen `// @ts-strict-ignore` scopes to make new code compile.
- Keep member scope as small as possible: default to `private`. Members referenced only from the component's own template stay `private` — Angular templates can access private members, so do not widen visibility for the template's sake. Enforce this for existing members in files you touch.
- Avoid redundant code; prefer shared utility functions over duplicated inline logic.
- Never use `delete` to remove object properties; rebuild the object instead (e.g. `Object.fromEntries(Object.entries(obj).filter(...))`).
- Reuse existing shared components, services, pipes, and utilities before adding new abstractions.
- Keep theme-specific behavior and branding in the relevant theme directory instead of hardcoding it in generic application code.
- Update the UI's translation files (`src/assets/i18n/` and every module-local `i18n/` directory) whenever user-facing text changes, and add every new translation key to all language files there (cz, de, en, es, fr, ja, nl) — missing keys silently fall back to English. Remove keys that are no longer used. Avoid OEM-specific texts (FENECON, FEMS, branding, GTC/AGB, law texts, internal links) in these files; prefer generic texts there — themes and OEM overlays are the place for them. Factual mentions (e.g. the OpenEMS project history) are allowed.
- When changing backend or Edge connectivity behavior, inspect the relevant environment files under `src/themes/*/environments/` and the matching configuration in `angular.json`.
- Document JSON-RPC Request/Response shapes with a fenced ` ```json ` code block in the class/namespace JSDoc (see `src/app/shared/jsonrpc/request/*.ts`), not `@typedef` or `<pre>`/HTML tags: `@typedef` is meant for real type aliases, not JSON examples, and `prettier-plugin-jsdoc` collapses `<p>`/`<pre>` onto the preceding line and destroys the example's formatting.

## Tests

Use `.github/skills/oe-ui-test/SKILL.md` for detailed Angular/Ionic test-writing patterns.

## Validation

- Run commands from `ui/`.
- Prefer non-watch tests: `npm test -- --watch=false --browsers=ChromeHeadlessCI`.
- Run lint with `npm run lint`.
