import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { CommonUiModule } from "../../common-ui.module";

@Component({
    selector: "oe-time-line",
    standalone: true,
    templateUrl: "./picktime.component.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        FormsModule,
        ReactiveFormsModule,
        CommonUiModule,
        IonicModule,
    ],
})
export class TimeLineComponent {

    @Input({ required: true }) public formGroup!: FormGroup;

    @Input({ required: true }) public controlName!: string;

    @Input() public name!: string;

    public onTimeChange(value: string | null): void {
        if (value == null) {
            return;
        }
        this.formGroup.get(this.controlName)?.patchValue(value, {
            emitEvent: false,
        });
    }
}
