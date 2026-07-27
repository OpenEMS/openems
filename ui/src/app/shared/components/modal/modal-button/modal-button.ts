import { Component, Input, ChangeDetectionStrategy } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";
import { AbstractModalLine } from "../abstract-modal-line";

@Component({
    selector: "oe-modal-buttons",
    templateUrl: "./modal-button.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ModalButtonsComponent extends AbstractModalLine {
    /** ControlName for interactive Button */
    @Input({ required: true }) protected control: { type: "RADIO" } | { type: "SELECT" } = { type: "SELECT" };

    public _buttons!: ButtonLabel[];

    @Input({ required: true }) set buttons(value: ButtonLabel[]) {
        this._buttons = value.map((button) => ({
            ...button,
            callback: button.callback ?? (() => {}),
        }));
    }
}

export type ButtonLabel = {
    /** Name of Label, displayed below the icon */
    name: string;
    value: string | number | boolean;
    description?: string;
    /** Icons for Button, displayed above the corresponding name */
    icon?: Icon;
    callback?: () => void;
    style?: Exclude<Partial<CSSStyleDeclaration>, "objectFit" | "width" | "height" | "src">;
    disabled?: boolean;
};
