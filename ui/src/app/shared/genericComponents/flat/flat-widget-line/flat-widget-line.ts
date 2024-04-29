// @ts-strict-ignore
import { Component, Input } from "@angular/core";

import { AbstractFlatWidgetLine } from "../abstract-flat-widget-line";

@Component({
    selector: 'oe-flat-widget-line',
    templateUrl: './flat-widget-line.html',
})
export class FlatWidgetLineComponent extends AbstractFlatWidgetLine {
    /** Name for parameter, displayed on the left side */
    @Input()
    public name: string;

    /** Width of left Column, right Column is (100 - width of left Column) */
    @Input()
    public leftColumnWidth: number;
}
