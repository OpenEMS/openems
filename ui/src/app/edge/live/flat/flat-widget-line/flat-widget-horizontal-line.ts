import { Component, Input } from "@angular/core";

@Component({
    selector: 'oe-flat-widget-horizontal-line',
    templateUrl: './flat-widget-horizontal-line.html'
})
export class FlatWidgetHorizontalLine {
    /** components */
    @Input() storageComponents: any[];
    @Input() index: number;
}