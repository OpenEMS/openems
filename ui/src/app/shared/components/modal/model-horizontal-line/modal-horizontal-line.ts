import { Component, Input, ChangeDetectionStrategy } from "@angular/core";

/** Shows a Horizontal Line for every but the last component or a simple Line. */
@Component({
    selector: "oe-modal-horizontal-line",
    templateUrl: "./modal-horizontal-line.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ModalHorizontalLineComponent {
    /** Components-Array to iterate over */
    @Input({ required: true }) public components!: any[];
    /** Index is an iterator */
    @Input({ required: true }) public index!: number;
}
