import { ArrayUtils } from "../array/array.utils";

export class ObjectUtils {

    public static excludeProperties<T extends Record<string, any>, K extends keyof T>(obj: T, keys: K[]): Omit<T, K> {
        const result = { ...obj };
        keys.forEach(key => delete result[key]);
        return result;
    }

    public static hasKeys<T extends Record<string, any>>(obj: T, keys: string[]): boolean {
        return ArrayUtils.containsAllStrings(Object.keys(obj), keys)
    }
}
