import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Router } from "@angular/router";
import { FieldWrapper } from "@ngx-formly/core";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "formly-field-navigation",
    templateUrl: "./formly-field-navigation.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    styles: [
        `
            ::ng-deep formly-form {
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
    protected router: Router = inject(Router);

    protected onSubmit(): void {
        this.field!.props!.onSubmit(this.form);
    }

    protected setForm(formGroup: FormGroup) {
        this.field!.props!.onSubmit(this.form);
    }

    protected async navigateTo(componentId: string): Promise<void> {
        const edge = await this.service.getCurrentEdge();
        await this.router.navigate([
            "/device",
            edge.id,
            "live",
            "evcs-cluster",
            "evcs",
            componentId,
        ]);
    }
}
