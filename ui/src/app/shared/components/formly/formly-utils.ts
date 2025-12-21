import { AbstractControl } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";

export namespace FormlyUtils {

    /**
     * Determines the border-bottom-color for a form field based on its state.
     *
     * @param formControl The form control instance.
     * @param isFocused A boolean indicating if the field is currently focused.
     * @returns An object with the CSS 'border-bottom-color' property.
     */
    export function getBorderBottomColor(
        formControl: AbstractControl,
        isFocused: boolean
    ): { [key: string]: string } {
        let borderBottomColor = "var(--ion-color-dark)"; // Default color

        if (formControl.invalid && formControl.touched) {
            borderBottomColor = "var(--highlight-color-invalid)";
        } else if (formControl.valid) {
            borderBottomColor = "var(--highlight-color-valid)";
        } else if (isFocused) {
            borderBottomColor = "var(--highlight-color-focused)";
        }

        return {
            "border-bottom-color": borderBottomColor,
        };
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
