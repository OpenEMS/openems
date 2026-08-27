import { Component, signal } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { AbstractFlatWidgetLine } from "../abstract-flat-widget-line";

@Component({
    selector: "oe-flat-widget-percentagebar",
    templateUrl: "./flat-widget-percentagebar.html",
    imports: [CommonUiModule, PipeComponentsModule],
})
export class FlatWidgetPercentagebarComponent extends AbstractFlatWidgetLine {
    protected readonly percentageValue = signal<number | null>(null);

    protected override setValue(value: any) {
        this.percentageValue.set(value);
    }
}
