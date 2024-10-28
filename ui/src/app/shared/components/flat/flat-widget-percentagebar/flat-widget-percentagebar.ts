import { Component } from "@angular/core";
import { AbstractFlatWidgetLine } from "../abstract-flat-widget-line";

@Component({
    selector: "oe-flat-widget-percentagebar",
    templateUrl: "./flat-widget-percentagebar.html",
})
export class FlatWidgetPercentagebarComponent extends AbstractFlatWidgetLine {

    protected get displayPercent(): number | null {
        return this.displayValue === null
            ? null
            : Math.round(Number.parseFloat(this.displayValue));
    }
}
