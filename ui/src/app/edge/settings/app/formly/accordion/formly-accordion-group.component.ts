import { Component, OnInit } from "@angular/core";
import { FieldType, FieldTypeConfig, FormlyFieldConfig } from "@ngx-formly/core";
import { FormlyFieldProps } from "@ngx-formly/ionic/form-field";

@Component({
    selector: "formly-accordion-group",
    templateUrl: "./formly-accordion-group.component.html",
    standalone: false,
})
export class FormlyAccordionGroupComponent extends FieldType<FieldTypeConfig<FormlyFieldProps & {
    isMulti?: boolean,
    missingOptionsText?: string,
    openAccordions?: string[],
}>> implements OnInit {

    protected accordions: FormlyFieldConfig[] = [];
    protected expandedAccordions: string[] | string | null = null;

    public ngOnInit(): void {
        this.accordions = this.field.fieldGroup || [];
        this.fillExpandedAccordions();
    }

    private fillExpandedAccordions(): void {

        if (this.props.openAccordions == null) {
            return;
        }
        const isMultiple = this.props.isMulti || false;
        this.expandedAccordions = this.getExistingAccordions(isMultiple);
    }

    private getExistingAccordions(isMulti: boolean): string | string[] | null {

        const accordions: string[] = [];
        this.props.openAccordions?.forEach(openAccordion => {
            const existingAccordion = this.accordions.find(accordion =>
                // customeId from IBN implementation
                accordion.props?.["accordionId"] === openAccordion ||
                // Fallback to App center's original key logic
                accordion.key === openAccordion ||
                // Fallback to Formly's auto-generated ID
                accordion.id === openAccordion
            );

            if (existingAccordion != null) {
                accordions.push(openAccordion);
            }
        });

        if (accordions.length === 0) {
            return isMulti ? [] : null;
        }

        return isMulti ? accordions : accordions[0];
    }
}
