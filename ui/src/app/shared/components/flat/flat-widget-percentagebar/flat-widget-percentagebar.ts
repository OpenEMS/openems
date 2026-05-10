import { Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { AbstractFlatWidgetLine } from "../abstract-flat-widget-line";

@Component({
    selector: "oe-flat-widget-percentagebar",
    templateUrl: "./flat-widget-percentagebar.html",
    standalone: true,
    imports: [
        CommonUiModule,
        PipeComponentsModule,
    ],
})
export class FlatWidgetPercentagebarComponent extends AbstractFlatWidgetLine {

    protected get displayPercent(): number | null {
        return this.displayValue === null
            ? null
            : Math.round(Number.parseFloat(this.displayValue));
    }
}
