import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";
import { AbstractModalLine } from "../modal-line/abstract-modal-line";

@Component({
    selector: 'oe-modal-buttons',
    templateUrl: './modal-button.html',
})
export class ModalButtons extends AbstractModalLine {
    /** Name for parameter, displayed on the left side*/

    @Input() labels: ButtonLabel;

    @Input() icons: Icon[];
}

export type ButtonLabel = {
    name: string;
    value: string;
}