/**
 * Utility functions for error handling and message extraction
 */


/**
 * Extracts a user-friendly error message from various types of error objects.
 * @param reason The error object or message to extract the message from.
 * @returns A string containing the extracted error message.
 */
export function extractErrorMessage(reason: any): string {
    const UNKNOWN_ERROR = "Unknown error";
    if (reason == null) {
        return UNKNOWN_ERROR;
    }

    if (typeof reason === "string") {
        return reason;
    }

    if (typeof reason === "object") {

        if( reason?.error?.message == "null") {
            return UNKNOWN_ERROR;
        }

        if (reason?.error?.message) {
            return reason.error.message;
        }

        if (reason?.message && reason.message !== "null") {
            return reason.message;
        }

        if (typeof reason?.error === "string") {
            return reason.error;
        }

        return JSON.stringify(reason);
    }

    return UNKNOWN_ERROR;
}
