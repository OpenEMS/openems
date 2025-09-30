// @ts-strict-ignore
import { ChangeDetectionStrategy, Component, OnInit } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "form-field-checkbox-hyperlink",
    templateUrl: "./form-field-checkbox-HYPERLINK.WRAPPER.HTML",
    changeDetection: CHANGE_DETECTION_STRATEGY.ON_PUSH,
    standalone: false,
})
export class FormlyCheckBoxHyperlinkWrapperComponent extends FieldWrapper implements OnInit {

    protected secondLabel: string;

    public ngOnInit() {
        // If the default value is not set in beginning.
        if (!THIS.FORM_CONTROL.VALUE) {
            THIS.FORM_CONTROL.SET_VALUE(THIS.FIELD.PROPS.DEFAULT_VALUE);
        }

        // Since its a custom wrapper, we are seperating label with checkbox.
        // mentioning required to true does not generate (*) to the label, so we are hard coding it.
        if (THIS.FIELD.PROPS.REQUIRED) {
            THIS.SECOND_LABEL = THIS.FIELD.PROPS.DESCRIPTION + "*";
        } else {
            THIS.SECOND_LABEL = THIS.FIELD.PROPS.DESCRIPTION;
        }
    }
}
