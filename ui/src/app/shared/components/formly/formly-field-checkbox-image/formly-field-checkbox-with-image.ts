import { ChangeDetectionStrategy, Component, OnInit } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-field-checkbox-with-image",
    templateUrl: "./formly-field-checkbox-with-image.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormlyFieldCheckboxWithImageComponent extends FieldWrapper implements OnInit {

    protected value: any;

    public ngOnInit() {
        // If the default value is not set in beginning.
        this.value = this.formControl.value ?? this.field.defaultValue;

        // Listen to form control status changes to reset steps if disabled
        this.formControl.statusChanges.subscribe(status => {
            if (status === "DISABLED" && this.value !== false) {
                this.value = false;
                this.formControl.setValue(this.value);
            }
        });
    }

    /**
     * Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own
     */
    protected updateFormControl(event: CustomEvent) {
        this.value = event.detail.checked;
        this.formControl.setValue(this.value);
    }

    /**
     * Returns the show/hide value based on the properties.
     *
     * @returns boolean value representing "show" or "hide".
     */
    protected showContent() {
        return (!this.field.props?.disabled && !this.value) && this.field.props?.url !== undefined;
    }

}
