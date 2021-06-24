import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";

/**
 * Shows the Info-Text.
 */
@Component({
    selector: 'oe-modal-line-note',
    templateUrl: './modal-line-note.html'
})
export class ModalLineNote {

    @Input() icon: Icon;

    @Input() text: string;
}