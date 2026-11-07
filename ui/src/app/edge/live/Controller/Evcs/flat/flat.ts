import { ChangeDetectionStrategy, Component } from "@angular/core";
import { EvcsComponent } from "src/app/shared/components/edge/config-components/evcs/evcsComponent";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

import { ModalComponent } from "../modal/modal";
import { SharedEvcs } from "../shared/shared";

@Component({
    selector: "Controller_Evcs",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
    public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);

    protected controller: EdgeConfig.Component | null = null;
    protected evcsComponent: EdgeConfig.Component | null = null;
    protected isConnectionSuccessful: boolean = false;
    protected isEnergySinceBeginningAllowed: boolean = false;
    protected mode: string = "-";
    protected isChargingEnabled: boolean = false;
    protected defaultChargeMinPower: number = 0;
    protected prioritization: string = "-";
    protected phases: number = 0;
    protected maxChargingValue: number = 0;
    protected energySessionLimit: number = 0;
    protected state: string = "";
    protected minChargePower: number = 0;
    protected maxChargePower: number = 0;
    protected forceChargeMinPower: string = "-";
    protected chargeMode: SharedEvcs.ChargeMode | null = null;
    protected readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
    protected readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;
    protected readonly CONVERT_MANUAL_ON_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
    protected chargeTarget: string = "-";
    protected energySession: string = "-";
    protected chargeDischargePower: { name: string; value: number } = { name: "", value: 0 };
    protected propertyMode: DefaultTypes.ManualOnOff | null = null;
    protected status: string = "-";
    protected isReadWrite: boolean = false;
    protected modalComponent: Modal | null = null;

    private chargePoint: EvcsComponent | null = null;

    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }

    protected getModalComponent(): Modal {
        return {
            component: ModalComponent,
            componentProps: {
                component: this.component,
            },
        };
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        const component = this.component;
        const edge = this.edge;
        const config = this.config;
        if (component == null || edge == null || config == null) {
            return [];
        }

        const chargePoint = EvcsComponent.from(component, edge.getCurrentConfig(), edge);
        this.chargePoint = chargePoint;
        if (chargePoint == null) {
            return [];
        }

        const result = [
            chargePoint.powerChannel,
            new ChannelAddress(component.id, "Phases"),
            new ChannelAddress(component.id, "Plug"),
            new ChannelAddress(component.id, "Status"),
            new ChannelAddress(component.id, "State"),
            new ChannelAddress(component.id, "EnergySession"),
            // channels for modal component, subscribe here for better UX
            new ChannelAddress(component.id, "MinimumHardwarePower"),
            new ChannelAddress(component.id, "MaximumHardwarePower"),
            new ChannelAddress(component.id, "SetChargePowerLimit"),
        ];

        const controllers = config.getComponentsByFactory("Controller.Evcs");
        for (const controller of controllers) {
            const properties = controller.properties;
            if ("evcs.id" in properties && properties["evcs.id"] === this.componentId) {
                this.controller = controller;
                result.push(new ChannelAddress(controller.id, "_PropertyEnabledCharging"));
            }
        }
        return result;
    }

    protected override onCurrentData(currentData: CurrentData) {
        const component = this.component;
        const config = this.config;
        const chargePoint = this.chargePoint;
        if (component == null || config == null || chargePoint == null) {
            return;
        }

        this.evcsComponent = config.getComponentSafely(component.id);
        if (this.evcsComponent == null) {
            return;
        }
        this.isConnectionSuccessful = currentData.allComponents[component.id + "/State"] != 3;
        this.isReadWrite = component.hasPropertyValue<boolean>("readOnly", true) === false;

        // Check if Energy since beginning is allowed
        if (
            currentData.allComponents[chargePoint.powerChannel.toString()] > 0 ||
            currentData.allComponents[component.id + "/Status"] == 2 ||
            currentData.allComponents[component.id + "/Status"] == 7
        ) {
            this.isEnergySinceBeginningAllowed = true;
        }

        // Mode
        if (this.isChargingEnabled) {
            if (this.chargeMode == SharedEvcs.ChargeMode.FORCE_CHARGE) {
                this.mode = this.translate.instant("GENERAL.MANUALLY");
            } else if (this.chargeMode == SharedEvcs.ChargeMode.EXCESS_POWER) {
                this.mode = this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.SHORT_NAME");
            }
        }

        // Check if Controller is set
        if (this.controller) {
            // ChargeMode
            this.chargeMode = this.controller.properties["chargeMode"];
            // Check if Charging is enabled
            this.isChargingEnabled = currentData.allComponents[this.controller.id + "/_PropertyEnabledCharging"] === 1;
            // DefaultChargeMinPower
            this.defaultChargeMinPower = this.controller.properties["defaultChargeMinPower"];
            // Prioritization
            // TODO translation string should be explicit
            this.prioritization =
                this.controller.properties["priority"] in Prioritization
                    ? "EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.CHARGING_PRIORITY." +
                      this.controller.properties["priority"]
                    : "";
            // MaxChargingValue
            if (this.phases) {
                this.maxChargingValue = Utils.multiplySafely(
                    this.controller.properties["forceChargeMinPower"],
                    this.phases,
                );
            } else {
                this.maxChargingValue = Utils.multiplySafely(this.controller.properties["forceChargeMinPower"], 3);
            }
            // EnergySessionLimit
            this.energySessionLimit = this.controller.properties["energySessionLimit"];
        }

        this.status = this.getState(
            this.isChargingEnabled,
            currentData.allComponents[component.id + "/Status"],
            currentData.allComponents[component.id + "/Plug"],
        );
        // Phases
        this.phases = currentData.allComponents[this.componentId + "/Phases"];

        this.chargeDischargePower = Utils.convertChargeDischargePower(
            this.translate,
            currentData.allComponents[chargePoint.powerChannel.toString()],
        );
        this.chargeTarget = Utils.CONVERT_TO_WATT(
            this.formatNumber(currentData.allComponents[component.id + "/SetChargePowerLimit"]),
        );
        this.energySession = this.Converter.CONVERT_TO_WATTHOURS()(
            currentData.allComponents[component.id + "/EnergySession"],
        );

        this.minChargePower = this.formatNumber(currentData.allComponents[component.id + "/MinimumHardwarePower"]);
        this.maxChargePower = this.formatNumber(currentData.allComponents[component.id + "/MaximumHardwarePower"]);
        this.state = currentData.allComponents[component.id + "/Status"];
    }

    /**
     * Returns the state of the EVCS
     *
     * @param state The state
     * @param plug The plug
     */
    private getState(enabledCharging: boolean, state: number, plug: number): string {
        if (this.isReadWrite === true && enabledCharging === false) {
            return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGING_STATION_DEACTIVATED");
        }

        if (plug == null) {
            if (state == null) {
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
            }
        } else if (plug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
            return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CABLE_NOT_CONNECTED");
        }
        switch (state) {
            case SharedEvcs.ChargeState.STARTING:
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.STARTING");
            case SharedEvcs.ChargeState.UNDEFINED:
            case SharedEvcs.ChargeState.ERROR:
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.ERROR");
            case SharedEvcs.ChargeState.READY_FOR_CHARGING:
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.READY_FOR_CHARGING");
            case SharedEvcs.ChargeState.NOT_READY_FOR_CHARGING:
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.NOT_READY_FOR_CHARGING");
            case SharedEvcs.ChargeState.AUTHORIZATION_REJECTED:
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
            case SharedEvcs.ChargeState.CHARGING:
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGING");
            case SharedEvcs.ChargeState.ENERGY_LIMIT_REACHED:
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CHARGE_LIMIT_REACHED");
            case SharedEvcs.ChargeState.CHARGING_FINISHED:
                return this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.CAR_FULL");
        }
        return "";
    }

    private formatNumber(i: number) {
        const round = Math.ceil(i / 100) * 100;
        return round;
    }
}

enum ChargePlug {
    UNDEFINED = -1, //Undefined
    UNPLUGGED, //Unplugged
    PLUGGED_ON_EVCS, //Plugged on EVCS
    PLUGGED_ON_EVCS_AND_LOCKED = 3, //Plugged on EVCS and locked
    PLUGGED_ON_EVCS_AND_ON_EV = 5, //Plugged on EVCS and on EV
    PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7, //Plugged on EVCS and on EV and locked
}
enum Prioritization {
    CAR,
    STORAGE,
}
