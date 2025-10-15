import { AbstractControl } from "@angular/forms";

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
}
