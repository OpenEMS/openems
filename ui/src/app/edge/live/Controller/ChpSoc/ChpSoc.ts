// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Icon } from "src/app/shared/type/widget";

import { ChannelAddress, CurrentData } from "../../../../shared/shared";
import { Controller_ChpSocModalComponent } from "./modal/MODAL.COMPONENT";

@Component({
    selector: "Controller_ChpSocComponent",
    templateUrl: "./CHP_SOC.HTML",
    standalone: false,
})
export class Controller_ChpSocComponent extends AbstractFlatWidget {

    private static PROPERTY_MODE: string = "_PropertyMode";
    public inputChannel: ChannelAddress | null = null;
    public outputChannel: ChannelAddress | null = null;
    public propertyModeChannel: ChannelAddress | null = null;
    public highThresholdValue: number;
    public lowThresholdValue: number;
    public state: string;
    public mode: string;
    public modeChannelValue: string;
    public inputChannelValue: number;
    public icon: Icon = {
        name: "",
        size: "large",
        color: "primary",
    };

    protected get thresholdDelta() {
        const delta = THIS.HIGH_THRESHOLD_VALUE - THIS.LOW_THRESHOLD_VALUE;
        return delta < 0 ? 0 : delta;
    }

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: Controller_ChpSocModalComponent,
            componentProps: {
                component: THIS.COMPONENT,
                edge: THIS.EDGE,
                outputChannel: THIS.OUTPUT_CHANNEL,
                inputChannel: THIS.INPUT_CHANNEL,
            },
        });
        return await MODAL.PRESENT();
    }

    protected override getChannelAddresses() {
        THIS.OUTPUT_CHANNEL = CHANNEL_ADDRESS.FROM_STRING(
            THIS.COMPONENT.PROPERTIES["outputChannelAddress"]);
        THIS.INPUT_CHANNEL = CHANNEL_ADDRESS.FROM_STRING(
            THIS.COMPONENT.PROPERTIES["inputChannelAddress"]);
        THIS.PROPERTY_MODE_CHANNEL = new ChannelAddress(THIS.COMPONENT.ID, Controller_ChpSocComponent.PROPERTY_MODE);
        return [
            THIS.OUTPUT_CHANNEL,
            THIS.INPUT_CHANNEL,
            THIS.PROPERTY_MODE_CHANNEL,
            new ChannelAddress(THIS.COMPONENT.ID, "_PropertyHighThreshold"),
            new ChannelAddress(THIS.COMPONENT.ID, "_PropertyLowThreshold"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {

        // Mode
        THIS.MODE_CHANNEL_VALUE = CURRENT_DATA.ALL_COMPONENTS[THIS.PROPERTY_MODE_CHANNEL.TO_STRING()];
        switch (THIS.MODE_CHANNEL_VALUE) {
            case "ON":
                THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.ON");
                break;
            case "OFF":
                THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.OFF");
                break;
            case "AUTOMATIC":
                THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.AUTOMATIC");
        }

        const outputChannelValue = CURRENT_DATA.ALL_COMPONENTS[THIS.OUTPUT_CHANNEL.TO_STRING()];

        switch (outputChannelValue) {
            case 0:
                THIS.STATE = THIS.TRANSLATE.INSTANT("GENERAL.INACTIVE");
                THIS.ICON.NAME == "help-outline";
                break;
            case 1:
                THIS.STATE = THIS.TRANSLATE.INSTANT("GENERAL.ACTIVE");
                break;
        }

        THIS.INPUT_CHANNEL_VALUE = CURRENT_DATA.ALL_COMPONENTS[THIS.INPUT_CHANNEL.TO_STRING()];
        THIS.HIGH_THRESHOLD_VALUE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/_PropertyHighThreshold"];
        THIS.LOW_THRESHOLD_VALUE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/_PropertyLowThreshold"];
    }

}
