import { Component, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { FieldWrapper } from '@ngx-formly/core';

@Component({
    selector: 'form-field-checkbox-hyperlink',
    templateUrl: './form-field-checkbox-hyperlink.wrapper.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormlyCheckBoxHyperlinkWrapperComponent extends FieldWrapper implements OnInit {

    protected secondLabel: string;

    public ngOnInit() {
        // If the default value is not set in beginning.
        if (!this.formControl.value) {
            this.formControl.setValue(this.field.props.defaultValue);
        }

        // Since its a custom wrapper, we are seperating label with checkbox.
        // mentioning required to true does not generate (*) to the label, so we are hard coding it.
        if (this.field.props.required) {
            this.secondLabel = this.field.props.description + '*';
        } else {
            this.secondLabel = this.field.props.description
        }
    }
}