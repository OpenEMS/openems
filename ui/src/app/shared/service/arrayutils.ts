export namespace ArrayUtils {
  export function equalsCheck(a: any[], b: any[]) {
    return a.length === b.length &&
      a.every((v, i) => v === b[i]);
  }
}
