#!/usr/bin/env node
/**
 * Lint translation keys to ensure they follow UPPER_SNAKE_CASE convention.
 *
 * Usage: node scripts/lint-translation-keys.js
 * Exit code: 0 if all keys are valid, 1 if any invalid keys found
 */

const fs = require('fs');
const path = require('path');

// UPPER_SNAKE_CASE pattern
const UPPER_SNAKE_CASE_PATTERN = /^[A-Z0-9]+(_[A-Z0-9]+)*$/;

/**
 * Recursively check all keys in a JSON object
 */
function checkKeys(obj, currentPath = '', errors = []) {
    if (typeof obj !== 'object' || obj === null) {
        return errors;
    }

    for (const [key, value] of Object.entries(obj)) {
        const fullPath = currentPath ? `${currentPath}.${key}` : key;

        // Check if key matches UPPER_SNAKE_CASE
        if (!UPPER_SNAKE_CASE_PATTERN.test(key)) {
            errors.push({
                path: fullPath,
                key: key,
                message: `Key '${key}' is not UPPER_SNAKE_CASE`
            });
        }

        // Recursively check nested objects
        if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
            checkKeys(value, fullPath, errors);
        }
    }

    return errors;
}

/**
 * Lint a single translation file
 */
function lintFile(filePath) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        const data = JSON.parse(content);

        // Module translation files (translation.json) have language codes as top-level keys
        // Global translation files (assets/i18n/*.json) have direct translation keys
        const isModuleTranslation = path.basename(filePath) === 'translation.json';

        let errors = [];
        if (isModuleTranslation) {
            // For module translations, skip language code validation and check nested content
            const languageCodes = ['de', 'en', 'es', 'fr', 'nl', 'cz', 'cs', 'ja'];
            for (const [key, value] of Object.entries(data)) {
                if (languageCodes.includes(key.toLowerCase())) {
                    // Check the nested translation keys, not the language code itself
                    checkKeys(value, '', errors);
                } else {
                    // If it's not a language code, validate it normally
                    checkKeys(data, '', errors);
                    break;
                }
            }
        } else {
            // For global translations, check all keys normally
            errors = checkKeys(data);
        }

        return { filePath, errors };
    } catch (error) {
        return {
            filePath,
            errors: [{
                path: filePath,
                key: '',
                message: `Error reading/parsing file: ${error.message}`
            }]
        };
    }
}

/**
 * Recursively find all translation files
 */
function findTranslationFiles(dir, fileList = []) {
    const files = fs.readdirSync(dir);

    files.forEach(file => {
        const filePath = path.join(dir, file);
        const stat = fs.statSync(filePath);

        if (stat.isDirectory()) {
            // Skip node_modules, target, www, and other build directories
            if (!['node_modules', 'target', 'www', 'dist', '.angular'].includes(file)) {
                findTranslationFiles(filePath, fileList);
            }
        } else if (file.endsWith('.json') && (file === 'translation.json' || filePath.includes(path.join('assets', 'i18n')))) {
            fileList.push(filePath);
        }
    });

    return fileList;
}

/**
 * Main function
 */
function main() {
    const srcDir = path.join(__dirname, '..', 'src');
    const globalI18nDir = path.join(srcDir, 'assets', 'i18n');
    const appDir = path.join(srcDir, 'app');

    // Check if source directory exists
    if (!fs.existsSync(srcDir)) {
        console.error('❌ Source directory not found:', srcDir);
        process.exit(1);
    }

    // Get all translation files (global + module-specific)
    const files = findTranslationFiles(srcDir).sort();

    if (files.length === 0) {
        console.error('❌ No translation files found in', srcDir);
        process.exit(1);
    }

    console.log('🔍 Linting translation keys for UPPER_SNAKE_CASE format...');
    console.log(`📁 Found ${files.length} translation file(s)\n`);

    // Lint all files
    const results = files.map(lintFile);
    let totalErrors = 0;

    results.forEach(result => {
        const relativePath = path.relative(srcDir, result.filePath);
        console.log(`Checking ${relativePath}...`);

        if (result.errors.length === 0) {
            console.log('  ✅ All keys are valid UPPER_SNAKE_CASE');
        } else {
            console.log(`  ❌ Found ${result.errors.length} invalid key(s)`);
            totalErrors += result.errors.length;
        }
    });

    console.log('\n' + '='.repeat(60));

    if (totalErrors > 0) {
        console.log(`\n❌ Linting FAILED - Found ${totalErrors} invalid key(s):\n`);

        results.forEach(result => {
            if (result.errors.length > 0) {
                const relativePath = path.relative(srcDir, result.filePath);
                console.log(`${relativePath}:`);
                result.errors.forEach(error => {
                    console.log(`  ❌ ${error.path}: '${error.key}' is not UPPER_SNAKE_CASE`);
                });
                console.log('');
            }
        });

        console.log('💡 All translation keys must be in UPPER_SNAKE_CASE format.');
        console.log('   Example: GENERAL.SUM_STATE, EDGE.INDEX.WIDGETS.EVCS\n');
        process.exit(1);
    } else {
        console.log(`\n✅ Linting PASSED - All translation keys are UPPER_SNAKE_CASE`);
        console.log(`   Checked ${files.length} file(s)\n`);
        process.exit(0);
    }
}

// Run if called directly
if (require.main === module) {
    main();
}

module.exports = { checkKeys, lintFile };
