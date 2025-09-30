import { ChangeDetectionStrategy, Component, OnInit } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "help-popover-label-with-description-and-checkbox",
    templateUrl: "./help-popover-label-with-description-and-CHECKBOX.HTML",
    changeDetection: CHANGE_DETECTION_STRATEGY.ON_PUSH,
    standalone: false,
})
export class FormlyFieldCheckboxWithLabelComponent extends FieldType implements OnInit {
    protected value: any;

    public ngOnInit() {
        // If the default value is not set in beginning.
        THIS.VALUE = THIS.FORM_CONTROL.VALUE ?? THIS.FIELD.DEFAULT_VALUE;

        // Listen to form control status changes to reset steps if disabled
        THIS.FORM_CONTROL.STATUS_CHANGES.SUBSCRIBE(status => {
            if (status === "DISABLED" && THIS.VALUE !== false) {
                THIS.VALUE = false;
                THIS.FORM_CONTROL.SET_VALUE(THIS.VALUE);
                THIS.FORM_CONTROL.MARK_AS_DIRTY();
            }
        });
    }

    /**
     * Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own
     */
    protected updateFormControl(event: CustomEvent) {
        THIS.VALUE = EVENT.DETAIL.CHECKED;
        THIS.FORM_CONTROL.SET_VALUE(THIS.VALUE);
        THIS.FORM_CONTROL.MARK_AS_DIRTY();
    }
}
