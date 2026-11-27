import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { IonInput } from "@ionic/angular";
import { Converter } from "../../shared/converter";
import { AbstractModalLine } from "../abstract-modal-line";
import { ButtonLabel } from "../modal-button/modal-button";

@Component({
    selector: "oe-modal-line",
    templateUrl: "./modal-line.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ModalLineComponent extends AbstractModalLine {
    // Width of Left Column, Right Column is (100% - leftColumn)
    @Input({ required: true }) protected leftColumnWidth!: number;
    @Input({ required: true }) protected hideValue: boolean = false;

    @Input() protected button: ButtonLabel | null = null;
    /** ControlName for interactive Button */
    @Input({ required: true }) protected control!:
        | { type: "TOGGLE" }
        | { type: "INPUT"; properties?: { unit: "W"; type: IonInput["type"] } }
        /* the available select options*/
        | { type: "SELECT"; options: { value: string; name: string }[] }
        | { type: "TEXT"; valueConverter?: Converter }
        | { type: "BUTTON"; button: ButtonLabel };

    /** Fixed indentation of the modal-line */
    @Input() protected textIndent: TextIndentation = TextIndentation.NONE;

    /** Toggle */
    protected toggleOnEnter(event: KeyboardEvent, controlName: string) {
        const control = this.formGroup.get(controlName);
        if (control) {
            control.setValue(!control.value);
            event.preventDefault();
        }
    }

    /** Select */
    protected selectOnEnter(event: KeyboardEvent, controlName: string) {
        const control = this.formGroup.get(controlName);
        if (control) {
            control.setValue(!control.value);
            event.preventDefault();
        }
    }
}

export enum TextIndentation {
    NONE = "0%",
    SINGLE = "5%",
    DOUBLE = "10%",
}
