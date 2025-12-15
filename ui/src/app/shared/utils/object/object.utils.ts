import { ArrayUtils } from "../array/array.utils";

/**
 * Helper functions for interacting with objects.
 */
export class ObjectUtils {

    /** Excludes specified properties from an object by creating a shallow copy. */
    public static excludeProperties<T extends Record<string, any>, K extends keyof T>(obj: T, keys: K[]): Omit<T, K> {
        const result = { ...obj };
        keys.forEach(key => delete result[key]);
        return result;
    }

    public static pickProperties<T extends Record<string, any>, K extends keyof T>(obj: T, keys: K[]): Pick<T, K> {
        return keys.reduce((res, key) => {
            res[key] = obj[key];
            return res;
        }, {} as Pick<T, K>);
    }

    public static hasKeys<T extends Record<string, any>>(obj: T, keys: string[]): boolean {
        return ArrayUtils.containsAll({ strings: Object.keys(obj), arr: keys });
    }

    public static hasValues<T extends Record<string, any>>(obj: T, keys: string[]): boolean {
        return ArrayUtils.containsAll({ strings: Object.keys(obj), arr: keys });
    }

    public static getKeySafely<T extends Record<string, any>, K extends keyof T>(obj: T, key: K): T[K] | null {
        return key in obj ? obj[key] : null;
    }

    public static findObjectWithProperty<T extends Record<string, any>>(obj: T, keys: string[]): boolean {
        return ArrayUtils.containsAll({ strings: Object.keys(obj), arr: keys });
    }

    public static isObjectNullOrEmpty(obj: Record<string, any> | null | undefined): boolean {
        return obj == null || Object.keys(obj).length === 0;
    }
    /**
    * Flattens a deep nested object into a one dimensional object with dot notation keys and string values.
    *
    * @param obj the object to flatten
    * @param parentKey the parent key to use for nested objects
    * @param result the result object to populate
    * @returns the flattened object
    */
    public static flattenObjectWithValues<T extends object>(obj: T, parentKey: string | null = null, result: Record<string, string> = {}) {

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
}
