export namespace ArrayUtils {
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

  /**
 * Sort arrays alphabetically, according to the string returned by fn.
 * Elements for which fn returns null or undefined are sorted to the end in an undefined order.
 *
 * @param array to sort
 * @param fn to get a string to sort by
 * @returns sorted array
 */
  export function sortedAlphabetically<Type>(array: Type[], fn: (arg: Type) => string): Type[] {
    return array.sort((a: Type, b: Type) => {
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
  * Checks if array contains at least one of the passed strings
  *
  * @param strings the strings
  * @param arr the array
  * @returns true if arr contains all of the strings
  */
  export function containsAllStrings(strings: (number | string | null)[], arr: (number | string | null)[]): boolean {
    return arr.every(el => strings.includes(el));
  }
}
