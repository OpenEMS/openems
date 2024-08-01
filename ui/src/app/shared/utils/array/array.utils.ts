export namespace ArrayUtils {
  export function equalsCheck<T>(a: T[], b: T[]): boolean {
    return a.length === b.length &&
      a.every((v, i) => v === b[i]);
  }

  /**
   * Finds the smallest number in a array
   *
   * @param arr the arr
   * @returns a number if arr not empty, else null
   */
  export function findSmallestNumber(arr: number[]): number | null {
    if (arr?.length === 0 || arr?.every(el => el == null)) {
      return null; // Return undefined for an empty array or handle it based on your requirements
    }
    return Math.min(...(arr.filter(Number.isFinite)));
  }

  /**
   * Finds the biggest number in a array
   *
   * @param arr the arr
   * @returns a number if arr not empty, else null
   */
  export function findBiggestNumber(arr: number[]): number | null {
    if (arr?.length === 0 || arr?.every(el => el == null)) {
      return null; // Return undefined for an empty array or handle it based on your requirements
    }

    return Math.max(...(arr.filter(Number.isFinite)));
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
      return aVal.localeCompare(bVal, undefined, { sensitivity: 'accent' });
    });
  }
}
