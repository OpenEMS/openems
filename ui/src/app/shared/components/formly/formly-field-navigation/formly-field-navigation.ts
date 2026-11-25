import { AfterViewChecked, ChangeDetectionStrategy, Component, inject, signal } from "@angular/core";
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
            .header-row-with-icon {
                position: relative;
            }
            .custom-title {
                color: var(--ion-text-color);
                font-size: 1.25rem;
                font-weight: 500;
                margin-inline: auto;
            }
            .custom-title-with-icon {
                max-width: 83.333%;
            }
            .header-favorite-col {
                align-items: center;
                bottom: 0;
                display: flex;
                flex: 0 0 auto;
                max-width: none;
                position: absolute;
                right: 0;
                top: 0;
                width: auto;
            }
        `,
    ],
})
export class FormlyFieldNavigationComponent extends FieldWrapper implements AfterViewChecked {
    protected service: Service = inject(Service);
    protected router: Router = inject(Router);

    private readonly isInit = signal<boolean>(false);

    /**
     * Defers rendering of the navigation view until after the first change detection cycle.
     *
     * Without this, fields hidden via {@link OeFormlyField} briefly appear and disappear on page load.
     */
    ngAfterViewChecked() {
        this.isInit.set(true);
    }

    protected onSubmit(): void {
        this.field!.props!.onSubmit(this.form);
    }

    protected setForm(formGroup: FormGroup) {
        this.field!.props!.onSubmit(this.form);
    }

    protected async navigateTo(componentId: string): Promise<void> {
        const edge = await this.service.getCurrentEdge();
        await this.router.navigate(["/device", edge.id, "live", "evcs-cluster", "evcs", componentId]);
    }
}
