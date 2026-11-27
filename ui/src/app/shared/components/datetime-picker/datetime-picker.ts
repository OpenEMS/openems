import { Component, computed, effect, Input, input, InputSignal, model, OnDestroy, OnInit } from "@angular/core";
import { FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonDatetime } from "@ionic/angular";
import { isPast } from "date-fns";
import { Subscription } from "rxjs";
import { CommonUiModule } from "../../common-ui.module";
import { DateTimeFormats, DateTimeUtils } from "../../utils/datetime/datetime-utils";
import { FormUtils } from "../../utils/form/form.utils";
import { ObjectUtils } from "../../utils/object/object-utils";

@Component({
    selector: "oe-date-time-line",
    templateUrl: "./datetime-picker.html",
    imports: [FormsModule, ReactiveFormsModule, CommonUiModule],
})
export class DateTimeLineComponent implements OnInit, OnDestroy {
    @Input() public formGroup!: FormGroup;
    @Input() public controlName!: string;
    public defaultLabel = input<string>();
    public label: InputSignal<(controlValue: string | number | null) => string> = input((controlValue) => "");

    /** Indicates whether only future values are allowed to be set */
    public onlyFutureValuesAllowed = input<boolean>(false);
    protected time = model<string | null>(null);
    private subscription: Subscription = new Subscription();

    /** Minimum date allowed for the datetime picker */
    private readonly min = computed<IonDatetime["min"] | null>(() => {
        if (!this.onlyFutureValuesAllowed() || !ObjectUtils.hasKeys(this.formGroup.controls, [this.controlName])) {
            return null;
        }

        return DateTimeUtils.format(new Date(), DateTimeFormats.YEAR_MONTH_DAY_TIME_WITH_SECONDS);
    });

    constructor() {
        effect(() => {
            const min = this.min();

            if (min == null) {
                return;
            }

            const value = this.formGroup.controls[this.controlName].value;
            const isInPast = value != null && isPast(new Date(value));

            if (isInPast) {
                this.formGroup.controls[this.controlName].setValue(
                    DateTimeUtils.format(new Date(), DateTimeFormats.YEAR_MONTH_DAY_TIME_WITH_SECONDS),
                );
            }
        });
    }

    setFormControlValue(event: CustomEvent<{ value: string }>) {
        const control = FormUtils.findFormControlSafely(this.formGroup, this.controlName);

        if (control == null) {
            return;
        }
        control.setValue(event.detail.value);
    }

    ngOnInit() {
        this.time.set(this.formGroup.controls[this.controlName].value);

        // Control needs to be rebuilt, otherwise field is not updated on init
        this.subscription.add(
            this.formGroup.controls[this.controlName].valueChanges.subscribe((value) => {
                this.time.set(value);
            }),
        );
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }
}
