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

function findRedundantKeys(files) {
    let duplicatedKeys = [];
    let groupLangFiles = files.reduce((arr, el) => {
        const lang = path.basename(el).toString();

        if (arr[lang] == null) {
            arr[lang] = [];
        }
        arr[lang].push(el);
        return arr;
    }, {})

    for (let [lang, files] of Object.entries(groupLangFiles)) {
        let allGroupedLang = {};
        for (let file of files) {
            if (allGroupedLang[lang] == null) {
                allGroupedLang[lang] = [];
            }
            const content = fs.readFileSync(file, 'utf8');
            const data = JSON.parse(content);
            allGroupedLang[lang].push(...getStringPaths(data));
        }
        const duplicates = findAllDuplicateOccurrences(allGroupedLang[lang]);
        duplicatedKeys.push({ lang: lang, duplicates: duplicates })
    }

    return duplicatedKeys;
}

function findAllDuplicateOccurrences(arr) {
    return arr.filter((item, index) => arr.indexOf(item) !== index);
}

function getStringPaths(obj, parentPath = "", result = []) {
    if (typeof obj !== "object" || obj === null) {
        return result;
    }

    for (const [key, value] of Object.entries(obj)) {
        const currentPath = parentPath ? `${parentPath}.${key}` : key;

        if (typeof value === "string") {
            // Check if string contains an object-like structure
            const trimmed = value.trim();
            const looksLikeObject =
                (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"));

            if (!looksLikeObject) {
                result.push(currentPath);
            }
        } else if (typeof value === "object" && value !== null) {
            getStringPaths(value, currentPath, result);
        }
    }

    return result;
}

/**
 * Recursively check all keys in a JSON object
 */
function checkKeys(obj, currentPath = '', errors = []) {
    if (typeof obj !== 'object' || obj === null) {
        return errors;
    }

    for (const [key, value] of Object.entries(obj)) {
        const fullPath = currentPath ? `${currentPath}.${key}` : key;
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
        let errors = [];
        checkKeys(data, '', errors);
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
 * Recursively search for folders named "i18n", starting from a directory,
 * and excluding certain folders.
 *
 * @param  startDir - The directory to start searching from.
 * @param  excludeDirs - Folder names to exclude.
 * @returns List of found i18n folder paths.
 */
function findAllI18nFiles(excludeDirs = ["node_modules", "dist", "build", "android"]) {
    const foundDirectories = [];
    const startDirectory = path.resolve("./src"); // only search in 

    function search(dir) {
        const entries = fs.readdirSync(dir, { withFileTypes: true });

        for (const entry of entries) {
            const fullPath = path.join(dir, entry.name);

            if (entry.isDirectory()) {
                if (excludeDirs.includes(entry.name)) continue;

                if (entry.name === "i18n") {
                    foundDirectories.push(fullPath);
                } else {
                    search(fullPath); // recurse deeper
                }
            }
        }
    }

    search(startDirectory);
    return foundDirectories
        .map(el => fs.readdirSync(el, { withFileTypes: true }))
        .flat()
        .map(el => path.join(el.parentPath, el.name))
        .filter(el => el.endsWith('.json'));
}

/**
 * Main function
 */
function main() {
    const srcDir = path.join(__dirname, '..', 'src');

    // Check if source directory exists
    if (!fs.existsSync(srcDir)) {
        console.error('❌ Source directory not found:', srcDir);
        process.exit(1);
    }

    // Get all translation files (global + module-specific)
    const files = findAllI18nFiles(["node_modules", "dist", "build", ".git"]).sort();

    if (files.length === 0) {
        console.error('❌ No translation files found in', srcDir);
        process.exit(1);
    }

    console.log('🔍 Linting translation keys for UPPER_SNAKE_CASE format...');
    console.log(`📁 Found ${files.length} translation file(s)\n`);

    // Lint all files
    const results = files.map(lintFile);
    const redundantKeys = findRedundantKeys(files);
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

    const duplicates = redundantKeys.flatMap(el => el.duplicates);
    const affectedLangs = redundantKeys.filter(el => el.duplicates.length > 0).map(el => el.lang);
    if (duplicates.length === 0) {
        console.log('  ✅ No redundant keys found');
    } else {
        console.log(`\n  ❌ Found ${duplicates.length} redundant keys in ${affectedLangs}: ${duplicates}`);
        totalErrors += redundantKeys.length;
    }

    if (totalErrors > 0) {

        results.forEach(result => {
            if (result.errors.length > 0) {
                console.log(`\n❌ Linting FAILED - Found ${totalErrors} invalid key(s):\n`);
                const relativePath = path.relative(srcDir, result.filePath);

                console.log(`${relativePath}:`);
                result.errors.forEach(error => {
                    console.log(`  ❌ ${error.path}: '${error.key}' is not UPPER_SNAKE_CASE`);
                });
                console.log('💡 All translation keys must be in UPPER_SNAKE_CASE format.');
                console.log('   Example: GENERAL.SUM_STATE, EDGE.INDEX.WIDGETS.EVCS\n');
            }
        });
        if (redundantKeys.length > 0) {
            console.log('\n💡 All translation keys must be unique.');
        }

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
