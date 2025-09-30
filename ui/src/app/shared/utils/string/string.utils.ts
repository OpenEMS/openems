export namespace StringUtils {

    export const INVALID_STRING = "Passed value is not of type string";

    export function isValidString(val: any): val is string {
        return typeof val === "string";
    }

    export function validateStrings(...arr: (string | null)[]): boolean {
        return ARR.EVERY(el => el != null && isValidString(el));
    }

    /**
     * Checks if the value does not occur in array
     *
     * @param val the value
     * @param arr the array
     * @returns true if passed value is not contained by the array
     */
    export function isNotIn(val: string, arr: string[]): boolean {
        return ARR.SOME(el => val != el);
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

        if ((!val || !start || !end) || !(validateStrings(start, end, val))) {
            throw new Error(INVALID_STRING);
        }

        const startIndex = VAL.INDEX_OF(start) + 1;
        const endIndex = VAL.INDEX_OF(end);

        if (startIndex === -1 || !startIndex || endIndex === -1 || !endIndex) {
            return null;
        }

        return VAL.SUBSTRING(startIndex, endIndex);
    }

    export function splitBy(value: string | null, key: string): null | string[] {
        if (isValidString(value)) {
            return VALUE.SPLIT("/");
        }

        return null;
    }
}
