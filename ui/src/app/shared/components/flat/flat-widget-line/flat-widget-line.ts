import { Component, Input } from "@angular/core";

import { AbstractFlatWidgetLine } from "../abstract-flat-widget-line";

@Component({
    selector: "oe-flat-widget-line",
    templateUrl: "./flat-widget-line.html",
})
export class FlatWidgetLineComponent extends AbstractFlatWidgetLine {

    /** Width of left Column, right Column is (100 - width of left Column) */
    @Input()
    public leftColumnWidth?: number;
}
