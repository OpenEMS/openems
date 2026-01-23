/** Utility generic types */

import { Signal } from "@angular/core";

/** Generic Type for a key-value pair */
export type TKeyValue<T> = {
    key: string,
    value: T
};

export type TOmitBy<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

/** Creates new type of type with optional properties */
export type TPartialBy<T, K extends keyof T> = TOmitBy<T, K> & Partial<Pick<T, K>>;

/** Creates new type of type with all required properties */
export type TRequiredBy<T, K extends keyof T> = {
    [P in K]-?: T[P];
};

/** Required type of type with all properties optional and accepts additional properties */
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

export type TIntRange<F extends number, T extends number> = Exclude<TRange<T>, TRange<F>>;

/** Empty Obj */
export type EmptyObj = Record<PropertyKey, never>;

// Type helpers

/** Creates/Extracts new type from signal */
export type TSignalValue<T> = T extends Signal<infer V> ? V : never;

/** Creates a union type from enum keys */
export type TEnumKeys<T extends Record<string, string | number>> = Extract<keyof T, string>;

/** Creates a type from an array for one element */
export type TArrayElement<ArrayType extends readonly unknown[]> =
    ArrayType extends readonly (infer ElementType)[] ? ElementType : never;

/** Creates a mutable type from an unmutable type */
export type TMutable<T> = {
    -readonly [P in keyof T]: T[P];
};
