import { Component, Input } from "@angular/core";

/**
 * Shows a horizontal line on all but the last entry of a "flat-widget" or a "simple line"
 */
@Component({
    selector: 'oe-flat-widget-horizontal-line',
    templateUrl: './flat-widget-horizontal-line.html'
})
export class FlatWidgetHorizontalLineComponent {
    /** Components-Array to iterate over */
    @Input() components: any[] | null = null;
    /** index is an iterator */
    @Input() index: number | null = null;
}