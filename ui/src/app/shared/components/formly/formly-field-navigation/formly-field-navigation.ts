import { Component, inject } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "formly-field-navigation",
    templateUrl: "./formly-field-navigation.html",
    standalone: false,
    styles: [`
        ::ng-deep formly-form{
            height: 100% !important;
        }
            .custom-title {
            color: var(--ion-text-color);
            font-size: 1.25rem;
            font-weight: 500;
        }
        `,
    ],
})
export class FormlyFieldNavigationComponent extends FieldWrapper {

    protected service: Service = inject(Service);

    protected onSubmit(): void {
        this.field!.props!.onSubmit(this.form);
    }
}
