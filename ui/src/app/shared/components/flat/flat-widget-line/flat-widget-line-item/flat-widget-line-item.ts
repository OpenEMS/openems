import { Component, input } from "@angular/core";
import { AbstractFlatWidgetLine } from "../../abstract-flat-widget-line";

@Component({
    /** If multiple items in line use this */
    selector: "oe-flat-widget-line-item",
    templateUrl: "./flat-widget-line-item.html",
    standalone: false,
})
export class FlatWidgetLineItemComponent extends AbstractFlatWidgetLine {
    /** Width of left Column, right Column is (100 - width of left Column) */
    public leftColumnWidth = input<number | undefined>();
}
