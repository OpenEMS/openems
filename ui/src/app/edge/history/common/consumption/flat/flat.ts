// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig } from "../../../../../shared/shared";

@Component({
    selector: "consumptionWidget",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    protected evcsComponents: EDGE_CONFIG.COMPONENT[] = [];
    protected heatComponents: EDGE_CONFIG.COMPONENT[] = [];
    protected consumptionMeterComponents: EDGE_CONFIG.COMPONENT[] = [];
    protected totalOtherEnergy: number;

    protected override getChannelAddresses(): ChannelAddress[] {
        const channels: ChannelAddress[] = [new ChannelAddress("_sum", "ConsumptionActiveEnergy")];

        THIS.EVCS_COMPONENTS = THIS.CONFIG?.getComponentsImplementingNature("IO.OPENEMS.EDGE.EVCS.API.EVCS")
            .filter(component => !(
                COMPONENT.FACTORY_ID == "EVCS.CLUSTER" ||
                COMPONENT.FACTORY_ID == "EVCS.CLUSTER.PEAK_SHAVING" ||
                COMPONENT.FACTORY_ID == "EVCS.CLUSTER.SELF_CONSUMPTION") && THIS.CONFIG?.hasComponentNature("IO.OPENEMS.EDGE.EVCS.API.DEPRECATED_EVCS", COMPONENT.ID));
        ;

        THIS.HEAT_COMPONENTS = THIS.CONFIG?.getComponentsImplementingNature("IO.OPENEMS.EDGE.HEAT.API.HEAT")
            .filter(component =>
                !(COMPONENT.FACTORY_ID === "CONTROLLER.HEAT.HEATINGELEMENT") &&
                !COMPONENT.IS_ENABLED === false);
        CHANNELS.PUSH(
            ...THIS.HEAT_COMPONENTS.MAP(
                (component) => new ChannelAddress(COMPONENT.ID, "ActiveProductionEnergy")
            )
        );

        THIS.CONSUMPTION_METER_COMPONENTS = THIS.CONFIG?.getComponentsImplementingNature("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
            .filter(component => {
                const natureIds = THIS.CONFIG?.getNatureIdsByFactoryId(COMPONENT.FACTORY_ID);
                const isEvcs = NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.EVCS.API.EVCS");
                const isDeprecatedEvcs = NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.EVCS.API.DEPRECATED_EVCS");
                const isHeat = NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.HEAT.API.HEAT");

                return COMPONENT.IS_ENABLED && THIS.CONFIG?.isTypeConsumptionMetered(component) &&
                    (isEvcs === false || (isEvcs === true && isDeprecatedEvcs === false)) && isHeat === false;
            });

        return channels;
    }

    protected override onCurrentData(currentData: CurrentData): void {
        THIS.TOTAL_OTHER_ENERGY = THIS.GET_TOTAL_OTHER_ENERGY(currentData);
    }

    /**
     * Gets the totalOtherEnergy
     *
     * @param currentData the current data
     * @returns the total other Energy
     */
    private getTotalOtherEnergy(currentData: CurrentData): number {

        let otherEnergy: number = 0;

        THIS.EVCS_COMPONENTS.FOR_EACH(component => {
            otherEnergy += CURRENT_DATA.ALL_COMPONENTS[COMPONENT.ID + "/ActiveConsumptionEnergy"] ?? 0;
        });

        [...THIS.CONSUMPTION_METER_COMPONENTS, ...THIS.HEAT_COMPONENTS].forEach(component => {
            otherEnergy += CURRENT_DATA.ALL_COMPONENTS[COMPONENT.ID + "/ActiveProductionEnergy"] ?? 0;
        });

        return CURRENT_DATA.ALL_COMPONENTS["_sum/ConsumptionActiveEnergy"] - otherEnergy;

    }
}

