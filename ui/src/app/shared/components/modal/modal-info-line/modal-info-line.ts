import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";

@Component({
    selector: "oe-modal-info-line",
    templateUrl: "./modal-info-line.html",
})
export class ModalInfoLineComponent {

    @Input({ required: true }) public info!: { text: string, lineStyle?: string }[] | string;

    /** Icon, displayed on the left side */
    @Input({ required: true }) protected icon!: Icon;

    /**
     *  Info-Text, displayed on the right side, optional style for all lines
     *  Multiple lines with own style is possible
     *  */

    @Input({ required: true }) protected lineStyle!: string;

    @Input({ required: true }) protected rowStyle!: string;
}
