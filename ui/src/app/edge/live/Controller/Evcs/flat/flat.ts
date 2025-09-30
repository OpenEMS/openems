// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

import { ModalComponent } from "../modal/modal";

type ChargeMode = "FORCE_CHARGE" | "EXCESS_POWER" | "OFF";


@Component({
  selector: "Controller_Evcs",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(THIS.TRANSLATE);

  protected controller: EDGE_CONFIG.COMPONENT;
  protected evcsComponent: EDGE_CONFIG.COMPONENT | null = null;
  protected isConnectionSuccessful: boolean = false;
  protected isEnergySinceBeginningAllowed: boolean = false;
  protected mode: string;
  protected isChargingEnabled: boolean = false;
  protected defaultChargeMinPower: number;
  protected prioritization: string;
  protected phases: number;
  protected maxChargingValue: number;
  protected energySessionLimit: number;
  protected state: string = "";
  protected minChargePower: number;
  protected maxChargePower: number;
  protected forceChargeMinPower: string;
  protected chargeMode: ChargeMode | null = null;
  protected readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
  protected readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;
  protected readonly CONVERT_MANUAL_ON_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(THIS.TRANSLATE);
  protected chargeTarget: string;
  protected energySession: string;
  protected chargeDischargePower: { name: string, value: number };
  protected propertyMode: DEFAULT_TYPES.MANUAL_ON_OFF | null = null;
  protected status: string;
  protected isReadWrite: boolean;

  formatNumber(i: number) {
    const round = MATH.CEIL(i / 100) * 100;
    return round;
  }


  async presentModal() {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: ModalComponent,
      componentProps: {
        component: THIS.COMPONENT,
      },
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    const result = [
      new ChannelAddress(THIS.COMPONENT.ID, "ChargePower"),
      new ChannelAddress(THIS.COMPONENT.ID, "Phases"),
      new ChannelAddress(THIS.COMPONENT.ID, "Plug"),
      new ChannelAddress(THIS.COMPONENT.ID, "Status"),
      new ChannelAddress(THIS.COMPONENT.ID, "State"),
      new ChannelAddress(THIS.COMPONENT.ID, "EnergySession"),
      // channels for modal component, subscribe here for better UX
      new ChannelAddress(THIS.COMPONENT.ID, "MinimumHardwarePower"),
      new ChannelAddress(THIS.COMPONENT.ID, "MaximumHardwarePower"),
      new ChannelAddress(THIS.COMPONENT.ID, "SetChargePowerLimit"),
    ];

    const controllers = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.EVCS");
    for (const controller of controllers) {
      const properties = CONTROLLER.PROPERTIES;
      if ("EVCS.ID" in properties && properties["EVCS.ID"] === THIS.COMPONENT_ID) {
        THIS.CONTROLLER = controller;
        RESULT.PUSH(new ChannelAddress(CONTROLLER.ID, "_PropertyEnabledCharging"));
      }
    }
    return result;
  }

  protected override onCurrentData(currentData: CurrentData) {

    THIS.EVCS_COMPONENT = THIS.CONFIG.GET_COMPONENT(THIS.COMPONENT.ID);
    THIS.IS_CONNECTION_SUCCESSFUL = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/State"] != 3 ? true : false;
    THIS.IS_READ_WRITE = THIS.COMPONENT.HAS_PROPERTY_VALUE<boolean>("readOnly", true) === false;
    THIS.STATUS = THIS.GET_STATE(THIS.CONTROLLER ? CURRENT_DATA.ALL_COMPONENTS[THIS.CONTROLLER.ID + "/_PropertyEnabledCharging"] === 1 : null, CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Status"], CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Plug"]);

    // Check if Energy since beginning is allowed
    if (CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/ChargePower"] > 0 || CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Status"] == 2 || CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Status"] == 7) {
      THIS.IS_ENERGY_SINCE_BEGINNING_ALLOWED = true;
    }

    // Mode
    if (THIS.IS_CHARGING_ENABLED) {
      if (THIS.CHARGE_MODE == "FORCE_CHARGE") {
        THIS.MODE = THIS.TRANSLATE.INSTANT("GENERAL.MANUALLY");
      } else if (THIS.CHARGE_MODE == "EXCESS_POWER") {
        THIS.MODE = THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.SHORT_NAME");
      }
    }

    // Check if Controller is set
    if (THIS.CONTROLLER) {

      // ChargeMode
      THIS.CHARGE_MODE = THIS.CONTROLLER.PROPERTIES["chargeMode"];
      // Check if Charging is enabled
      THIS.IS_CHARGING_ENABLED = CURRENT_DATA.ALL_COMPONENTS[THIS.CONTROLLER.ID + "/_PropertyEnabledCharging"] === 1 ? true : false;
      // DefaultChargeMinPower
      THIS.DEFAULT_CHARGE_MIN_POWER = THIS.CONTROLLER.PROPERTIES["defaultChargeMinPower"];
      // Prioritization
      THIS.PRIORITIZATION =
        THIS.CONTROLLER.PROPERTIES["priority"] in Prioritization
          ? "EDGE.INDEX.WIDGETS.EVCS.OPTIMIZED_CHARGE_MODE.CHARGING_PRIORITY." + THIS.CONTROLLER.PROPERTIES["priority"].toLowerCase()
          : "";
      // MaxChargingValue
      if (THIS.PHASES) {
        THIS.MAX_CHARGING_VALUE = UTILS.MULTIPLY_SAFELY(THIS.CONTROLLER.PROPERTIES["forceChargeMinPower"], THIS.PHASES);
      } else {
        THIS.MAX_CHARGING_VALUE = UTILS.MULTIPLY_SAFELY(THIS.CONTROLLER.PROPERTIES["forceChargeMinPower"], 3);
      }
      // EnergySessionLimit
      THIS.ENERGY_SESSION_LIMIT = THIS.CONTROLLER.PROPERTIES["energySessionLimit"];
    }

    // Phases
    THIS.PHASES = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT_ID + "/Phases"];

    THIS.CHARGE_DISCHARGE_POWER = UTILS.CONVERT_CHARGE_DISCHARGE_POWER(THIS.TRANSLATE, CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/ChargePower"]);
    THIS.CHARGE_TARGET = Utils.CONVERT_TO_WATT(THIS.FORMAT_NUMBER(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SetChargePowerLimit"]));
    THIS.ENERGY_SESSION = Utils.CONVERT_TO_WATT(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/EnergySession"]);

    THIS.MIN_CHARGE_POWER = THIS.FORMAT_NUMBER(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/MinimumHardwarePower"]);
    THIS.MAX_CHARGE_POWER = THIS.FORMAT_NUMBER(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/MaximumHardwarePower"]);
    THIS.STATE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Status"];
  }

  /**
 * Returns the state of the EVCS
 *
 * @param state the state
 * @param plug the plug
 */
  private getState(enabledCharging: boolean, state: number, plug: number): string {

    if (enabledCharging === false) {
      return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGING_STATION_DEACTIVATED");
    }

    if (plug == null) {
      if (state == null) {
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
      }
    } else if (plug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
      return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CABLE_NOT_CONNECTED");
    }
    switch (state) {
      case CHARGE_STATE.STARTING:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.STARTING");
      case CHARGE_STATE.UNDEFINED:
      case CHARGE_STATE.ERROR:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.ERROR");
      case ChargeState.READY_FOR_CHARGING:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.READY_FOR_CHARGING");
      case ChargeState.NOT_READY_FOR_CHARGING:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_READY_FOR_CHARGING");
      case ChargeState.AUTHORIZATION_REJECTED:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
      case CHARGE_STATE.CHARGING:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGING");
      case ChargeState.ENERGY_LIMIT_REACHED:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGE_LIMIT_REACHED");
      case ChargeState.CHARGING_FINISHED:
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CAR_FULL");
    }
  }

}

enum ChargeState {
  UNDEFINED = -1,           //Undefined
  STARTING,                 //Starting
  NOT_READY_FOR_CHARGING,   //Not ready for Charging E.G. unplugged, X1 or "ena" not enabled, RFID not enabled,...
  READY_FOR_CHARGING,       //Ready for Charging waiting for EV charging request
  CHARGING,                 //Charging
  ERROR,                    //Error
  AUTHORIZATION_REJECTED,   //Authorization rejected
  ENERGY_LIMIT_REACHED,     //Energy limit reached
  CHARGING_FINISHED,         //Charging has finished
}


enum ChargePlug {
  UNDEFINED = -1,                           //Undefined
  UNPLUGGED,                                //Unplugged
  PLUGGED_ON_EVCS,                          //Plugged on EVCS
  PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
  PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
  PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7,  //Plugged on EVCS and on EV and locked
}
enum Prioritization {
  CAR,
  STORAGE,
}
