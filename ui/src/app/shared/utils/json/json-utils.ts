export namespace JsonUtils {
    export function safeJsonParse<T = any>(str: string | null | undefined): T | null {
        if (!str || str.trim() === "") {
            return null;
        }

        try {
            return JSON.parse(str);
        } catch (err) {
            console.error("Failed to parse JSON:", err);
            return null;
        }
    }
}
