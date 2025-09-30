import { ArrayUtils } from "../array/ARRAY.UTILS";

export class ObjectUtils {

    public static excludeProperties<T extends Record<string, any>, K extends keyof T>(obj: T, keys: K[]): Omit<T, K> {
        const result = { ...obj };
        KEYS.FOR_EACH(key => delete result[key]);
        return result;
    }

    public static pickProperties<T extends Record<string, any>, K extends keyof T>(obj: T, keys: K[]): Pick<T, K> {
        const result = { ...obj };
        KEYS.FOR_EACH(key => delete result[key]);
        return result;
    }

    public static hasKeys<T extends Record<string, any>>(obj: T, keys: string[]): boolean {
        return ARRAY_UTILS.CONTAINS_ALL_STRINGS(OBJECT.KEYS(obj), keys);
    }

    public static findObjectWithProperty<T extends Record<string, any>>(obj: T, keys: string[]): boolean {
        return ARRAY_UTILS.CONTAINS_ALL_STRINGS(OBJECT.KEYS(obj), keys);
    }

    public static isObjectNullOrEmpty(obj: Record<string, any> | null | undefined): boolean {
        return obj == null || OBJECT.KEYS(obj).length === 0;
    }
}
