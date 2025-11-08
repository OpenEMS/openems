import { ArrayUtils } from "../array/array.utils";

export namespace StringUtils {

    export const INVALID_STRING = "Passed value is not of type string";
    export type UppercaseString<T extends string> = T extends Uppercase<T> ? T : never;

    export function isValidString(val: any): val is string {
        const isString = typeof val === "string";
        if (!isString) {
            throw new Error(INVALID_STRING);
        }
        return isString;
    }

    export function validateStrings(arr: string[] | null): boolean {
        return arr?.every(el => el != null && isValidString(el)) ?? false;
    }

    /**
     * Checks if the value does not occur in array
     *
     * @param val the value
     * @param arr the array
     * @returns true if passed value is not contained by the array
     */
    export function isNotInArr(val: string | null, arr: string[] | null): boolean {
        ArrayUtils.isValidArr(arr);
        StringUtils.isValidString(val);
        StringUtils.validateStrings(arr);
        return arr?.every(el => val != el) ?? true;
    }

    /**
     * Checks if the value does occur in array
     *
     * @param val the value
     * @param arr the array
     * @returns true if passed value is ocurring in the array
     */
    export function isInArr(val: string, arr: string[]): boolean {
        return arr.some(el => val == el);
    }

    /**
     * Gets the substring between a start and end character
     *
     * @param start the start character
     * @param end the end character
     * @param val the value
     * @returns a string, if valid, else null
     */
    export function getSubstringInBetween(start: string | null, end: string | null, val: string | null): string | null {

        if ((!val || !start || !end) || !(validateStrings([start, end, val]))) {
            throw new Error(INVALID_STRING);
        }

        const startIndex = val.indexOf(start) + 1;
        const endIndex = val.indexOf(end);

        if (startIndex === -1 || !startIndex || endIndex === -1 || !endIndex) {
            return null;
        }

        return val.substring(startIndex, endIndex);
    }

    export function splitBy(value: string | null, key: string): null | string[] {
        if (isValidString(value)) {
            return value.split(key);
        }

        return null;
    }

    export function splitByGetIndexSafely(value: string | null, key: string, index: number): null | string {
        const arr = StringUtils.splitBy(value, key);
        if (arr == null || arr.length == 0) {
            return null;
        }
        return arr[index];
    }
}
