// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig } from "../../../../../shared/shared";

@Component({
    selector: "consumptionWidget",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    protected evcsComponents: EdgeConfig.Component[] = [];
    protected heatComponents: EdgeConfig.Component[] = [];
    protected consumptionMeterComponents: EdgeConfig.Component[] = [];
    protected totalOtherEnergy: number;

    protected override getChannelAddresses(): ChannelAddress[] {
        const channels: ChannelAddress[] = [new ChannelAddress("_sum", "ConsumptionActiveEnergy")];

        this.evcsComponents = this.config?.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
            .filter(component => !(
                component.factoryId == "Evcs.Cluster" ||
                component.factoryId == "Evcs.Cluster.PeakShaving" ||
                component.factoryId == "Evcs.Cluster.SelfConsumption") && this.config?.hasComponentNature("io.openems.edge.evcs.api.DeprecatedEvcs", component.id));
        ;

        this.heatComponents = this.config?.getComponentsImplementingNature("io.openems.edge.heat.api.Heat")
            .filter(component =>
                !(component.factoryId === "Controller.Heat.Heatingelement") &&
                !component.isEnabled === false);
        channels.push(
            ...this.heatComponents.map(
                (component) => new ChannelAddress(component.id, "ActiveProductionEnergy")
            )
        );

        this.consumptionMeterComponents = this.config?.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter(component => {
                const natureIds = this.config?.getNatureIdsByFactoryId(component.factoryId);
                const isEvcs = natureIds.includes("io.openems.edge.evcs.api.Evcs");
                const isDeprecatedEvcs = natureIds.includes("io.openems.edge.evcs.api.DeprecatedEvcs");
                const isHeat = natureIds.includes("io.openems.edge.heat.api.Heat");

                return component.isEnabled && this.config?.isTypeConsumptionMetered(component) &&
                    (isEvcs === false || (isEvcs === true && isDeprecatedEvcs === false)) && isHeat === false;
            });

        return channels;
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.totalOtherEnergy = this.getTotalOtherEnergy(currentData);
    }

    /**
     * Gets the totalOtherEnergy
     *
     * @param currentData the current data
     * @returns the total other Energy
     */
    private getTotalOtherEnergy(currentData: CurrentData): number {

        let otherEnergy: number = 0;

        this.evcsComponents.forEach(component => {
            otherEnergy += currentData.allComponents[component.id + "/ActiveConsumptionEnergy"] ?? 0;
        });

        [...this.consumptionMeterComponents, ...this.heatComponents].forEach(component => {
            otherEnergy += currentData.allComponents[component.id + "/ActiveProductionEnergy"] ?? 0;
        });

        return currentData.allComponents["_sum/ConsumptionActiveEnergy"] - otherEnergy;

    }
}

