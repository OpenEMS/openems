export namespace StringUtils {
    export function isNot(val: string, arr: string[]): boolean {
        return arr.some(el => val != el);
    }
}
