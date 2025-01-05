import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";
import { AbstractModalLine } from "../abstract-modal-line";

@Component({
    selector: "oe-modal-buttons",
    templateUrl: "./modal-button.html",
    standalone: false,
})
export class ModalButtonsComponent extends AbstractModalLine {

    @Input({ required: true }) protected buttons!: ButtonLabel[];
}

export type ButtonLabel = {
    /** Name of Label, displayed below the icon */
    name: string;
    value: string;
    /** Icons for Button, displayed above the corresponding name */
    icons?: Icon;
    callback?: () => void;
    style?: { [key: string]: string };
};
