import fs from "fs";
import path from "path";

/**
 * Local ESLint plugin ensuring every `data-tutorial-id` value of the guided tour is assigned in exactly one place.
 *
 * - Templates must bind `data-tutorial-id` via `TutorialTargetId` instead of a hardcoded string.
 * - Each `TutorialTargetId` entry may only be assigned once, either in a template
 *   (`[attr.data-tutorial-id]="tutorialTargetId.X"`) or as navigation `tourId` / card `tutorialTargetId` (`tourId:
 *   TutorialTargetId.X`).
 *
 * Assignments in other files are indexed once per ESLint process. The linted file itself always uses its current text,
 * so editors report duplicates involving the open file immediately.
 */

const UI_DIR = path.join(import.meta.dirname, "..");
const SRC_DIRS = [path.join(UI_DIR, "src", "app"), path.join(UI_DIR, "src", "themes")];

const HARDCODED_ATTRIBUTE_PATTERN = /(?<![\w.[-])data-tutorial-id\s*=\s*"/g;
const TEMPLATE_ASSIGNMENT_PATTERN = /\[attr\.data-tutorial-id\]\s*=\s*"tutorialTargetId\.([A-Z0-9_]+)"/g;
const NAVIGATION_ASSIGNMENT_PATTERN = /(?:tourId|tutorialTargetId)\s*:\s*TutorialTargetId\.([A-Z0-9_]+)/g;

/** @type {Map<string, { id: string; index: number; length: number }[]> | null} */
let assignmentIndex = null;

function isRelevantFile(file) {
    return file.endsWith(".html") || (file.endsWith(".ts") && !file.endsWith(".spec.ts"));
}

function findAssignments(file, text) {
    const pattern = file.endsWith(".html") ? TEMPLATE_ASSIGNMENT_PATTERN : NAVIGATION_ASSIGNMENT_PATTERN;
    return [...text.matchAll(pattern)].map((match) => ({ id: match[1], index: match.index, length: match[0].length }));
}

function getAssignmentIndex() {
    if (assignmentIndex != null) {
        return assignmentIndex;
    }

    assignmentIndex = new Map();
    for (const entry of SRC_DIRS.flatMap((dir) => fs.readdirSync(dir, { recursive: true, withFileTypes: true }))) {
        const file = path.join(entry.parentPath, entry.name);
        if (!entry.isFile() || !isRelevantFile(file)) {
            continue;
        }

        const assignments = findAssignments(file, fs.readFileSync(file, "utf8"));
        if (assignments.length > 0) {
            assignmentIndex.set(path.resolve(file), assignments);
        }
    }
    return assignmentIndex;
}

function isInSourceDir(file) {
    return SRC_DIRS.some((dir) => file.startsWith(dir + path.sep));
}

function getLine(text, index) {
    return text.slice(0, index).split("\n").length;
}

/** @type {import("eslint").Rule.RuleModule} */
const uniqueTourTarget = {
    meta: {
        type: "problem",
        docs: { description: "Ensures every tour target is assigned via TutorialTargetId and only once." },
        schema: [],
        messages: {
            hardcoded: 'Hardcoded data-tutorial-id, use [attr.data-tutorial-id]="tutorialTargetId.<ID>" instead.',
            duplicate: "TutorialTargetId.{{id}} is also assigned at {{locations}}.",
        },
    },
    create(context) {
        const file = path.resolve(context.filename);

        return {
            Program() {
                if (!isRelevantFile(file) || !isInSourceDir(file)) {
                    return;
                }

                const sourceCode = context.sourceCode;
                const text = sourceCode.text;
                const report = (index, length, messageId, data) =>
                    context.report({
                        loc: {
                            start: sourceCode.getLocFromIndex(index),
                            end: sourceCode.getLocFromIndex(index + length),
                        },
                        messageId,
                        data,
                    });

                if (file.endsWith(".html")) {
                    for (const match of text.matchAll(HARDCODED_ATTRIBUTE_PATTERN)) {
                        report(match.index, match[0].length, "hardcoded");
                    }
                }

                const index = getAssignmentIndex();
                const ownAssignments = findAssignments(file, text);
                index.set(file, ownAssignments);

                for (const assignment of ownAssignments) {
                    const otherLocations = [...index].flatMap(([otherFile, assignments]) =>
                        assignments
                            .filter(
                                (other) =>
                                    other.id === assignment.id &&
                                    !(otherFile === file && other.index === assignment.index),
                            )
                            .map((other) => {
                                const otherText = otherFile === file ? text : fs.readFileSync(otherFile, "utf8");
                                return `${path.relative(UI_DIR, otherFile)}:${getLine(otherText, other.index)}`;
                            }),
                    );

                    if (otherLocations.length > 0) {
                        report(assignment.index, assignment.length, "duplicate", {
                            id: assignment.id,
                            locations: otherLocations.join(", "),
                        });
                    }
                }
            },
        };
    },
};

export default {
    rules: {
        "unique-tour-target": uniqueTourTarget,
    },
};
