import { ChangeDetectionStrategy, Component, OnInit } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "help-popover-label-with-description-and-checkbox",
    templateUrl: "./help-popover-label-with-description-and-checkbox.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormlyFieldCheckboxWithLabelComponent extends FieldType implements OnInit {
    protected value: any;

    public ngOnInit() {
        // If the default value is not set in beginning.
        this.value = this.formControl.value ?? this.field.defaultValue;

        // Listen to form control status changes to reset steps if disabled
        this.formControl.statusChanges.subscribe(status => {
            if (status === "DISABLED" && this.value !== false) {
                this.value = false;
                this.formControl.setValue(this.value);
                this.formControl.markAsDirty();
            }
        });
    }

    /**
     * Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own
     */
    protected updateFormControl(event: CustomEvent) {
        this.value = event.detail.checked;
        this.formControl.setValue(this.value);
        this.formControl.markAsDirty();
    }
}
