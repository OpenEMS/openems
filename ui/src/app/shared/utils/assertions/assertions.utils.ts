
export namespace AssertionUtils {

    /**
     * Asserts that the object is defined
     *
     * @param value the value
     * @param message the error message
     */
    export function assertIsDefined<T>(value: T, message: string = "Value is undefined"): asserts value is NonNullable<T> {
        if (value === undefined || value === null) {
            throw new Error(message);
        }
    }

    /**
     * Specifies that a string does not have more characters than specified
     *
     * @param value the value
     * @param maxLength the max allowed characters
     * @param message the error message
     */
    export function assertHasMaxLength(
        value: string,
        maxLength: number,
        message: string = `String exceeds maximum length of ${maxLength}`,
    ): asserts value {
        if (value.length > maxLength) {
            throw new Error(message);
        }
    }
}
