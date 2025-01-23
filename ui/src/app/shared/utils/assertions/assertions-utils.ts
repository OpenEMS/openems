
export namespace AssertionUtils {

    export function assertIsDefined<T>(value: T, message: string = "Value is undefined"): asserts value is NonNullable<T> {
        if (value === undefined || value === null) {
            throw new Error(message);
        }
    }

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
