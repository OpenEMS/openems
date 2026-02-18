import { AbstractControl } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";

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
        cssProperty: string
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
     * @param key the key
     * @param fields the formly fields
     * @returns formly field props if existing, else null
     */
    export function changeFormlyFieldProps(
        key: FormlyFieldConfig["key"],
        fields: FormlyFieldConfig[],
        callback: (props: FormlyFieldConfig["props"]) => FormlyFieldConfig["props"]
    ): FormlyFieldConfig[] {
        const field = fields.find(el => el.key === key) ?? null;
        if (field == null || field.props == null) {
            return fields;
        }
        field.props = callback(field.props);
        return fields;
    }
}
