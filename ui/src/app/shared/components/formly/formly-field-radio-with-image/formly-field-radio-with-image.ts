import { ChangeDetectionStrategy, Component, OnInit } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-field-radio-with-image",
    templateUrl: "./formly-field-radio-with-IMAGE.HTML",
    changeDetection: CHANGE_DETECTION_STRATEGY.ON_PUSH,
    standalone: false,
})
export class FormlyFieldRadioWithImageComponent extends FieldWrapper implements OnInit {

    protected value: any;

    public ngOnInit() {
        THIS.VALUE = THIS.FIELD.DEFAULT_VALUE;
        if (THIS.FORM_CONTROL.GET_RAW_VALUE()) {
            THIS.VALUE = THIS.FORM_CONTROL.GET_RAW_VALUE();
        }
    }

    /**
     * Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own
     */
    protected updateFormControl() {
        THIS.FORM_CONTROL.SET_VALUE(THIS.VALUE);
    }
}
