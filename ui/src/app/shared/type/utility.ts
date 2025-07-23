/** Utility generic types */

/** Generic Type for a key-value pair */
export type TKeyValue<T> = {
    key: string,
    value: T
};

export type TOmitBy<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

/** Creates new type of type with optional properties  */
export type TPartialBy<T, K extends keyof T> = TOmitBy<T, K> & Partial<Pick<T, K>>;

/** Creates new type of type with all properties optional and accepts additional properties */
export type TAllPartialWithExtraProps<T> = {
    [K in keyof T]?: T[K] extends object
    ? T[K] extends (...args: any[]) => any
    ? T[K]
    : TAllPartialWithExtraProps<T[K]>
    : T[K];
} & {
    [key: string]: any;
};

/** Creates new type from property of type */
export type TPropType<TObj, TProp extends keyof TObj> = TObj[TProp];

/** Creates new number type, that only accepts numbers in a range  */
export type TRange<N extends number, Acc extends number[] = []> = Acc["length"] extends N
    ? Acc[number]
    : TRange<N, [...Acc, Acc["length"]]>;

/** Empty Obj */
export type EmptyObj = Record<PropertyKey, never>;
