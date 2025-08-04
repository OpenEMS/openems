import unusedImports from "eslint-plugin-unused-imports";
import stylistic from "@stylistic/eslint-plugin";
import globals from "globals";
import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import { FlatCompat } from "@eslint/eslintrc";
import importPlugin from "eslint-plugin-import";
import checkFile from "eslint-plugin-check-file";

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
  "plugin:import/recommended"
).map(config => ({
  ...config,
  "files": ["**/*.ts"],
})), {
  "files": ["**/*.ts"],

  "plugins": {
    "unused-imports": unusedImports,
    "@stylistic": stylistic,
    "import": importPlugin,
    "check-file": checkFile,
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
    "check-file/filename-naming-convention": [
      "off",
      {
        "**/*.{ts}": "KEBAB_CASE"
      }
    ],
    "curly": "error",
    "unused-imports/no-unused-imports": "error",
    "import/order": [
      "error",
      {
        "groups": [
          "builtin",
          "external",
          "internal",
          "parent",
          "sibling",
          "index"
        ],
        "alphabetize": {
          "order": "asc",
          "caseInsensitive": true
        }
      }
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
    "@angular-eslint/prefer-standalone": "off",
    "@angular-eslint/directive-selector": [
      "error",
      {
        "type": "attribute",
        "prefix": [
          "app",
          "oe",
          "ngVar",
          "ngDomChange"
        ],
        "style": "camelCase"
      }
    ],
    "@stylistic/semi": "error",
    "@stylistic/quote-props": [
      "warn",
      "consistent"
    ],
    "@stylistic/comma-dangle": [
      "error",
      "always-multiline"
    ],
    "@stylistic/eol-last": "error",
    "@stylistic/no-trailing-spaces": "error",
    "@typescript-eslint/no-unused-vars": [
      "error",
      {
        "args": "none",
        "ignoreRestSiblings": true,
        "varsIgnorePattern": "^_"
      }
    ],
    "@typescript-eslint/no-explicit-any": 0,
    "@typescript-eslint/no-namespace": 0,
    "@typescript-eslint/no-restricted-types": 0,
    "@typescript-eslint/member-ordering": "error",
    "@typescript-eslint/no-unused-expressions": "off",
    "@typescript-eslint/no-empty-object-type": "off",
    "@stylistic/no-multiple-empty-lines": ["error", { "max": 2, "maxEOF": 1, "maxBOF": 0 }],
    "@stylistic/quotes": [
      "error",
      "double"
    ],
    "no-restricted-syntax": [
      "error",
      {
        "selector": "CallExpression[callee.name='fdescribe']",
        "message": "Using 'fdescribe' is not allowed."
      },
      {
        "selector": "CallExpression[callee.name='xdescribe']",
        "message": "Using 'xdescribe' is not allowed."
      }
    ]
  },
  "settings": {
    "import/resolver": {
      "typescript": {}
    }
  }
}, {
  "files": ["*.component.ts", "*.service.ts", "*.module.ts"],
  "plugins": {
    "check-file": checkFile,
  },
  "rules": {
    "check-file/filename-naming-convention": "off",
  },
}, ...compat.extends("plugin:@angular-eslint/template/recommended").map(config => ({
  ...config,
  "files": ["**/*.html"],
})), {
  "files": ["**/*.html"],
  "rules": {},
}];
