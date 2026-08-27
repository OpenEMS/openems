import { ArrayUtils } from "../array/array.utils";

/** Helper functions for interacting with objects. */
export class ObjectUtils {
    /**
     * Excludes specified properties from an object by creating a shallow copy.
     *
     * @param obj The object
     * @param keys The keys to exclude from given object
     * @returns The given object, except properties from given keys
     */
    public static excludeProperties<T extends Record<string, any>, K extends keyof T>(
        obj: T | null,
        keys: K[],
    ): Omit<T, K> | null {
        if (obj == null) {
            return null;
        }

        const result = { ...obj };
        keys.forEach((key) => delete result[key]);
        return result;
    }

    /**
     * Picks specified properties from an object by creating a shallow copy.
     *
     * @param obj The object
     * @param keys The keys to pick from given object
     * @returns The given object, only including properties from given keys
     */
    public static pickProperties<T extends Record<string, any>, K extends keyof T>(
        obj: T | null,
        keys: K[],
    ): Pick<T, K> | null {
        if (obj == null) {
            return null;
        }

        return keys.reduce(
            (res, key) => {
                res[key] = obj[key];
                return res;
            },
            {} as Pick<T, K>,
        );
    }

    /**
     * Checks if given object has given keys.
     *
     * @param obj The object
     * @param keys The keys to look for in given object
     * @returns True, if all keys are found in the object
     */
    public static hasKeys<T extends Record<string, any>>(obj: T, keys: string[]): boolean {
        return ArrayUtils.containsAll({ strings: Object.keys(obj), arr: keys });
    }

    /**
     * Gets the value of a object by key safely.
     *
     * @param obj The object
     * @param key The key to look for in given object
     * @returns The value of the object with key, if not existing null
     */
    public static getValueByKeySafely<T extends Record<string, any>, K extends keyof T>(obj: T, key: K): T[K] | null {
        if (obj === null || obj === undefined) {
            return null;
        }
        return key in obj ? obj[key] : null;
    }

    /**
     * Checks if the given object is not null and not an empty object.
     *
     * @param obj The object
     * @returns True, if object is not null or empty
     */
    public static isObjectNullOrEmpty(obj: Record<string, any> | null | undefined): boolean {
        return obj == null || Object.keys(obj).length === 0;
    }

    /**
     * Flattens a deep nested object into a one dimensional object with dot notation keys and string values.
     *
     * @param obj The object to flatten
     * @param parentKey The parent key to use for nested objects
     * @param result The result object to populate
     * @returns The flattened object
     */
    public static flattenObjectWithValues<T extends object>(
        obj: T,
        parentKey: string | null = null,
        result: Record<string, string> = {},
    ) {
        for (const key in obj) {
            if (!(key in obj)) {
                continue;
            }

            const newKey = parentKey !== null ? `${parentKey}.${key}` : key;
            const value = obj[key];

            if (typeof value === "object" && value !== null && !Array.isArray(value)) {
                this.flattenObjectWithValues(value, newKey, result);
            } else {
                result[newKey] = String(value);
            }
        }
        return result;
    }

    /**
     * Parses a stringified object to object.
     *
     * @param obj The stringified object to parse
     * @returns The parsed object or null
     */
    public static parseFromString<T extends object>(obj: string | null): T | null {
        if (obj == null) {
            return null;
        }

        try {
            return JSON.parse(obj);
        } catch {
            return null;
        }
    }

    /**
     * Removes properties with a `null` or `undefined` value from a shallow object. Optionally accepts additional
     * validation checks via a predicate callback.
     *
     * Sending an explicit `null` (e.g. from a formly field reset via `resetOnHide`) instead of omitting the key can
     * cause the backend to reject the value instead of falling back to its default, so this should be applied before
     * sending request properties (e.g. app installation/estimation properties) to the backend.
     *
     * @param obj The object to strip `null`/`undefined` values from
     * @param additionalCheck Callback returning `true` to keep the value, `false` to omit it
     * @returns A shallow copy of the object without `null`/`undefined` or failing values
     */
    public static omitNullOrUndefinedValues<T extends object>(
        obj: T,
        additionalCheck: (value: unknown) => boolean = () => true,
    ): Partial<T> {
        const result: Partial<T> = {};
        for (const key of Object.keys(obj) as (keyof T)[]) {
            const value = obj[key];
            if (value !== null && value !== undefined && additionalCheck(value)) {
                result[key] = value;
            }
        }
        return result;
    }
}
