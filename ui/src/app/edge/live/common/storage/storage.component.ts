// @ts-strict-ignore
import { formatNumber } from "@angular/common";
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { DateUtils } from "src/app/shared/utils/date/dateutils";

import { StorageModalComponent } from "./modal/MODAL.COMPONENT";

@Component({
    selector: "storage",
    templateUrl: "./STORAGE.COMPONENT.HTML",
    standalone: false,
})
export class StorageComponent extends AbstractFlatWidget {

    public essComponents: EDGE_CONFIG.COMPONENT[] = [];
    public chargerComponents: EDGE_CONFIG.COMPONENT[] = [];
    public storageIconStyle: string | null = null;
    public isHybridEss: boolean[] = [];
    public emergencyReserveComponents: { [essId: string]: EDGE_CONFIG.COMPONENT } = {};
    public currentSoc: number[] = [];
    public isEmergencyReserveEnabled: boolean[] = [];
    protected possibleBatteryExtensionMessage: Map<string, { color: string, text: string }> = new Map();
    private prepareBatteryExtensionCtrl: { [key: string]: EDGE_CONFIG.COMPONENT };

    /**
    * Use 'convertChargePower' to convert/map a value
     *
    * @param value takes @Input value or channelAddress for chargePower
     * @returns value
    */
    public convertChargePower = (value: any): string => {
        return THIS.CONVERT_POWER(UTILS.MULTIPLY_SAFELY(value, -1), true);
    };

    /**
     * Use 'convertDischargePower' to convert/map a value
     *
     * @param value takes @Input value or channelAddress for dischargePower
     * @returns value
     */
    public convertDischargePower = (value: any): string => {
        return THIS.CONVERT_POWER(value);
    };

    /**
     * Use 'convertPower' to check whether 'charge/discharge' and to be only showed when not negative
     *
     * @param value takes passed value when called
     * @returns only positive and 0
     */
    public convertPower(value: number, isCharge?: boolean) {
        const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
        if (value == null) {
            return "-";
        }

        const thisValue: number = (value / 1000);

        // Round thisValue to Integer when decimal place equals 0
        if (thisValue > 0) {
            return formatNumber(thisValue, locale, "1.0-1") + " kW";

        } else if (thisValue == 0 && isCharge) {
            // if thisValue is 0, then show only when charge and not discharge
            return "0 kW";

        } else {
            return "-";
        }
    }

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: StorageModalComponent,
            componentProps: {
                edge: THIS.EDGE,
                component: THIS.COMPONENT,
            },
        });
        return await MODAL.PRESENT();
    }

    protected override getChannelAddresses() {

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress("_sum", "EssSoc"),

            // TODO should be moved to Modal
            new ChannelAddress("_sum", "EssActivePowerL1"),
            new ChannelAddress("_sum", "EssActivePowerL2"),
            new ChannelAddress("_sum", "EssActivePowerL3"),
        ];

        THIS.PREPARE_BATTERY_EXTENSION_CTRL = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.ESS.PREPARE_BATTERY_EXTENSION")
            .filter(component => COMPONENT.IS_ENABLED)
            .reduce((result, component) => {
                return {
                    ...result,
                    [COMPONENT.PROPERTIES["ESS.ID"]]: component,
                };
            }, {});


        for (const essId in THIS.PREPARE_BATTERY_EXTENSION_CTRL) {
            const controller = THIS.PREPARE_BATTERY_EXTENSION_CTRL[essId];
            CHANNEL_ADDRESSES.PUSH(
                new ChannelAddress(CONTROLLER.ID, "CtrlIsBlockingEss"),
                new ChannelAddress(CONTROLLER.ID, "CtrlIsChargingEss"),
                new ChannelAddress(CONTROLLER.ID, "CtrlIsDischargingEss"),
                new ChannelAddress(CONTROLLER.ID, "_PropertyIsRunning"),
                new ChannelAddress(CONTROLLER.ID, "_PropertyTargetTimeSpecified"),
                new ChannelAddress(CONTROLLER.ID, "_PropertyTargetTime"),
            );
        }

        // Get emergencyReserves
        THIS.EMERGENCY_RESERVE_COMPONENTS = THIS.CONFIG
            .getComponentsByFactory("CONTROLLER.ESS.EMERGENCY_CAPACITY_RESERVE")
            .filter(component => COMPONENT.IS_ENABLED)
            .reduce((result, component) => {
                return {
                    ...result,
                    [COMPONENT.PROPERTIES["ESS.ID"]]: component,
                };
            }, {});
        for (const component of OBJECT.VALUES(THIS.EMERGENCY_RESERVE_COMPONENTS)) {

            CHANNEL_ADDRESSES.PUSH(
                new ChannelAddress(COMPONENT.ID, "_PropertyReserveSoc"),
                new ChannelAddress(COMPONENT.ID, "_PropertyIsReserveSocEnabled"),
            );
        }
        // Get Chargers
        // TODO should be moved to Modal
        THIS.CHARGER_COMPONENTS = THIS.CONFIG
            .getComponentsImplementingNature("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER")
            .filter(component => COMPONENT.IS_ENABLED);
        for (const component of THIS.CHARGER_COMPONENTS) {
            CHANNEL_ADDRESSES.PUSH(
                new ChannelAddress(COMPONENT.ID, "ActualPower"),
            );
        }

        // Get ESSs
        THIS.ESS_COMPONENTS = THIS.CONFIG
            .getComponentsImplementingNature("IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS")
            .filter(component => COMPONENT.IS_ENABLED && !THIS.CONFIG
                .getNatureIdsByFactoryId(COMPONENT.FACTORY_ID)
                .includes("IO.OPENEMS.EDGE.ESS.API.META_ESS"));

        for (const component of THIS.CONFIG
            .getComponentsImplementingNature("IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS")
            .filter(component => COMPONENT.IS_ENABLED && !THIS.CONFIG
                .getNatureIdsByFactoryId(COMPONENT.FACTORY_ID)
                .includes("IO.OPENEMS.EDGE.ESS.API.META_ESS"))) {

            // Check if essComponent is HybridEss
            THIS.IS_HYBRID_ESS[COMPONENT.ID] = THIS.CONFIG
                .getNatureIdsByFactoryId(COMPONENT.FACTORY_ID)
                .includes("IO.OPENEMS.EDGE.ESS.API.HYBRID_ESS");

            CHANNEL_ADDRESSES.PUSH(
                new ChannelAddress(COMPONENT.ID, "Soc"),
                new ChannelAddress(COMPONENT.ID, "Capacity"),
            );
            if (THIS.CONFIG.FACTORIES[COMPONENT.FACTORY_ID].NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.ESS.API.ASYMMETRIC_ESS")) {
                CHANNEL_ADDRESSES.PUSH(
                    new ChannelAddress(COMPONENT.ID, "ActivePowerL1"),
                    new ChannelAddress(COMPONENT.ID, "ActivePowerL2"),
                    new ChannelAddress(COMPONENT.ID, "ActivePowerL3"),
                );
            }
        }
        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {

        for (const essId in THIS.PREPARE_BATTERY_EXTENSION_CTRL) {
            const controller = THIS.PREPARE_BATTERY_EXTENSION_CTRL[essId];

            THIS.POSSIBLE_BATTERY_EXTENSION_MESSAGE.SET(
                essId,
                THIS.GET_BATTERY_CAPACITY_EXTENSION_STATUS(
                    CURRENT_DATA.ALL_COMPONENTS[CONTROLLER.ID + "/_PropertyIsRunning"] == 1,
                    CURRENT_DATA.ALL_COMPONENTS[CONTROLLER.ID + "/CtrlIsBlockingEss"],
                    CURRENT_DATA.ALL_COMPONENTS[CONTROLLER.ID + "/CtrlIsChargingEss"],
                    CURRENT_DATA.ALL_COMPONENTS[CONTROLLER.ID + "/CtrlIsDischargingEss"],
                    CURRENT_DATA.ALL_COMPONENTS[CONTROLLER.ID + "/_PropertyTargetTimeSpecified"],
                    CURRENT_DATA.ALL_COMPONENTS[CONTROLLER.ID + "/_PropertyTargetTime"],
                ));
        }

        // Check total State_of_Charge for dynamical icon in widget-header
        const soc = CURRENT_DATA.ALL_COMPONENTS["_sum/EssSoc"];
        THIS.STORAGE_ICON_STYLE = "storage-" + UTILS.GET_STORAGE_SOC_SEGMENT(soc);

        for (const essId in THIS.EMERGENCY_RESERVE_COMPONENTS) {
            const controller = THIS.EMERGENCY_RESERVE_COMPONENTS[essId];
            controller["currentReserveSoc"] = CURRENT_DATA.ALL_COMPONENTS[CONTROLLER.ID + "/_PropertyReserveSoc"];
            THIS.IS_EMERGENCY_RESERVE_ENABLED[essId] = CURRENT_DATA.ALL_COMPONENTS[CONTROLLER.ID + "/_PropertyIsReserveSocEnabled"] == 1 ? true : false;
        }
    }

    private getBatteryCapacityExtensionStatus(isRunning: boolean, essIsBlocking: number, essIsCharging: number, essIsDischarging: number, targetTimeSpecified: boolean, targetDate: Date): { color: string, text: string } {

        if (!isRunning) {
            return null;
        }
        // Planned Expansion
        if (targetTimeSpecified && targetDate) {

            const date = DATE_UTILS.STRING_TO_DATE(TARGET_DATE.TO_STRING());
            return {
                color: "green", text: THIS.TRANSLATE.INSTANT("EDGE.INDEX.RETROFITTING.TARGET_TIME_SPECIFIED", {
                    targetDate: DATE_UTILS.TO_LOCALE_DATE_STRING(date),
                    targetTime: DATE.TO_LOCALE_TIME_STRING(),
                }),
            };
        }

        if (essIsBlocking != null && essIsBlocking == 1) {
            // If ess reached targetSoc
            return { color: "green", text: THIS.TRANSLATE.INSTANT("EDGE.INDEX.RETROFITTING.REACHED_TARGET_SOC") };

        } else if ((essIsCharging != null && essIsCharging == 1) || (essIsDischarging != null && essIsDischarging == 1)) {

            // If Ess is charging to or discharging to the targetSoc
            return { color: "orange", text: THIS.TRANSLATE.INSTANT("EDGE.INDEX.RETROFITTING.PREPARING") };
        } else {
            return null;
        }
    }

}
