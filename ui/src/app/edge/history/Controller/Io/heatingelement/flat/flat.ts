import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { EdgeConfig } from "src/app/shared/shared";

@Component({
    selector: "controller-io-heatingelement-widget",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = THIS.CONVERTER.FORMAT_SECONDS_TO_DURATION(THIS.TRANSLATE.CURRENT_LANG);
    protected consumptionMeter: EDGE_CONFIG.COMPONENT =  new EDGE_CONFIG.COMPONENT();

    protected override afterIsInitialized(): void {
        THIS.CONSUMPTION_METER = THIS.CONFIG?.getComponent(THIS.COMPONENT.PROPERTIES["METER.ID"]);
    }
}
