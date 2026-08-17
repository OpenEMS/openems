import { Component, inject, OnInit, OnDestroy, ChangeDetectionStrategy } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { Service } from "src/app/shared/shared";

@Component({
    selector: FormlyFieldWaitingSpinnerComponent.SELECTOR,
    template: ' <ngx-spinner [name]="spinnerId"></ngx-spinner> ',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [NgxSpinnerModule],
})
export class FormlyFieldWaitingSpinnerComponent extends FieldWrapper implements OnInit, OnDestroy {
    public static readonly SELECTOR = "formly-field-waiting-spinner";
    protected readonly spinnerId = FormlyFieldWaitingSpinnerComponent.SELECTOR;
    private readonly service = inject(Service);

    ngOnInit() {
        this.service.startSpinner(this.spinnerId);
    }

    ngOnDestroy() {
        this.service.stopSpinner(this.spinnerId);
    }
}
