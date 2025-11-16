import { Component } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "formly-field-navigation",
    templateUrl: "./formly-field-navigation.html",
    standalone: false,
    styles: [`
        ::ng-deep formly-form{
            height: 100% !important;
        }`,
    ],
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
