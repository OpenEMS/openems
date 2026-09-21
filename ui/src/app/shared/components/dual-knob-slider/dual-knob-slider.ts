import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { IonicModule, IonRange } from "@ionic/angular";
import { RangeValue } from "@ionic/core";
import { takeUntil } from "rxjs";
import { PipeModule } from "../../pipe/pipe.module";
import { AbstractModalLine } from "../modal/abstract-modal-line";
import { OeFormlyField } from "../shared/oe-formly-component";

@Component({
    selector: "oe-dual-knob-slider",
    templateUrl: "./dual-knob-slider.html",
    standalone: true,
    imports: [IonicModule, PipeModule],
    changeDetection: ChangeDetectionStrategy.Eager,
})
export class DualKnobSliderComponent extends AbstractModalLine {
    protected lowerControlName = input<string | null>(null);
    protected upperControlName = input<string | null>(null);
    protected properties = input<(OeFormlyField.RangeLineProperties & { dualKnobs: true }) | null>(null);

    protected dualRangeValue: RangeValue = { lower: 0, upper: 0 };
    private isDualDragging: boolean = false;

    public override ngOnInit() {
        super.ngOnInit();

        this.syncDualRangeFromControls();

        this.formGroup?.valueChanges.pipe(takeUntil(this.stopOnDestroy)).subscribe(() => {
            if (!this.isDualDragging) {
                this.syncDualRangeFromControls();
            }
        });
    }

    protected readonly DEFAULT_PIN_FORMATTER: IonRange["pinFormatter"] = (val: number) => val;

    protected onDualRangeInput(event: CustomEvent<{ value: RangeValue }>) {
        const value = event.detail.value;
        if (typeof value === "number" || value == null) {
            return;
        }

        const lowerName = this.lowerControlName() ?? this.controlName;
        const upperName = this.upperControlName();
        if (!lowerName || !upperName) {
            return;
        }

        const lowerControl = this.formGroup.controls[lowerName];
        const upperControl = this.formGroup.controls[upperName];
        if (!lowerControl || !upperControl) {
            return;
        }

        this.isDualDragging = true;
        this.dualRangeValue = { lower: value.lower, upper: value.upper };

        lowerControl.setValue(value.lower);
        upperControl.setValue(value.upper);
        lowerControl.markAsDirty();
        upperControl.markAsDirty();
    }

    protected onDualRangeChange(event: CustomEvent<{ value: RangeValue }>) {
        this.onDualRangeInput(event);
        this.isDualDragging = false;
        this.syncDualRangeFromControls();
    }

    private syncDualRangeFromControls() {
        const lowerName = this.lowerControlName() ?? this.controlName;
        const upperName = this.upperControlName();

        if (!this.formGroup || !lowerName || !upperName) {
            return;
        }

        this.dualRangeValue = {
            lower: this.formGroup.controls[lowerName]?.value ?? 0,
            upper: this.formGroup.controls[upperName]?.value ?? 0,
        };
    }
}
