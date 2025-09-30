import { Component, OnInit } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "form-field-multi-step",
    templateUrl: "./form-field-multi-STEP.HTML",
    standalone: false,
})
export class FormlyFieldMultiStepComponent extends FieldType implements OnInit {

    protected get steps() {
        return THIS.PROPS.STEPS || [];
    }

    public ngOnInit() {
        // Ensure the model has an array to track steps
        const stepArray = THIS.FORM_CONTROL.VALUE;

        if (!ARRAY.IS_ARRAY(stepArray)) {
            THIS.FORM_CONTROL.SET_VALUE(Array(THIS.STEPS.LENGTH).fill(false));
        }
    }

    protected onCheckboxChange(event: any, index: number) {
        const updatedValue = THIS.FORM_CONTROL.VALUE;
        updatedValue[index] = EVENT.DETAIL.CHECKED;
        THIS.FORM_CONTROL.SET_VALUE(updatedValue);
    }


    /**
     * Returns the show/hide value based on the properties.
     *
     * @returns boolean value representing "show" or "hide".
     */
    protected showContent(index: number) {
        return (!THIS.FIELD.PROPS?.disabled && !THIS.FORM_CONTROL.VALUE[index]);
    }

    /**
     * Returns true/false value based on the step array values.
     *
     * @returns boolean value representing all values are true or not.
     */
    protected isAllTrue() {
        return THIS.FORM_CONTROL.VALUE.LAST_INDEX_OF(false) === -1 ? true : false;
    }

}
