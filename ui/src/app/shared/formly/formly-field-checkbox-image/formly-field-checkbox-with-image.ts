import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FieldWrapper } from '@ngx-formly/core';

@Component({
    selector: 'formly-field-checkbox-with-image',
    templateUrl: './formly-field-checkbox-with-image.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormlyFieldCheckboxWithImageComponent extends FieldWrapper implements OnInit {

    protected value: any;

    public ngOnInit() {
        // If the default value is not set in beginning.
        this.value = this.field.defaultValue;
    }

    /**
     * Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own
     */
    protected updateFormControl(event: CustomEvent) {
        this.value = event.detail.checked;
        this.formControl.setValue(this.value);
    }

}
