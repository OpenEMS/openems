
import { Component, Input } from "@angular/core";
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IonCheckbox, IonicModule } from "@ionic/angular";
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
    IonicModule,
    FormsModule,
    ReactiveFormsModule
],
})
export class OeCheckboxComponent {

    @Input({ required: true }) public attributes: Partial<Pick<IonCheckbox, "labelPlacement"> & Pick<HTMLElement, "slot"> & { text: string }> | null = null;
    @Input({ required: true }) public form: { formGroup: FormGroup, controlName: FormControl } | null = null;
}
