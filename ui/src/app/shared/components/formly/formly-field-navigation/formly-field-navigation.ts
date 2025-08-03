import { Component, inject } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "formly-field-navigation",
    templateUrl: "./formly-field-navigation.html",
    standalone: false,
})
export class FormlyFieldNavigationComponent extends FieldWrapper {
    protected service = inject(Service);

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);


    constructor() {
        super();
    }

    protected onSubmit(): void {
        this.field!.props!.onSubmit(this.form);
    }
}
