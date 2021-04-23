import { Component, Input } from "@angular/core";

@Component({
    selector: 'oe-flat-widget-horizontal-line',
    templateUrl: './flat-widget-horizontal-line.html'
})
export class FlatWidgetHorizontalLine {
    /** Components-Array to iterate over */
    @Input() storageComponents: any[];
    /** index is an iterator */
    @Input() index: number;
}