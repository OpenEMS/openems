export namespace PromiseUtils {
    export namespace Functions {
        export function handleOrElse<T>(promise: Promise<T>, orElse: T): Promise<[null | Error, T]> {
            return promise
                .then((data): [null, T] => [null, data])
                .catch((err): [Error, T] => [err, orElse]);
        }
        /**
            * Handles jsonRpcRequests
            *
            * @param promise the promise
            * @returns either an error or the result
            */
        export function handle<T>(promise: Promise<T>): Promise<[Error | null, T | null]> {
            return promise
                .then((data): [null, T] => [null, data])
                .catch((err: Error): [Error, null] => [err, null]);
        }
    }
    export namespace Types {
        export type PromiseFns<T> = Parameters<ConstructorParameters<typeof Promise<T>>[0]>;
        export type Resolve<T = any> = PromiseFns<T>[0];
        export type Reject<T = any> = PromiseFns<T>[1];
    }
}
