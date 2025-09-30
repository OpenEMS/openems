// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { WorkMode } from "src/app/shared/type/general";
import { ModalComponent } from "../modal/modal";
import { getInactiveIfPowerIsLow, getRunStateConverter, Level, State } from "../util/utils";


@Component({
    selector: "Controller_Io_HeatingElement",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    private static PROPERTY_MODE: string = "_PropertyMode";
    protected readonly CONVERT_HEATING_ELEMENT_RUNSTATE = getRunStateConverter(THIS.TRANSLATE);

    protected mode: string;
    protected runState: State;
    protected workMode: WorkMode;
    protected level: Level;
    protected readonly WorkMode = WorkMode;
    protected readonly Level = Level;
    protected readonly State = State;
    protected readonly CONVERT_SECONDS_TO_DATE_FORMAT = Utils.CONVERT_SECONDS_TO_DATE_FORMAT;
    protected outputChannelArray: ChannelAddress[] = [];
    protected consumptionMeter: EDGE_CONFIG.COMPONENT = null;

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: ModalComponent,
            componentProps: {
                component: THIS.COMPONENT,
            },
        });
        return await MODAL.PRESENT();
    }

    protected override getChannelAddresses() {

        THIS.OUTPUT_CHANNEL_ARRAY.PUSH(
            CHANNEL_ADDRESS.FROM_STRING(
                THIS.COMPONENT.PROPERTIES["outputChannelPhaseL1"]),
            CHANNEL_ADDRESS.FROM_STRING(
                THIS.COMPONENT.PROPERTIES["outputChannelPhaseL2"]),
            CHANNEL_ADDRESS.FROM_STRING(
                THIS.COMPONENT.PROPERTIES["outputChannelPhaseL3"]),
        );

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(THIS.COMPONENT.ID, "ForceStartAtSecondsOfDay"),
            ...THIS.OUTPUT_CHANNEL_ARRAY,
            new ChannelAddress(THIS.COMPONENT.ID, "Level"),
            new ChannelAddress(THIS.COMPONENT.ID, "Status"),
            new ChannelAddress(THIS.COMPONENT.ID, FlatComponent.PROPERTY_MODE),
            new ChannelAddress(THIS.COMPONENT.ID, "_PropertyWorkMode"),

        ];

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {

        THIS.WORK_MODE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + "_PropertyWorkMode"];
        THIS.CONSUMPTION_METER = THIS.CONFIG.GET_COMPONENT(THIS.COMPONENT.PROPERTIES["METER.ID"]) ?? null;

        // get current mode
        switch (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + FlatComponent.PROPERTY_MODE]) {
            case "MANUAL_ON": {
                THIS.MODE = "GENERAL.ON";
                break;
            }
            case "MANUAL_OFF": {
                THIS.MODE = "GENERAL.OFF";
                break;
            }
            case "AUTOMATIC": {
                THIS.MODE = "GENERAL.AUTOMATIC";
                break;
            }
        }

        THIS.LEVEL = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + "Level"];

        if (THIS.EDGE.IS_VERSION_AT_LEAST("2022.8")) {
            THIS.RUN_STATE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + "Status"];

            if (THIS.CONSUMPTION_METER) {
                const activePower = CURRENT_DATA.ALL_COMPONENTS[THIS.CONSUMPTION_METER.ID + "/ActivePower"];
                THIS.RUN_STATE = getInactiveIfPowerIsLow(THIS.RUN_STATE, activePower);
            }
        }
    }
}
