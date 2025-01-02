import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";

@Component({
    selector: "formly-wrapper-ion-form-field",
    templateUrl: "./form-field.wrapper.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormlyWrapperFormFieldComponent extends FieldWrapper { }
