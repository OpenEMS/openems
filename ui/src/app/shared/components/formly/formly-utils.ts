import { AbstractControl } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";

export type NestedKeyOf<ObjectType extends object> = {
    [Key in keyof ObjectType & (string | number)]: ObjectType[Key] extends object
        ? `${Key}` | `${Key}.${NestedKeyOf<ObjectType[Key]>}`
        : `${Key}`;
}[keyof ObjectType & (string | number)];

export type TypedFormlyFieldConfig<T extends object> = Omit<
    FormlyFieldConfig,
    "key" | "model" | "expressions" | "hooks" | "fieldGroup"
> & {
    key?: string;
    model?: T;
    fieldGroup?: TypedFormlyFieldConfig<T>[];
    expressions?: {
        [property: string]: string | ((field: TypedFormlyFieldConfig<T>) => any);
    };
    hooks?: {
        onInit?: (field: TypedFormlyFieldConfig<T>) => void;
    };
};

export type StrictFormlyFieldConfig<T extends object> = Omit<TypedFormlyFieldConfig<T>, "fieldGroup"> & {
    key?: NestedKeyOf<T>;
    fieldGroup?: StrictFormlyFieldConfig<T>[];
};

export namespace FormlyUtils {
    /**
     * Returns a CSS style object based on the control's validation state.
     *
     * @param control The Angular AbstractControl to check
     * @param isFocused Whether the field is currently focused
     * @param cssProperty The CSS property to target (e.g., 'border-color', 'border-bottom-color')
     */
    export function getControlStyle(
        control: AbstractControl | null | undefined,
        isFocused: boolean,
        cssProperty: string,
    ): { [key: string]: string } {
        let color = "var(--ion-color-dark)"; // Default

        if (control !== null && control !== undefined) {
            if (control.touched && control.invalid) {
                color = "var(--ion-color-danger)";
            } else if (control.valid && (control.dirty || control.touched || control.value)) {
                color = "var(--ion-color-success)";
            } else if (isFocused) {
                color = "var(--ion-color-primary)";
            }
        }

        return { [cssProperty]: color };
    }

    /**
     * Gets the formly field props safely.
     *
     * @param key The key
     * @param fields The formly fields
     * @returns Formly field props if existing, else null
     */
    export function changeFormlyFieldProps(
        key: FormlyFieldConfig["key"],
        fields: FormlyFieldConfig[],
        callback: (props: FormlyFieldConfig["props"]) => FormlyFieldConfig["props"],
    ): FormlyFieldConfig[] {
        const field = fields.find((el) => el.key === key) ?? null;
        if (field == null || field.props == null) {
            return fields;
        }
        field.props = callback(field.props);
        return fields;
    }

    /** Evaluates to TRUE if the boolean property is TRUE. */
    export function propIsTrue<T extends object, K extends keyof T = keyof T>(
        propName: K,
        defaultWhenMissing: boolean = false,
    ) {
        return (field: TypedFormlyFieldConfig<T>): boolean => {
            if (!field.model) {
                return defaultWhenMissing;
            }
            return field.model[propName] === true;
        };
    }

    /** Evaluates to TRUE if the boolean property is FALSE. */
    export function propIsFalse<T extends object, K extends keyof T = keyof T>(
        propName: K,
        defaultWhenMissing: boolean = true,
    ) {
        return (field: TypedFormlyFieldConfig<T>): boolean => {
            if (!field.model) {
                return defaultWhenMissing;
            }
            return field.model[propName] === false;
        };
    }

    /**
     * Combines multiple Formly expression functions with a logical OR. Returns true if ANY of the provided functions
     * evaluate to true.
     */
    export function combineOr<T extends object>(...conditions: ((field: TypedFormlyFieldConfig<T>) => boolean)[]) {
        return (field: TypedFormlyFieldConfig<T>): boolean => {
            return conditions.some((condition) => condition(field));
        };
    }

    /**
     * Combines multiple Formly expression functions with a logical AND. Returns true ONLY if ALL of the provided
     * functions evaluate to true.
     */
    export function combineAnd<T extends object>(...conditions: ((field: TypedFormlyFieldConfig<T>) => boolean)[]) {
        return (field: TypedFormlyFieldConfig<T>): boolean => {
            return conditions.every((condition) => condition(field));
        };
    }

    /** Evaluates to TRUE (hides) if ANY of the provided boolean properties are FALSE. */
    export function anyPropIsFalse<T extends object, K extends keyof T = keyof T>(...propNames: K[]) {
        return (field: TypedFormlyFieldConfig<T>): boolean => {
            if (!field.model) {
                return true;
            } // Default to hide if no model

            // Returns true if some property in the array is exactly false
            return propNames.some((prop) => field.model![prop] === false);
        };
    }

    /**
     * Evaluates to TRUE if the property EQUALS the expected value.
     *
     * @param defaultWhenMissing What to return if the model hasn't loaded yet (default: false)
     */
    export function propEquals<T extends object, K extends keyof T = keyof T>(
        propName: K,
        expectedValue: T[K],
        defaultWhenMissing: boolean = false,
    ) {
        return (field: TypedFormlyFieldConfig<T>): boolean => {
            if (!field.model) {
                return defaultWhenMissing;
            }
            return field.model[propName] === expectedValue;
        };
    }

    /**
     * Evaluates to TRUE if the property does NOT EQUAL the expected value.
     *
     * @param defaultWhenMissing What to return if the model hasn't loaded yet (default: true)
     */
    export function propNotEquals<T extends object, K extends keyof T = keyof T>(
        propName: K,
        expectedValue: T[K],
        defaultWhenMissing: boolean = true,
    ) {
        return (field: TypedFormlyFieldConfig<T>): boolean => {
            if (!field.model) {
                return defaultWhenMissing;
            }
            return field.model[propName] !== expectedValue;
        };
    }
}
