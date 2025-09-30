import { Component } from "@angular/core";
import { AbstractFlatWidgetLine } from "../abstract-flat-widget-line";

@Component({
    selector: "oe-flat-widget-percentagebar",
    templateUrl: "./flat-widget-PERCENTAGEBAR.HTML",
    standalone: false,
})
export class FlatWidgetPercentagebarComponent extends AbstractFlatWidgetLine {

    protected get displayPercent(): number | null {
        return THIS.DISPLAY_VALUE === null
            ? null
            : MATH.ROUND(NUMBER.PARSE_FLOAT(THIS.DISPLAY_VALUE));
    }
}
