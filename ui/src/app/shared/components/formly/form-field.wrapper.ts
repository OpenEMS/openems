import { ChangeDetectionStrategy, Component, ViewEncapsulation } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-wrapper-ion-form-field",
    templateUrl: "./form-FIELD.WRAPPER.HTML",
    changeDetection: CHANGE_DETECTION_STRATEGY.ON_PUSH,
    standalone: false,
    encapsulation: VIEW_ENCAPSULATION.NONE,
    styles: [`
            formly-field-ion-toggle, formly-field-ion-checkbox, formly-custom-select,
            formly-input-serial-number {
                width: 100%;
            }
    `],
})
export class FormlyWrapperFormFieldComponent extends FieldWrapper {

    get itemLines(): "none" | "inset" {
        return THIS.PROPS.DESCRIPTION ? "none" : "inset";
    }
}
