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

            formly-field-ion-toggle ion-toggle {
                padding: 0 !important;
                margin: 0 !important;
                transform: none !important;
            }

            ion-toggle::part(label),
            ion-checkbox::part(label),
            ion-radio::part(label) {
                white-space: pre-wrap !important;
                flex: 1;
                margin-inline-end: 0 !important;
            }
    `],
})
export class FormlyWrapperFormFieldComponent extends FieldWrapper {

    get itemLines(): "none" | "inset" {
        return this.props.description ? "none" : "inset";
    }
}
