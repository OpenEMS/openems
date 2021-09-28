import { Component, Input } from "@angular/core";
import { AbstractModal } from "./abstractModal";

@Component({
    selector: 'oe-modal',
    templateUrl: 'modal.html',
    styles: [`
        :host {
            height: 100%;
            margin-bottom: 15%;
            font-size: 0.9em;
        }
    `]
})
export class ModalComponent extends AbstractModal {

    /** Title in Header */
    @Input() title: string;

}