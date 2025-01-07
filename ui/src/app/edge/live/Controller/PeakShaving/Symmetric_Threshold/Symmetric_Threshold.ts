// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { ChannelAddress, CurrentData, Utils } from "../../../../../shared/shared";
import { Controller_Symmetric_Threshold_PeakShavingModalComponent } from "./modal/modal.component";

@Component({
    selector: "Controller_Symmetric_Threshold_PeakShaving",
    templateUrl: "./Symmetric_Threshold.html",
    standalone: false,
})
export class Controller_Symmetric_Threshold_PeakShavingComponent extends AbstractFlatWidget {

    public activePower: number;
    public propertyPeakShavingPower: number;
    public propertyRechargePower: number;
    public propertyPeakShavingThresholdPower: number;

    public peakShavingTargetPower: number;
    public peakShavingPower: number;
    public gridPowerWithoutPeakShaving: number;
    public peakShavingStateMachine: number;
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_Symmetric_Threshold_PeakShavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
            },
        });
        modal.onDidDismiss().then(() => {
            this.service.getConfig().then(config => {
                this.component = config.components[this.componentId];
            });
        });
        return await modal.present();
    }

    protected override getChannelAddresses() {
        return [
            new ChannelAddress(this.component.properties["meter.id"], "ActivePower"),
            new ChannelAddress(this.componentId, "_PropertyPeakShavingPower"),
            new ChannelAddress(this.componentId, "_PropertyRechargePower"),
            new ChannelAddress(this.componentId, "_PropertyPeakShavingThresholdPower"),
            new ChannelAddress(this.componentId, "PeakShavingPower"),
            new ChannelAddress(this.componentId, "PeakShavingTargetPower"),
            new ChannelAddress(this.componentId, "GridPowerWithoutPeakShaving"),
            new ChannelAddress(this.componentId, "PeakShavingStateMachine"),
        ];
    }
    protected override onCurrentData(currentData: CurrentData) {

        // activePower is 0 for negative Values
        this.activePower = currentData.allComponents[this.component.properties["meter.id"] + "/ActivePower"] >= 0
            ? currentData.allComponents[this.component.properties["meter.id"] + "/ActivePower"] : 0;
        this.propertyPeakShavingPower = this.component.properties["peakShavingPower"];
        this.propertyRechargePower = this.component.properties["rechargePower"];
        this.propertyPeakShavingThresholdPower = this.component.properties["peakShavingThresholdPower"];

        this.peakShavingPower = currentData.allComponents[this.componentId + "/PeakShavingPower"] > 0 ? currentData.allComponents[this.componentId + "/PeakShavingPower"] : 0;
        this.peakShavingTargetPower = currentData.allComponents[this.componentId + "/PeakShavingTargetPower"] > 0 ? currentData.allComponents[this.componentId + "/PeakShavingTargetPower"] : 0;
        this.gridPowerWithoutPeakShaving = currentData.allComponents[this.componentId + "/GridPowerWithoutPeakShaving"] >= 0 ? currentData.allComponents[this.componentId + "/GridPowerWithoutPeakShaving"] : 0;
        this.peakShavingStateMachine = currentData.allComponents[this.componentId + "/PeakShavingStateMachine"];
    }

}
