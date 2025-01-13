import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-wrapper-ion-form-field",
    templateUrl: "./form-field.wrapper.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
    styles: [`
    :host {
        formly-field-ion-toggle, formly-field-ion-checkbox{
            width: 100%;
        }
    }
    `]
})
export class FormlyWrapperFormFieldComponent extends FieldWrapper { }
