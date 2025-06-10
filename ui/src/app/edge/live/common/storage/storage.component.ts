// @ts-strict-ignore
import { formatNumber } from "@angular/common";
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { DateUtils } from "src/app/shared/utils/date/dateutils";

import { StorageModalComponent } from "./modal/modal.component";

@Component({
    selector: "storage",
    templateUrl: "./storage.component.html",
    standalone: false,
})
export class StorageComponent extends AbstractFlatWidget {

    public essComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public storageIconStyle: string | null = null;
    public isHybridEss: boolean[] = [];
    public emergencyReserveComponents: { [essId: string]: EdgeConfig.Component } = {};
    public currentSoc: number[] = [];
    public isEmergencyReserveEnabled: boolean[] = [];
    protected possibleBatteryExtensionMessage: Map<string, { color: string, text: string }> = new Map();
    private prepareBatteryExtensionCtrl: { [key: string]: EdgeConfig.Component };

    /**
    * Use 'convertChargePower' to convert/map a value
     *
    * @param value takes @Input value or channelAddress for chargePower
     * @returns value
    */
    public convertChargePower = (value: any): string => {
        return this.convertPower(Utils.multiplySafely(value, -1), true);
    };

    /**
     * Use 'convertDischargePower' to convert/map a value
     *
     * @param value takes @Input value or channelAddress for dischargePower
     * @returns value
     */
    public convertDischargePower = (value: any): string => {
        return this.convertPower(value);
    };

    /**
     * Use 'convertPower' to check whether 'charge/discharge' and to be only showed when not negative
     *
     * @param value takes passed value when called
     * @returns only positive and 0
     */
    public convertPower(value: number, isCharge?: boolean) {
        const locale: string = (Language.getByKey(localStorage.LANGUAGE) ?? Language.DEFAULT).i18nLocaleKey;
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
        const modal = await this.modalController.create({
            component: StorageModalComponent,
            componentProps: {
                edge: this.edge,
                component: this.component,
            },
        });
        return await modal.present();
    }

    protected override getChannelAddresses() {

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress("_sum", "EssSoc"),

            // TODO should be moved to Modal
            new ChannelAddress("_sum", "EssActivePowerL1"),
            new ChannelAddress("_sum", "EssActivePowerL2"),
            new ChannelAddress("_sum", "EssActivePowerL3"),
        ];

        this.prepareBatteryExtensionCtrl = this.config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension")
            .filter(component => component.isEnabled)
            .reduce((result, component) => {
                return {
                    ...result,
                    [component.properties["ess.id"]]: component,
                };
            }, {});


        for (const essId in this.prepareBatteryExtensionCtrl) {
            const controller = this.prepareBatteryExtensionCtrl[essId];
            channelAddresses.push(
                new ChannelAddress(controller.id, "CtrlIsBlockingEss"),
                new ChannelAddress(controller.id, "CtrlIsChargingEss"),
                new ChannelAddress(controller.id, "CtrlIsDischargingEss"),
                new ChannelAddress(controller.id, "_PropertyIsRunning"),
                new ChannelAddress(controller.id, "_PropertyTargetTimeSpecified"),
                new ChannelAddress(controller.id, "_PropertyTargetTime"),
            );
        }

        // Get emergencyReserves
        this.emergencyReserveComponents = this.config
            .getComponentsByFactory("Controller.Ess.EmergencyCapacityReserve")
            .filter(component => component.isEnabled)
            .reduce((result, component) => {
                return {
                    ...result,
                    [component.properties["ess.id"]]: component,
                };
            }, {});
        for (const component of Object.values(this.emergencyReserveComponents)) {

            channelAddresses.push(
                new ChannelAddress(component.id, "_PropertyReserveSoc"),
                new ChannelAddress(component.id, "_PropertyIsReserveSocEnabled"),
            );
        }
        // Get Chargers
        // TODO should be moved to Modal
        this.chargerComponents = this.config
            .getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
            .filter(component => component.isEnabled);
        for (const component of this.chargerComponents) {
            channelAddresses.push(
                new ChannelAddress(component.id, "ActualPower"),
            );
        }

        // Get ESSs
        this.essComponents = this.config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => component.isEnabled && !this.config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.MetaEss"));

        for (const component of this.config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => component.isEnabled && !this.config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.MetaEss"))) {

            // Check if essComponent is HybridEss
            this.isHybridEss[component.id] = this.config
                .getNatureIdsByFactoryId(component.factoryId)
                .includes("io.openems.edge.ess.api.HybridEss");

            channelAddresses.push(
                new ChannelAddress(component.id, "Soc"),
                new ChannelAddress(component.id, "Capacity"),
            );
            if (this.config.factories[component.factoryId].natureIds.includes("io.openems.edge.ess.api.AsymmetricEss")) {
                channelAddresses.push(
                    new ChannelAddress(component.id, "ActivePowerL1"),
                    new ChannelAddress(component.id, "ActivePowerL2"),
                    new ChannelAddress(component.id, "ActivePowerL3"),
                );
            }
        }
        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {

        for (const essId in this.prepareBatteryExtensionCtrl) {
            const controller = this.prepareBatteryExtensionCtrl[essId];

            this.possibleBatteryExtensionMessage.set(
                essId,
                this.getBatteryCapacityExtensionStatus(
                    currentData.allComponents[controller.id + "/_PropertyIsRunning"] == 1,
                    currentData.allComponents[controller.id + "/CtrlIsBlockingEss"],
                    currentData.allComponents[controller.id + "/CtrlIsChargingEss"],
                    currentData.allComponents[controller.id + "/CtrlIsDischargingEss"],
                    currentData.allComponents[controller.id + "/_PropertyTargetTimeSpecified"],
                    currentData.allComponents[controller.id + "/_PropertyTargetTime"],
                ));
        }

        // Check total State_of_Charge for dynamical icon in widget-header
        const soc = currentData.allComponents["_sum/EssSoc"];
        this.storageIconStyle = "storage-" + Utils.getStorageSocSegment(soc);

        for (const essId in this.emergencyReserveComponents) {
            const controller = this.emergencyReserveComponents[essId];
            controller["currentReserveSoc"] = currentData.allComponents[controller.id + "/_PropertyReserveSoc"];
            this.isEmergencyReserveEnabled[essId] = currentData.allComponents[controller.id + "/_PropertyIsReserveSocEnabled"] == 1 ? true : false;
        }
    }

    private getBatteryCapacityExtensionStatus(isRunning: boolean, essIsBlocking: number, essIsCharging: number, essIsDischarging: number, targetTimeSpecified: boolean, targetDate: Date): { color: string, text: string } {

        if (!isRunning) {
            return null;
        }
        // Planned Expansion
        if (targetTimeSpecified && targetDate) {

            const date = DateUtils.stringToDate(targetDate.toString());
            return {
                color: "green", text: this.translate.instant("Edge.Index.RETROFITTING.TARGET_TIME_SPECIFIED", {
                    targetDate: DateUtils.toLocaleDateString(date),
                    targetTime: date.toLocaleTimeString(),
                }),
            };
        }

        if (essIsBlocking != null && essIsBlocking == 1) {
            // If ess reached targetSoc
            return { color: "green", text: this.translate.instant("Edge.Index.RETROFITTING.REACHED_TARGET_SOC") };

        } else if ((essIsCharging != null && essIsCharging == 1) || (essIsDischarging != null && essIsDischarging == 1)) {

            // If Ess is charging to or discharging to the targetSoc
            return { color: "orange", text: this.translate.instant("Edge.Index.RETROFITTING.PREPARING") };
        } else {
            return null;
        }
    }

}
