import { Component, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { FieldType, FieldTypeConfig, FormlyFieldConfig } from "@ngx-formly/core";
import { FormlyFieldProps } from "@ngx-formly/ionic/form-field";

@Component({
    selector: "formly-accordion",
    templateUrl: "./formly-accordion.component.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class FormlyAccordionComponent extends FieldType<FieldTypeConfig<FormlyFieldProps>> implements OnInit {
    protected accordionFields: FormlyFieldConfig[] = [];

    public ngOnInit(): void {
        this.accordionFields = this.field.fieldGroup || [];
    }
}
