import { AbstractControl, FormControl, FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";

/**
 * Helper functions for interacting with angular forms and formGroups
 */
export namespace FormUtils {

    /**
     * Searches for a formControl in a given formGroup
     *
     * @param f the formGroup
     * @param formControlName the control to search for
     * @returns the formControl if found, else null
     */
    export function findFormControlSafely<T extends AbstractControl>(f: FormGroup | null, formControlName: string): AbstractControl | null {

        if (f == null) {
            return null;
        }

        const controls = f.controls;

        if (formControlName in controls) {
            return controls[formControlName] as T;
        }

        for (const el of Object.values(controls)) {
            if (el instanceof FormGroup) {
                const result = findFormControlSafely<T>(el, formControlName);
                if (result != null) {
                    return result;
                }
            } else if (el instanceof FormControl && el.value instanceof FormGroup) {
                const result = findFormControlSafely<T>(el.value, formControlName);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }


    /**
     * Finds a formControls in a given formGroup and returns the value
     *
     * @template T The expected type of the control's value.
     * @param f the formGroup
     * @param formControlName the formControl to search for
     * @returns the <T>control if found, else null
     */
    export function findFormControlsValueSafely<T>(f: FormGroup, formControlName: string): T | null {
        const formControl: AbstractControl | null = FormUtils.findFormControlSafely(f, formControlName);

        if (!formControl) {
            return null;
        }

        return formControl.value;
    }

    /**
     * Filters fields with a specific props key
     *
     * @param fields the fields
     * @param key the key to look for
     * @returns fields if key in {@link FormlyFieldConfig.props formlyfield props}, else empty arr
     */
    export function filterFieldPropsWithKey(fields: FormlyFieldConfig[], key: string): FormlyFieldConfig[] {
        return fields.filter(field => {
            return field?.props != null && key in field.props;
        });
    }
};
