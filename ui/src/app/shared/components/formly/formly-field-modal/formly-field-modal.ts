import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from "@angular/core";
import { FieldWrapper } from "@ngx-formly/core";
import { Subject, takeUntil } from "rxjs";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "formly-field-modal",
    templateUrl: "./formly-field-modal.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormlyFieldModalComponent extends FieldWrapper implements OnInit, OnDestroy {

    private destroy$ = new Subject<void>();

    constructor(
        protected service: Service,
        protected cdRef: ChangeDetectorRef,
    ) {
        super();
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }

    ngOnInit() {
        this.form.valueChanges
            .pipe(takeUntil(this.destroy$))
            .subscribe((f) => {
                this.cdRef.markForCheck();
            });
    }

    protected onSubmit(): void {
        this.field!.props!.onSubmit(this.form);
    }
}
