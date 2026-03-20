import { Utils } from "../../shared";
import { ObjectUtils } from "../object/object.utils";

export namespace ArrayUtils {

    export const INVALID_ARRAY = "Passed value is not a array";

    export function isValidArr<T>(val: T[] | null): val is T[] {
        const isArray = Array.isArray(val);
        if (!isArray) {
            throw new Error(INVALID_ARRAY);
        }
        return isArray;
    }
    export function equalsCheck<T>(a: T[], b: T[]): boolean {
        return a.length === b.length &&
            a.every((v, i) => v === b[i]);
    }

    /**
     * Finds the smallest number in a array.
     * null, undefined, NaN, +-Infinity are ignored in this method.
     *
     * @param arr the arr
     * @returns a number if arr not empty, else null
     */
    export function findSmallestNumber(arr: (number | null | undefined)[]): number | null {
        const filteredArr = arr.filter((el): el is number => Number.isFinite(el));
        return filteredArr.length > 0 ? Math.min(...filteredArr) : null;
    }

    /**
     * Finds the biggest number in a array.
     * null, undefined, NaN, +-Infinity are ignored in this method.
     *
     * @param arr the arr
     * @returns a number if arr not empty, else null
     */
    export function findBiggestNumber(arr: (number | null | undefined)[]): number | null {
        const filteredArr = arr.filter((el): el is number => Number.isFinite(el));
        return filteredArr.length > 0 ? Math.max(...filteredArr) : null;
    }

    export function summarizeValuesByIndex(data: { [name: string]: number[] }): (number | null)[] {
        const result: (number | null)[] = [];

        for (const key in data) {
            data[key].forEach((value, index) => {
                result[index] = Utils.addSafely(result[index], value);
            });
        }

        return result;
    }

    /**
     * Sort arrays alphabetically, according to the string returned by fn.
     * Elements for which fn returns null or undefined are sorted to the end in an undefined order.
     *
     * @param array to sort
     * @param fn to get a string to sort by
     * @returns sorted array
     */
    export function sortedAlphabetically<T>(array: T[], fn: (arg: T) => string): T[] {
        return array.sort((a: T, b: T) => {
            const aVal = fn(a);
            const bVal = fn(b);
            if (!aVal) {
                return !bVal ? 0 : 1;
            } else if (!bVal) {
                return -1;
            }
            return aVal.localeCompare(bVal, undefined, { sensitivity: "accent" });
        });
    }

    /**
     * Checks if array contains at least one of the passed strings
     *
     * @param strings the strings
     * @param arr the array
     * @returns true if arr contains at least one of the strings
     */
    export function containsStrings(strings: (number | string | null)[], arr: (number | string | null)[]): boolean {
        return arr.filter(el => strings.includes(el)).length > 0;
    }

    /**
     * Checks if array contains all of the passed strings
     *
     * @param strings the strings
     * @param arr the array
     * @returns true if arr contains all of the strings
     */
    export function containsAll<T>({ arr, strings = [] }: { arr: T[]; strings: T[] }): boolean {
        return arr.every(el => strings.includes(el));
    }

    export function getArrayOfLength<T = number>(length: number): T[] {
        return Array.from({ length }, (_, index) => index) as T[];
    }

    /**
     * Sanitizes the arr from null and undefined values.
     *
     * @param arr the arr
     * @returns the sanitized arr
     */
    export function sanitize<T>(arr: (T | null)[]): T[] {
        return arr.filter(el => el != null);
    }

    /**
     * Removes overlapping elements between arrays from input array.
     *
     * @param inputArr the arr to remove elements from
     * @param values the arrays to match against {@link inputArr}
     * @returns arr1 with distinct elements
     */
    export function removeMatching<T extends any[]>(inputArr: T | null, ...values: (T | null)[]): T | null {

        if (inputArr == null || values == null || values?.length === 0) {
            return null;
        }

        const restArrays = values.flat(1);
        return inputArr?.filter(item => restArrays.includes(item) == false) as T ?? null;
    }


    export function findObjectInArray<T extends object>(array: T[], target: Partial<T>): T | undefined {
        return array.find(obj =>
            deepEqual(obj, target)
        );
    }

    export function deepEqual(a: any, b: any): boolean {
        if (a === b) { return true; }

        if (typeof a !== "object" || a === null || typeof b !== "object" || b === null) { return false; }

        const keysA = Object.keys(a);
        const keysB = Object.keys(b);

        if (keysA.length !== keysB.length) { return false; }

        return keysA.every(key => deepEqual(a[key], b[key]));
    }

    export function arraysDeepEqualHash<T = any>(arr1: T[], arr2: T[]): boolean {
        if (arr1 == null || arr2 == null) {
            return false;
        }
        if (arr1.length !== arr2.length) {
            return false;
        }

        for (let i = 0; i < arr1.length; i++) {
            if (hashObject(arr1[i]) !== hashObject(arr2[i])) {
                return false;
            };
        }
        return true;
    }

    function hashObject(obj: any): string {
        return JSON.stringify(obj, Object.keys(obj).sort()); // deterministic key order
    }

    export function getFirstElementWhereOrNull<T extends Record<string, any>, K extends keyof T>(arr: T[], property: K, propertyValue: string): T | null {
        const results = arr.filter(el => ObjectUtils.getKeySafely(el, property) === propertyValue) ?? [];

        if (results.length == 1) {
            return arr[0];
        }
        return null;
    }


    export namespace ReducerFunctions {
        export const sum = ((acc: number, val: number) => acc + val);
        export const STRINGIFY_SAFELY: (
            arr: string[],
            item: string | null | undefined) => string[] =
            (arr, item) => {
                if (item == null) {
                    return arr;
                }
                arr.push(item.toString());
                return arr;
            };
    }
}
