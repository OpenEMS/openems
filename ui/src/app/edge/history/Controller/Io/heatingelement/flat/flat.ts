import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { EdgeConfig } from "src/app/shared/shared";

@Component({
    selector: "controller-io-heatingelement-widget",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = this.Converter.FORMAT_SECONDS_TO_DURATION(this.translate.getCurrentLang());
    protected consumptionMeter: EdgeConfig.Component =  new EdgeConfig.Component();

    protected override afterIsInitialized(): void {
        this.consumptionMeter = this.config?.getComponent(this.component.properties["meter.id"]);
    }
}
