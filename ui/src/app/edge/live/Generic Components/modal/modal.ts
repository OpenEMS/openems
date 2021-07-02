import { Component, Input } from "@angular/core";
import { AbstractModal } from "./abstractModal";

@Component({
    selector: 'oe-modal',
    templateUrl: 'modal.html',
})
export class ModalComponent extends AbstractModal {

    /** Title in Header */
    @Input() title: string;

}