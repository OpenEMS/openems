import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";
import { AbstractModalLine } from "../abstract-modal-line";

@Component({
    selector: "oe-modal-buttons",
    templateUrl: "./modal-button.html",
    standalone: false,
})
export class ModalButtonsComponent extends AbstractModalLine {

    @Input({ required: true }) public buttons!: ButtonLabel[];

    /** ControlName for interactive Button */
    @Input({ required: true }) protected control:
        { type: "RADIO" } |
        { type: "SELECT" } = { type: "SELECT" };
}

export type ButtonLabel = {
    /** Name of Label, displayed below the icon */
    name: string;
    value: string | number | boolean;
    /** Icons for Button, displayed above the corresponding name */
    icon?: Icon;
    callback?: () => void;
    style?: Exclude<Partial<CSSStyleDeclaration>, "objectFit" | "width" | "height" | "src">,
    disabled?: boolean;
};
