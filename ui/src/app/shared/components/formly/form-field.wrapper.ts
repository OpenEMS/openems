import { Component, ViewEncapsulation } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-wrapper-ion-form-field",
    templateUrl: "./form-field.wrapper.html",
    encapsulation: ViewEncapsulation.None,
    styles: [`
        formly-field-ion-toggle, formly-field-ion-checkbox, formly-custom-select,
        formly-input-serial-number, formly-custom-select {
            width: 100%;
        }

        formly-field-ion-toggle, ion-toggle  {
            padding: 0 !important;
            margin: 0 !important;
            transform: none !important;
        }

        ion-toggle::part(label),
        ion-checkbox::part(label),
        ion-radio::part(label) {
            white-space: pre-wrap !important;
            flex: 1;
            margin-inline-end: 0 !important;
        }
    `],
    standalone: false,
})
export class FormlyWrapperFormFieldComponent extends FieldWrapper {

    get itemLines(): "none" | "inset" {
        // Helper to find the nearest fieldGroup that represents the whole form (or subform)
        const rootGroup = this.getRootFieldGroup();

        if (rootGroup == null || rootGroup.length == 0) {
            return "inset";
        }

        // Consider only visible fields
        const visibleFields = rootGroup.filter(
            f => !f.hide && !f.props?.hidden
        );

        const lastVisibleField = visibleFields[visibleFields.length - 1];
        const isLastVisibleField: boolean = lastVisibleField === this.field;

        // Always show a bottom line for the last visible field in the form
        if (isLastVisibleField) {
            return "inset";
        }

        // Special case: input with description. (It has it's own line within the input template)
        if (this.field.type === "input" && this.props.description != null) {
            return "none";
        }

        // Default case
        return "inset";
    }

    private getRootFieldGroup(): any[] | null {
        let current = this.field.parent;
        let lastFieldGroup = null;

        while (current) {
            if (Array.isArray(current.fieldGroup)) {
                lastFieldGroup = current.fieldGroup;
            }
            current = current.parent;
        }

        return lastFieldGroup;
    }
}
