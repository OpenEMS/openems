import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Icon } from "src/app/shared/type/widget";
import { Converter } from "../../shared/converter";
import { AbstractModalLine } from "../abstract-modal-line";
import { CurrentData } from "src/app/shared/shared";

@Component({
    selector: 'oe-modal-buttons',
    templateUrl: './modal-button.html'
})
export class ModalButtonsComponent extends AbstractModalLine {

    @Input() protected buttons: ButtonLabel[];
    @Input() formToBeBuildt: { controlName: string, channel: string }[] = null;
    @Input() formControlValue: Converter = Converter.TO_STRING;
    @Output() setFormGroup: EventEmitter<FormGroup> = new EventEmitter();

    protected override onCurrentData(currentData: CurrentData): void {

        this.formToBeBuildt.forEach(({ controlName, channel }) => {
            let value = currentData.allComponents[channel];
            if (value != null) {
                this.formGroup.registerControl(controlName, value);
                this.setFormGroup.emit(this.formGroup);
            }
        })
    }
}

export type ButtonLabel = {
    /** Name of Label, displayed below the icon */
    name: string;
    value: string;
    /** Icons for Button, displayed above the corresponding name */
    icons?: Icon;
    callback?: Function;
}