import { Component, Input } from "@angular/core";

/**
 * Shows a Horizontal Line for every but the last component or a simple Line.
 */
@Component({
    selector: 'oe-modal-horizontal-line',
    templateUrl: './modal-horizontal-line.html',
})
export class ModalHorizontalLineComponent {

    /** Components-Array to iterate over */
    @Input() protected components: any[];
    /** index is an iterator */
    @Input() protected index: number;
}
