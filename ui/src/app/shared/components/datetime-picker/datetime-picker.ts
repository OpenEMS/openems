import { ChangeDetectionStrategy, Component, Input, input, InputSignal, model, OnDestroy, OnInit } from "@angular/core";
import { FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { Subscription } from "rxjs";
import { CommonUiModule } from "../../common-ui.module";
import { FormUtils } from "../../utils/form/form.utils";

@Component({
    selector: "oe-date-time-line",
    standalone: true,
    templateUrl: "./datetime-picker.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        FormsModule,
        ReactiveFormsModule,
        CommonUiModule,
    ],
})
export class DateTimeLineComponent implements OnInit, OnDestroy {

    @Input() public formGroup!: FormGroup;
    @Input() public controlName!: string;
    public defaultLabel = input<string>();
    public label: InputSignal<(controlValue: string | number | null) => string> = input((controlValue) => "");
    protected time = model<string | null>(null);
    private subscription: Subscription = new Subscription();

    setFormControlValue(event: CustomEvent<{ value: string }>) {
        const control = FormUtils.findFormControlSafely(this.formGroup, this.controlName);

        if (control == null) {
            return;
        }
        control.setValue(event.detail.value);
    }

    ionViewWillEnter() {
        this.ngOnInit();
    }

    ngOnInit() {

        this.time.set(this.formGroup.controls[this.controlName].value);

        // Control needs to be rebuilt, otherwise field is not updated on init
        this.subscription.add(this.formGroup.controls[this.controlName].valueChanges.subscribe(value => {
            this.time.set(value);
        }));
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }
}
