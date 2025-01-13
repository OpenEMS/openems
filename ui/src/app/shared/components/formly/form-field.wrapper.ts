import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-wrapper-ion-form-field",
    templateUrl: "./form-field.wrapper.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
    styles: [`
    :host {
        formly-field-ion-toggle {
            width: 100%;
        }

        formly-field-ion-select {

        }
    }
    `]
})
export class FormlyWrapperFormFieldComponent extends FieldWrapper { }
