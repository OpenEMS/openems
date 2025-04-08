import { Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "formly-field-modal",
    templateUrl: "./formly-field-modal.html",
    standalone: false,
})
export class FormlyFieldModalComponent extends FieldWrapper {

    constructor(
        protected service: Service,
    ) {
        super();
    }

    protected onSubmit(): void {
        this.field!.props!.onSubmit(this.form);
    }
}
