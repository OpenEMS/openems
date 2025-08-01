// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { WorkMode } from "src/app/shared/type/general";
import { ModalComponent } from "../modal/modal";
import { getInactiveIfPowerIsLow, getRunStateConverter, Level, State } from "../util/utils";


@Component({
    selector: "Controller_Io_HeatingElement",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    private static PROPERTY_MODE: string = "_PropertyMode";
    protected readonly CONVERT_HEATING_ELEMENT_RUNSTATE = getRunStateConverter(this.translate);

    protected mode: string;
    protected runState: State;
    protected workMode: WorkMode;
    protected level: Level;
    protected readonly WorkMode = WorkMode;
    protected readonly Level = Level;
    protected readonly State = State;
    protected readonly CONVERT_SECONDS_TO_DATE_FORMAT = Utils.CONVERT_SECONDS_TO_DATE_FORMAT;
    protected outputChannelArray: ChannelAddress[] = [];
    protected consumptionMeter: EdgeConfig.Component = null;

    async presentModal() {
        const modal = await this.modalController.create({
            component: ModalComponent,
            componentProps: {
                component: this.component,
            },
        });
        return await modal.present();
    }

    protected override getChannelAddresses() {

        this.outputChannelArray.push(
            ChannelAddress.fromString(
                this.component.properties["outputChannelPhaseL1"]),
            ChannelAddress.fromString(
                this.component.properties["outputChannelPhaseL2"]),
            ChannelAddress.fromString(
                this.component.properties["outputChannelPhaseL3"]),
        );

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, "ForceStartAtSecondsOfDay"),
            ...this.outputChannelArray,
            new ChannelAddress(this.component.id, "Level"),
            new ChannelAddress(this.component.id, "Status"),
            new ChannelAddress(this.component.id, FlatComponent.PROPERTY_MODE),
            new ChannelAddress(this.component.id, "_PropertyWorkMode"),

        ];

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {

        this.workMode = currentData.allComponents[this.component.id + "/" + "_PropertyWorkMode"];
        this.consumptionMeter = this.config.getComponent(this.component.properties["meter.id"]) ?? null;

        // get current mode
        switch (currentData.allComponents[this.component.id + "/" + FlatComponent.PROPERTY_MODE]) {
            case "MANUAL_ON": {
                this.mode = "General.on";
                break;
            }
            case "MANUAL_OFF": {
                this.mode = "General.off";
                break;
            }
            case "AUTOMATIC": {
                this.mode = "General.automatic";
                break;
            }
        }

        this.level = currentData.allComponents[this.component.id + "/" + "Level"];

        if (this.edge.isVersionAtLeast("2022.8")) {
            this.runState = currentData.allComponents[this.component.id + "/" + "Status"];

            if (this.consumptionMeter) {
                const activePower = currentData.allComponents[this.consumptionMeter.id + "/ActivePower"];
                this.runState = getInactiveIfPowerIsLow(this.runState, activePower);
            }
        }
    }
}
