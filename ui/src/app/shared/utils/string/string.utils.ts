export namespace StringUtils {

    /**
     * Checks if the value does not occur in array 
     * 
     * @param val the value
     * @param arr the array
     * @returns true if passed value is not contained by the array
     */
    export function isNotIn(val: string, arr: string[]): boolean {
        return arr.some(el => val != el);
    }
}
