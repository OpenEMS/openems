import unusedImports from "eslint-plugin-unused-imports";
import stylistic from "@stylistic/eslint-plugin";
import globals from "globals";
import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
    baseDirectory: __dirname,
    recommendedConfig: js.configs.recommended,
    allConfig: js.configs.all
});

export default [{
    "ignores": ["projects/**/*"],
}, ...compat.extends(
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:@angular-eslint/recommended",
    "plugin:@angular-eslint/template/process-inline-templates",
).map(config => ({
    ...config,
    "files": ["**/*.ts"],
})), {
    "files": ["**/*.ts"],

    "plugins": {
        "unused-imports": unusedImports,
        "@stylistic": stylistic,
    },

    "languageOptions": {
        "globals": {
            ...globals.browser,
            ...globals.node,
            ...globals.jest,
        },

        "ecmaVersion": 5,
        "sourceType": "commonjs",

        "parserOptions": {
            "project": ["tsconfig.json"],
            "createDefaultProgram": true,
        },
    },

    "rules": {
        "unused-imports/no-unused-imports": "error",
        "@stylistic/semi": [
          "error",
          "always"
        ],
        "@stylistic/quote-props": [
          "warn",
          "consistent"
        ],
        "@typescript-eslint/explicit-member-accessibility": [
          "error",
          {
            "accessibility": "explicit",
            "overrides": {
              "accessors": "off",
              "constructors": "off",
              "methods": "off",
              "properties": "explicit",
              "parameterProperties": "off"
            }
          }
        ],
        "@angular-eslint/use-lifecycle-interface": [
          "error"
        ],
        "@angular-eslint/directive-selector": [
          "error",
          {
            "type": "attribute",
            "prefix": [
              "app",
              "oe",
              "ngVar"
            ],
            "style": "camelCase"
          }
        ],
        "curly": "error",
        "@stylistic/comma-dangle": [
          "error",
          "always-multiline"
        ],
        "@stylistic/eol-last": [
          "error",
          "always"
        ],
        "@stylistic/no-trailing-spaces": "error",
        "@typescript-eslint/no-unused-vars": [
          "error",
          {
            "args": "none"
          }
        ],
        "@typescript-eslint/no-explicit-any": 0,
        "@typescript-eslint/no-namespace": 0,
        "@typescript-eslint/ban-types": [
          "error",
          {
            "extendDefaults": true,
            "types": {
              "{}": false,
              "Object": false,
              "Function": false
            }
          }
        ]
    },
}, ...compat.extends("plugin:@angular-eslint/template/recommended").map(config => ({
    ...config,
    "files": ["**/*.html"],
})), {
    "files": ["**/*.html"],
    "rules": {},
}];
