/** Helper functions for interacting with numbers. */
export namespace NumberUtils {
    /**
     * Parses a string value to a number.
     *
     * @param value The value
     * @returns The casted value if parsable, else null
     */
    export function parseNumberSafely(value: number | string | null): number | null {
        if (value == null || value == "") {
            return null;
        }
        const castedValue = Number.parseInt(value.toString());
        if (castedValue == null || Number.isFinite(castedValue) == false) {
            return null;
        }

        return castedValue;
    }

    /**
     * Parses a string value to a number.
     *
     * @param value The value
     * @param orElse The orElse if value is not parsable
     * @returns The casted value if parsable, else null
     */
    export function parseNumberSafelyOrElse(value: number | string | null, orElse: number): number {
        if (value == null) {
            return orElse;
        }

        const castedValue = NumberUtils.parseNumberSafely(value);

        if (castedValue == null) {
            return orElse;
        }

        return castedValue;
    }

    /**
     * Adds values to each other - possibly null values
     *
     * @param values The values
     * @returns A number, if at least one value is not null, else null
     */
    export function addSafely(...values: (number | null)[]): number | null {
        return values
            .filter((value) => value !== null && value !== undefined)
            .reduce((sum: number | null, curr) => {
                if (sum == null) {
                    sum = curr;
                } else {
                    sum += curr;
                }

                return sum;
            }, null);
    }

    /**
     * Subtracts values from each other - possibly null values
     *
     * @param values The values
     * @returns A number, if at least one value is not null, else null
     */
    export function subtractSafely(...values: (number | null)[]): number | null {
        return values
            .filter((value) => value !== null && value !== undefined)
            .reduce((sum: number | null, curr: number) => {
                if (sum == null) {
                    sum = curr;
                } else {
                    sum -= curr;
                }

                return sum;
            }, null);
    }

    /**
     * Dividing values from each other - possibly null values
     *
     * @param dividend The dividend value
     * @param divisor The divisor value
     * @returns The quotient, if both values are not null and divisor is not zero, else null
     */
    export function divideSafely(dividend: number | null, divisor: number | null): number | null {
        if (dividend == null || divisor == null) {
            return null;
        } else if (divisor == 0) {
            return null; // divide by zero
        } else {
            return dividend / divisor;
        }
    }

    /**
     * Multiplying values with each other - possibly null values
     *
     * @param values The values
     * @returns A number, if at least one value is not null, else null
     */
    export function multiplySafely(...values: (number | null)[]): number | null {
        const [firstFactor, ...furtherFactors] = values;
        if (firstFactor == null) {
            return null;
        }

        let result = firstFactor;
        for (const factor of furtherFactors) {
            if (factor != null) {
                result *= factor;
            }
        }

        return result;
    }

    /**
     * Ceils a value safely.
     *
     * @param value The value
     * @returns The smallest integer greater than or equal to its numeric argument, if valid, else null
     */
    export function ceilSafely(value: number | null): number | null {
        if (value === null) {
            return null;
        }
        return Math.ceil(value);
    }

    /**
     * Floors a value safely.
     *
     * @param value The value
     * @returns The greatest integer less than or equal to its numeric argument, if valid, else null
     */
    export function floorSafely(value: number | null): number | null {
        if (value === null) {
            return null;
        }
        return Math.floor(value);
    }

    /**
     * Converts the number to have a max value
     *
     * @param value The value
     * @param atMost The max number to be allowed
     * @returns The value
     */
    export function convertNumberToBeAtMost(value: number | null, atMost: number): number | null {
        if (value == null) {
            return value;
        }
        return Math.min(value, atMost);
    }

    /**
     * Checks whether a value is a finite number (not NaN/Infinity).
     *
     * Useful as a type guard before numeric calculations on unknown input.
     *
     * @param value The value to validate
     * @returns True if value is a finite number, else false
     */
    export function isPresentNumber(value: unknown): value is number {
        return typeof value === "number" && Number.isFinite(value);
    }

    /**
     * Converts a number to a boolean.
     *
     * @param value The value to convert
     * @param orElse The value to return if the input is not 0 or 1
     * @returns False for 0, true for 1, else orElse
     */
    export function numberToBooleanOrElse(value: 0 | 1, orElse: boolean): boolean {
        if (value === 0) {
            return false;
        }
        if (value === 1) {
            return true;
        }
        return orElse;
    }
}
