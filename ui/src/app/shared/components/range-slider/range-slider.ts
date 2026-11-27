import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule, IonRange } from "@ionic/angular";
import { Subscription } from "rxjs";
import { PipeModule } from "../../pipe/pipe.module";
import { FormUtils } from "../../utils/form/form.utils";
import { NumberUtils } from "../../utils/number/number-utils";
import { AbstractModalLine } from "../modal/abstract-modal-line";
import { OeFormlyField } from "../shared/oe-formly-component";

@Component({
    selector: "oe-range-slider",
    templateUrl: "./range-slider.html",
    standalone: true,
    imports: [IonicModule, PipeModule, ReactiveFormsModule],
    changeDetection: ChangeDetectionStrategy.Eager,
})
export class RangeSliderComponent extends AbstractModalLine {
    private static readonly DEFAULT_TICK_MAX: number = 100;
    protected properties = input.required<OeFormlyField.RangeLineProperties>();

    protected rangeMax: number = 100;
    private rangeMaxSubscription?: Subscription;

    override ngOnChanges(): void {
        super.ngOnChanges();
        this.bindRangeMax();
    }

    override ngOnDestroy(): void {
        this.rangeMaxSubscription?.unsubscribe();
        super.ngOnDestroy();
    }

    protected readonly DEFAULT_PIN_FORMATTER: IonRange["pinFormatter"] = (val: number) => val;

    private bindRangeMax(): void {
        this.rangeMaxSubscription?.unsubscribe();
        this.rangeMax = this.resolveRangeMax();

        const maxControlName = this.properties().tickMaxControlName;

        if (maxControlName == null || this.formGroup == null) {
            return;
        }

        const maxControl = FormUtils.findFormControlSafely(this.formGroup, maxControlName);
        if (maxControl == null) {
            return;
        }

        this.rangeMaxSubscription = maxControl.valueChanges.subscribe(() => {
            this.rangeMax = this.resolveRangeMax();
        });
    }

    private resolveRangeMax(): number {
        const maxControlName = this.properties().tickMaxControlName;
        const dynamicMax = maxControlName
            ? NumberUtils.parseNumberSafely(FormUtils.findFormControlsValueSafely(this.formGroup, maxControlName))
            : NaN;

        if (dynamicMax != null && Number.isFinite(dynamicMax)) {
            return dynamicMax;
        }

        return this.properties().tickMax ?? RangeSliderComponent.DEFAULT_TICK_MAX;
    }
}
