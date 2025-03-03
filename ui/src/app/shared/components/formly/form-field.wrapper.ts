import { ChangeDetectionStrategy, Component, ViewEncapsulation } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-wrapper-ion-form-field",
    templateUrl: "./form-field.wrapper.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
    encapsulation: ViewEncapsulation.None,
    styles: [`
            formly-field-ion-toggle, formly-field-ion-checkbox, formly-custom-select,
            formly-input-serial-number {
                width: 100%;
            }
    `],
})
export class FormlyWrapperFormFieldComponent extends FieldWrapper { }
