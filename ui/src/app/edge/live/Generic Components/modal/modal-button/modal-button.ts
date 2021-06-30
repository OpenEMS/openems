import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";
import { AbstractModalLine } from "../modal-line/abstract-modal-line";

@Component({
    selector: 'oe-modal-buttons',
    templateUrl: './modal-button.html',
})
export class ModalButtons extends AbstractModalLine {

    /** Name of Label, displayed below the icon */
    @Input() labels: ButtonLabel;

    /** Icons for Button, displayed above the corresponding name */
    @Input() icons: Icon[];
}

export type ButtonLabel = {
    name: string;
    value: string;
}