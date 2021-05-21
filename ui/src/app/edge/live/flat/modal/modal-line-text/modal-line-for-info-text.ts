import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";

/**
 * Shows the Info-Text.
 */
@Component({
    selector: 'oe-modal-line-text',
    templateUrl: './modal-line-for-info-text.html'
})
export class ModalLineText {

    @Input() icon: Icon;

    @Input() text: string;
}