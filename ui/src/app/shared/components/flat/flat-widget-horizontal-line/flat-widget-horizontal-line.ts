import { Component, Input, ChangeDetectionStrategy } from "@angular/core";

/** Shows a horizontal line on all but the last entry of a "flat-widget" or a "simple line" */
@Component({
    selector: "oe-flat-widget-horizontal-line",
    templateUrl: "./flat-widget-horizontal-line.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class FlatWidgetHorizontalLineComponent {
    /** Components-Array to iterate over */
    @Input() protected components: any[] | null = null;
    /** Index is an iterator */
    @Input() protected index: number | null = null;
}
