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
- Reuse existing shared components, services, pipes, and utilities before adding new abstractions.
- Keep theme-specific behavior and branding in the relevant theme directory instead of hardcoding it in generic application code.
- Update `src/app/shared/i18n/` whenever user-facing text changes.
- When changing backend or Edge connectivity behavior, inspect the relevant environment files under `src/themes/*/environments/` and the matching configuration in `angular.json`.

## Tests

Use `.github/skills/oe-ui-test/SKILL.md` for detailed Angular/Ionic test-writing patterns.

## Validation

- Run commands from `ui/`.
- Prefer non-watch tests: `npm test -- --watch=false --browsers=ChromeHeadlessCI`.
- Run lint with `npm run lint`.
