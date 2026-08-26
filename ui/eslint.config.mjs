import angularTemplate from "@angular-eslint/eslint-plugin-template";
import angularTemplateParser from "@angular-eslint/template-parser";
import tseslint from "typescript-eslint";
import stylistic from "@stylistic/eslint-plugin";
import checkFile from "eslint-plugin-check-file";
import importPlugin from "eslint-plugin-import-x";
import unusedImports from "eslint-plugin-unused-imports";
import globals from "globals";
import angular from "angular-eslint";
import { defineConfig } from "eslint/config";
import { createTypeScriptImportResolver } from "eslint-import-resolver-typescript";

export default defineConfig([
    {
        ignores: ["projects/**/*"],
    },
    ...tseslint.configs.recommended,
    ...angular.configs.tsRecommended,
    {
        ...importPlugin.flatConfigs.recommended,
        files: ["**/*.ts"],
    },
    {
        files: ["**/*.ts"],
        plugins: {
            "unused-imports": unusedImports,
            "@stylistic": stylistic,
            "check-file": checkFile,
        },

        languageOptions: {
            globals: {
                ...globals.browser,
                ...globals.node,
                ...globals.jest,
            },
            ecmaVersion: 5,
            sourceType: "commonjs",
            parserOptions: {
                project: ["tsconfig.json"],
                createDefaultProgram: true,
            },
        },
        rules: {
            "check-file/filename-naming-convention": [
                "off",
                {
                    "**/*.{ts}": "KEBAB_CASE",
                },
            ],
            curly: "error",
            "unused-imports/no-unused-imports": "error",
            "import-x/order": [
                "error",
                {
                    groups: ["builtin", "external", "internal", "parent", "sibling", "index"],
                    alphabetize: {
                        order: "asc",
                        caseInsensitive: true,
                    },
                },
            ],
            "@typescript-eslint/explicit-member-accessibility": [
                "error",
                {
                    accessibility: "explicit",
                    overrides: {
                        accessors: "off",
                        constructors: "off",
                        methods: "off",
                        properties: "explicit",
                        parameterProperties: "off",
                    },
                },
            ],
            "@angular-eslint/use-lifecycle-interface": ["error"],
            "@angular-eslint/prefer-standalone": "off",
            "@angular-eslint/directive-selector": [
                "error",
                {
                    type: "attribute",
                    prefix: ["app", "oe", "ngVar", "ngDomChange"],
                    style: "camelCase",
                },
            ],
            "@stylistic/semi": "error",
            "@stylistic/quote-props": ["warn", "as-needed"],
            "@stylistic/eol-last": "error",
            "@stylistic/no-trailing-spaces": "error",
            "no-unused-vars": "off",
            "@typescript-eslint/no-unused-vars": [
                "error",
                {
                    args: "none",
                    ignoreRestSiblings: true,
                    varsIgnorePattern: "^_",
                },
            ],

            "@typescript-eslint/no-explicit-any": 0,
            "@typescript-eslint/no-namespace": 0,
            "@typescript-eslint/no-restricted-types": 0,
            "@typescript-eslint/member-ordering": "error",
            "@typescript-eslint/no-unused-expressions": "off",
            "@typescript-eslint/no-empty-object-type": "off",
            "@stylistic/no-multiple-empty-lines": ["error", { max: 2, maxEOF: 1, maxBOF: 0 }],
            "@stylistic/quotes": [
                "error",
                "double",
                {
                    avoidEscape: true,
                },
            ],
            "no-restricted-syntax": [
                "error",
                {
                    selector: "CallExpression[callee.name='fdescribe']",
                    message: "Using 'fdescribe' is not allowed.",
                },
                {
                    selector: "CallExpression[callee.name='xdescribe']",
                    message: "Using 'xdescribe' is not allowed.",
                },
            ],
            // TODO reapply this rule
            // "@angular-eslint/template/accessibility-interactive-supports-focus": "error"
            "@angular-eslint/prefer-inject": "off",

            // Deactivated for angular migration pu
            "@angular-eslint/prefer-on-push-component-change-detection": "off",
            "no-redeclare": "off",
            "@typescript-eslint/no-redeclare": "off",
            "no-undef": "off",
        },
        settings: {
            "import-x/resolver-next": [
                createTypeScriptImportResolver({ project: "./tsconfig.json" }),
            ]
        },
    },
    {
        files: ["*.component.ts", "*.service.ts", "*.module.ts"],
        plugins: {
            "check-file": checkFile,
        },
        rules: {
            "check-file/filename-naming-convention": "off",
        },
    },
    {
        files: ["**/*.html"],
        languageOptions: {
            parser: angularTemplateParser,
        },
        plugins: {
            "@angular-eslint/template": angularTemplate,
        },
        rules: {
            "@angular-eslint/template/no-positive-tabindex": "error",
            "@angular-eslint/template/no-autofocus": "error",
            "@angular-eslint/template/mouse-events-have-key-events": "error",
            "@angular-eslint/template/click-events-have-key-events": "error",
        },
    },
]);
