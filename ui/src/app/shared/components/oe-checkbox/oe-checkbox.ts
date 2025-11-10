import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonCheckbox } from "@ionic/angular";
import { CommonUiModule } from "../../common-ui.module";
/**
 * Checkbox component for labels with embedded links
 *
 * Using custom checkbox due to hitbox being too big on default ion-checkbox
 */
@Component({
    selector: "oe-checkbox",
    templateUrl: "./oe-checkbox.html",
    standalone: true,
    imports: [
        FormsModule,
        ReactiveFormsModule,
        CommonUiModule,
    ],
})
export class OeCheckboxComponent {

    @Input({ required: true }) public attributes: Partial<Pick<IonCheckbox, "labelPlacement"> & Pick<HTMLElement, "slot"> & { text: string }> | null = null;
    @Input({ required: true }) public form: { formGroup: FormGroup, controlName: FormControl } | null = null;
    @Output() public setFormGroup: EventEmitter<FormGroup> = new EventEmitter();

    protected onChange() {
        if (this?.form == null) {
            return;
        }

        this.setFormGroup.emit(this.form.formGroup);
    }
}
