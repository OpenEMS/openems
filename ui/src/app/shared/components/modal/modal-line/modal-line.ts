import { Component, Input } from "@angular/core";
import { IonRange } from "@ionic/angular";
import { Converter } from "../../shared/converter";
import { AbstractModalLine } from "../abstract-modal-line";
import { ButtonLabel } from "../modal-button/modal-button";

@Component({
    selector: "oe-modal-line",
    templateUrl: "./modal-line.html",
    standalone: false,
})
export class ModalLineComponent extends AbstractModalLine {


    // Width of Left Column, Right Column is (100% - leftColumn)
    @Input({ required: true }) protected leftColumnWidth!: number;

    @Input() protected button: ButtonLabel | null = null;
    /** ControlName for interactive Button */
    @Input({ required: true }) protected control!:
        { type: "TOGGLE" } |
        { type: "INPUT", properties?: { unit: "W" } } |
        /* the available select options*/
        { type: "SELECT", options: { value: string, name: string }[] } |
        /* the properties for range slider*/
        { type: "RANGE", properties: { /* ticks*/ tickMin: number, tickMax: number, tickFormatter?: IonRange["pinFormatter"], unit: "H" | string, step?: number, pinFormatter: IonRange["pinFormatter"], label?: IonRange["label"], snaps?: boolean } } |
        { type: "TEXT", valueConverter?: Converter };

    /** Fixed indentation of the modal-line */
    @Input() protected textIndent: TextIndentation = TextIndentation.NONE;
    protected readonly DEFAULT_PIN_FORMATTER: IonRange["pinFormatter"] = (val: number) => val;
}

export enum TextIndentation {
    NONE = "0%",
    SINGLE = "5%",
    DOUBLE = "10%",
}
