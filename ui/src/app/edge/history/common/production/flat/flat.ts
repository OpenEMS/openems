import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { ChannelAddress, EdgeConfig, Utils } from "../../../../../shared/shared";
import { ArrayUtils } from "../../../../../shared/utils/array/array.utils";

@Component({
    selector: "productionWidget",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    public productionMeterComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;

    protected override getChannelAddresses(): ChannelAddress[] {
        //  Get Chargers
        this.chargerComponents = ArrayUtils.sortedAlphabetically(
            this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled),
            (c) => c.alias);

        // Get productionMeters
        this.productionMeterComponents = ArrayUtils.sortedAlphabetically(
            this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && this.config.isProducer(component)),
            (c) => c.alias);
        return [];
    }
}
