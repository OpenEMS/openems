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
    export function findFormControlSafely(f: FormGroup, formControlName: string): AbstractControl | null {

        let result: AbstractControl | null = null;
        const controls = f.controls;
        if (formControlName in controls) {
            result = f.controls[formControlName];
            return result;
        }

        Object.values(controls).map(el => {
            if (el instanceof FormGroup) {
                result = FormUtils.findFormControlSafely(el, formControlName);
            }

            if (el instanceof FormControl) {
                if (el.value instanceof FormGroup) {
                    result = FormUtils.findFormControlSafely(el.value, formControlName);
                }
            }
        });

        return result;
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
            return field.props != null && key in field.props;
        });
    }
};
