import { Component, Input } from "@angular/core";
import { FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { v4 as uuidv4 } from "uuid";
import { CommonUiModule } from "../../common-ui.module";

@Component({
    selector: "oe-time-line",
    standalone: true,
    templateUrl: "./picktime.component.html",
    imports: [
        FormsModule,
        ReactiveFormsModule,
        CommonUiModule,
        IonicModule,
    ],
    styles: [
        `
            .datetime-button {
                &::part(native) {
                   background-color: var(--ion-color-toolbar-primary);
                   }

                &::part(content) {
                    padding: 0 !important;
                }
            }

            .picker-opts {
                --background: none;
            }
        `,
    ],
})
export class TimeLineComponent {

    @Input({ required: true }) public formGroup!: FormGroup;

    @Input({ required: true }) public controlName!: string;

    @Input() public name!: string;
    protected readonly spinnerId: string = uuidv4();

    public onTimeChange(value: string | null): void {
        if (value == null) {
            return;
        }
        this.formGroup.get(this.controlName)?.setValue(value);
        this.formGroup.get(this.controlName)?.markAsDirty();
    }
}
