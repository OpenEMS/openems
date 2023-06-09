import { Component, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { FieldWrapper } from '@ngx-formly/core';

@Component({
    selector: 'formly-field-radio-with-image',
    templateUrl: './formly-field-radio-with-image.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormlyFieldRadioWithImageComponent extends FieldWrapper implements OnInit {

    protected value: any;

    public ngOnInit() {
        this.value = this.field.defaultValue;
        if (this.formControl.getRawValue()) {
            this.value = this.formControl.getRawValue();
        }
    }

    /**
     * Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own
     */
    protected updateFormControl() {
        this.formControl.setValue(this.value);
    }
}