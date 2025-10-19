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
        const errors = checkKeys(data);
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
 * Main function
 */
function main() {
    const i18nDir = path.join(__dirname, '..', 'src', 'assets', 'i18n');

    // Check if directory exists
    if (!fs.existsSync(i18nDir)) {
        console.error('❌ Directory not found:', i18nDir);
        process.exit(1);
    }

    // Get all JSON files
    const files = fs.readdirSync(i18nDir)
        .filter(file => file.endsWith('.json'))
        .map(file => path.join(i18nDir, file))
        .sort();

    if (files.length === 0) {
        console.error('❌ No translation files found in', i18nDir);
        process.exit(1);
    }

    console.log('🔍 Linting translation keys for UPPER_SNAKE_CASE format...\n');

    // Lint all files
    const results = files.map(lintFile);
    let totalErrors = 0;

    results.forEach(result => {
        const fileName = path.basename(result.filePath);
        console.log(`Checking ${fileName}...`);

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
                const fileName = path.basename(result.filePath);
                console.log(`${fileName}:`);
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
