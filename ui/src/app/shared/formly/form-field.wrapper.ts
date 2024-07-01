import { Component, ChangeDetectionStrategy } from '@angular/core';
import { FieldWrapper } from '@ngx-formly/core';

@Component({
    selector: 'formly-wrapper-ion-form-field',
    templateUrl: './form-field.wrapper.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormlyWrapperFormFieldComponent extends FieldWrapper { }
