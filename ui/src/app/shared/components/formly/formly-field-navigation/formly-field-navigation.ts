import { Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "formly-field-navigation",
    templateUrl: "./formly-field-navigation.html",
    standalone: false,
})
export class FormlyFieldNavigationComponent extends FieldWrapper {

    constructor(
        protected service: Service,
    ) {
        super();
    }

    protected onSubmit(): void {
        this.field!.props!.onSubmit(this.form);
    }
}
