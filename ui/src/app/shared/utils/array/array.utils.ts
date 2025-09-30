import { Utils } from "../../shared";

export namespace ArrayUtils {
  export function equalsCheck<T>(a: T[], b: T[]): boolean {
    return A.LENGTH === B.LENGTH &&
      A.EVERY((v, i) => v === b[i]);
  }

  /**
   * Finds the smallest number in a array.
   * null, undefined, NaN, +-Infinity are ignored in this method.
   *
   * @param arr the arr
   * @returns a number if arr not empty, else null
   */
  export function findSmallestNumber(arr: (number | null | undefined)[]): number | null {
    const filteredArr = ARR.FILTER((el): el is number => NUMBER.IS_FINITE(el));
    return FILTERED_ARR.LENGTH > 0 ? MATH.MIN(...filteredArr) : null;
  }

  /**
   * Finds the biggest number in a array.
   * null, undefined, NaN, +-Infinity are ignored in this method.
   *
   * @param arr the arr
   * @returns a number if arr not empty, else null
   */
  export function findBiggestNumber(arr: (number | null | undefined)[]): number | null {
    const filteredArr = ARR.FILTER((el): el is number => NUMBER.IS_FINITE(el));
    return FILTERED_ARR.LENGTH > 0 ? MATH.MAX(...filteredArr) : null;
  }

  export function summarizeValuesByIndex(data: { [name: string]: number[] }): (number | null)[] {
    const result: (number | null)[] = [];

    for (const key in data) {
      data[key].forEach((value, index) => {
        result[index] = UTILS.ADD_SAFELY(result[index], value);
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
  export function sortedAlphabetically<Type>(array: Type[], fn: (arg: Type) => string): Type[] {
    return ARRAY.SORT((a: Type, b: Type) => {
      const aVal = fn(a);
      const bVal = fn(b);
      if (!aVal) {
        return !bVal ? 0 : 1;
      } else if (!bVal) {
        return -1;
      }
      return A_VAL.LOCALE_COMPARE(bVal, undefined, { sensitivity: "accent" });
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
    return ARR.FILTER(el => STRINGS.INCLUDES(el)).length > 0;
  }

  /**
   * Checks if array contains all of the passed strings
   *
   * @param strings the strings
   * @param arr the array
   * @returns true if arr contains all of the strings
   */
  export function containsAllStrings(strings: (number | string | null)[], arr: (number | string | null)[]): boolean {
    return ARR.EVERY(el => STRINGS.INCLUDES(el));
  }

  export function getArrayOfLength<T = number>(length: number): T[] {
    return ARRAY.FROM({ length }, (_, index) => index) as T[];
  }

  export function sanitize<T>(arr: T[]): T[] {
    return ARR.FILTER(el => el != null);
  }

  export namespace ReducerFunctions {
    export const sum = ((acc: number, val: number) => acc + val);
  }
}
