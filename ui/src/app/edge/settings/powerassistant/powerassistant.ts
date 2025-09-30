// @ts-strict-ignore
import { formatNumber } from "@angular/common";
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Language } from "src/app/shared/type/language";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "../../../shared/shared";
import { LiveDataService } from "../../live/livedataservice";

type Channel = {
  title: string,
  address: string,
  converter: (value: any) => string,
  value?: any,
};

type OpenemsComponent = {
  id: string,
  alias: string,
  factoryId: string,
  channels: Channel[],
};

type Entry = {
  ess: OpenemsComponent,
  battery?: OpenemsComponent,
  batteryInverter?: OpenemsComponent,
  controllers: OpenemsComponent[],
};

@Component({
  selector: "powerassistant",
  templateUrl: "./POWERASSISTANT.HTML",
  providers: [{
    useClass: LiveDataService,
    provide: DataService,
  }],
  standalone: false,
})
export class PowerAssistantComponent extends AbstractFlatWidget {


  protected entries: Entry[] = [];
  protected ignoredControllers: OpenemsComponent[] = [];
  protected date: string = THIS.SERVICE?.historyPeriod?.value?.getText(THIS.TRANSLATE, THIS.SERVICE) ?? "";

  protected override afterIsInitialized() {
    // Create map of ESSs to Controllers, ordered by Scheduler
    const esss: { [essId: string]: OpenemsComponent[] } = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("SCHEDULER.ALL_ALPHABETICALLY")[0]?.properties["CONTROLLERS.IDS"]
      .reduce((result, id) => {
        const component = THIS.CONFIG.COMPONENTS[id];
        const ctrl = THIS.MAP_CONTROLLER(component);
        if (!ctrl) {
          return result;
        }
        // Create Controller Component
        return {
          ...result,
          [CTRL.ESS_ID]: [...(result[CTRL.ESS_ID] || []), {
            id: id,
            alias: COMPONENT.ALIAS,
            factory: COMPONENT.FACTORY_ID,
            channels: CTRL.CHANNELS,
          }],
        };
      },
        // Initialize ESS Keys
        THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS")
          .reduce((result, ess) => {
            return {
              ...result,
              [ESS.ID]: [],
            };
          }, {}),
      );

    THIS.ENTRIES = OBJECT.ENTRIES(esss) //
      .map(e => {
        const ess = THIS.CONFIG.COMPONENTS[e[0]];
        const result = {
          // Create ESS Component
          ess: {
            id: ESS.ID,
            alias: ESS.ALIAS,
            factoryId: ESS.FACTORY_ID,
            channels: [
              { title: "Allowed Charge Power", address: ESS.ID + "/AllowedChargePower", converter: CONVERTER.UNIT("W") },
              { title: "Allowed Discharge Power", address: ESS.ID + "/AllowedDischargePower", converter: CONVERTER.UNIT("W") },
              { title: "Max Apparent Power", address: ESS.ID + "/MaxApparentPower", converter: CONVERTER.UNIT("W") },
              null,
              { title: "Result: Set Active Power", address: ESS.ID + "/DebugSetActivePower", converter: CONVERTER.UNIT("W") },
              { title: "Result: Actual Active Power", address: ESS.ID + "/ActivePower", converter: CONVERTER.UNIT("W") },
              { title: "Result: Set Reactive Power", address: ESS.ID + "/DebugSetReactivePower", converter: CONVERTER.UNIT("var") },
              { title: "Result: Actual Reactive Power", address: ESS.ID + "/ReactivePower", converter: CONVERTER.UNIT("var") },
            ],
          },
          controllers: e[1],
        };
        if (ESS.FACTORY_ID === "ESS.GENERIC.MANAGED_SYMMETRIC") {
          // Create optional Battery Component
          const battery = THIS.CONFIG.COMPONENTS[ESS.PROPERTIES["BATTERY.ID"]];
          result["battery"] = {
            id: BATTERY.ID,
            alias: BATTERY.ALIAS,
            factoryId: BATTERY.FACTORY_ID,
            channels: [
              { title: "Charge Max Current", address: BATTERY.ID + "/ChargeMaxCurrent", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Charge-Limit from BMS", address: BATTERY.ID + "/BpChargeBms", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Charge-Limit from Min-Voltage", address: BATTERY.ID + "/BpChargeMinVoltage", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Charge-Limit from Max-Voltage", address: BATTERY.ID + "/BpChargeMaxVoltage", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Charge-Limit from Min-Temperature", address: BATTERY.ID + "/BpChargeMinTemperature", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Charge-Limit from Max-Temperature", address: BATTERY.ID + "/BpChargeMaxTemperature", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Charge-Limit from Increase-Ramp", address: BATTERY.ID + "/BpChargeIncrease", converter: CONVERTER.UNIT("A") },
              null,
              { title: "Discharge Max Current", address: BATTERY.ID + "/DischargeMaxCurrent", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Discharge-Limit from BMS", address: BATTERY.ID + "/BpDischargeBms", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Discharge-Limit from Min-Voltage", address: BATTERY.ID + "/BpDischargeMinVoltage", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Discharge-Limit from Max-Voltage", address: BATTERY.ID + "/BpDischargeMaxVoltage", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Discharge-Limit from Min-Temperature", address: BATTERY.ID + "/BpDischargeMinTemperature", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Discharge-Limit from Max-Temperature", address: BATTERY.ID + "/BpDischargeMaxTemperature", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Discharge-Limit from Increase-Ramp", address: BATTERY.ID + "/BpDischargeIncrease", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Force-Charge", address: BATTERY.ID + "/BpForceCharge", converter: CONVERTER.UNIT("A") },
              { title: "Battery-Protection: Force-Discharge", address: BATTERY.ID + "/BpForceDischarge", converter: CONVERTER.UNIT("A") },
              null,
              { title: "Min-Cell-Voltage", address: BATTERY.ID + "/MinCellVoltage", converter: CONVERTER.UNIT("mV") },
              { title: "Max-Cell-Voltage", address: BATTERY.ID + "/MaxCellVoltage", converter: CONVERTER.UNIT("mV") },
              { title: "Min-Cell-Temperature", address: BATTERY.ID + "/MinCellTemperature", converter: CONVERTER.UNIT("°C") },
              { title: "Max-Cell-Temperature", address: BATTERY.ID + "/MaxCellTemperature", converter: CONVERTER.UNIT("°C") },
            ],
          };
          // Create optional Battery-Inverter Component
          const batteryInverter = THIS.CONFIG.COMPONENTS[ESS.PROPERTIES["BATTERY_INVERTER.ID"]];
          result["batteryInverter"] = {
            id: BATTERY_INVERTER.ID,
            alias: BATTERY_INVERTER.ALIAS,
            factoryId: BATTERY_INVERTER.FACTORY_ID,
            channels: [],
          };
        }
        return result;
      });
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    return THIS.ALL_CHANNELS()
      .map(c => CHANNEL_ADDRESS.FROM_STRING(C.ADDRESS));
  }

  protected override onCurrentData(currentData: CurrentData) {
    THIS.ALL_CHANNELS()
      .reduce((arr, c) => {
        C.VALUE = CURRENT_DATA.ALL_COMPONENTS[C.ADDRESS];
        ARR.PUSH(c);
        return arr;
      }, []);
  }

  private allChannels(): Channel[] {
    return THIS.ENTRIES
      .flatMap(e => E.ESS.CHANNELS
        .concat(E.BATTERY?.channels || [])
        .concat(E.BATTERY_INVERTER?.channels || [])
        .concat(E.CONTROLLERS.FLAT_MAP(c => C.CHANNELS)))
      .filter(c => c != null);
  }

  private mapController(controller: EDGE_CONFIG.COMPONENT): { essId: string, channels: Channel[] } | null {
    const ctrlId = CONTROLLER.ID;
    const essId = CONTROLLER.PROPERTIES["ESS.ID"];

    const channels: Channel[] | null = [{ title: "Enabled", address: ctrlId + "/_PropertyEnabled", converter: CONVERTER.ENABLED() }];

    switch (CONTROLLER.FACTORY_ID) {
      case "CONTROLLER.ESS.FIX_ACTIVE_POWER":
        CHANNELS.PUSH(
          { title: "Mode", address: ctrlId + "/_PropertyMode", converter: Utils.CONVERT_MANUAL_ON_OFF(THIS.TRANSLATE) },
          { title: "Set-Point", address: ctrlId + "/_PropertyPower", converter: CONVERTER.UNIT("W") },
        );
        break;

      case "CONTROLLER.ESS.EMERGENCY_CAPACITY_RESERVE":
        CHANNELS.PUSH(
          { title: "Mode", address: ctrlId + "/_PropertyIsReserveSocEnabled", converter: CONVERTER.ENABLED() },
          { title: "Set-Point", address: ctrlId + "/DebugSetActivePowerLessOrEquals", converter: CONVERTER.UNIT("W") },
        );
        break;

      case "CONTROLLER.ESS.TIME-Of-Use-Tariff":
        CHANNELS.PUSH(
          { title: "StateMachine", address: ctrlId + "/StateMachine", converter: (value) => value },
        );
        break;

      case "CONTROLLER.ESS.GRID_OPTIMIZED_CHARGE":
        CHANNELS.PUSH(
          { title: "Charge Limit", address: ctrlId + "/SellToGridLimitMinimumChargeLimit", converter: CONVERTER.UNIT("W") },
        );
        break;

      case "CONTROLLER.SYMMETRIC.BALANCING":
      case "CONTROLLER.ESS.HYBRID.SURPLUS-Feed-To-Grid":
        break;

      case "CONTROLLER.EVCS":
        return null;

      default:
        THIS.IGNORED_CONTROLLERS.PUSH({
          id: CONTROLLER.ID,
          alias: CONTROLLER.ALIAS,
          factoryId: CONTROLLER.FACTORY_ID,
          channels: [],
        });
        CONSOLE.LOG("Ignore Controller: " + CONTROLLER.ID + " (" + CONTROLLER.FACTORY_ID + ")", CONTROLLER.PROPERTIES);
        return null;
    }

    return { essId: essId, channels: channels };
  }

}

export namespace Converter {
  export function unit(unit: string): (value: any) => string {
    return function (value: any): string {
      const locale: string = (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.DEFAULT).i18nLocaleKey;
      if (value == null) {
        return "-";
      } else if (value >= 0) {
        return formatNumber(value, locale, "1.0-0") + " " + unit;
      }
    };
  }

  export function enabled(): (value: any) => string {
    return function (value: any): string {
      if (value == null) {
        return "-";
      } else if (value == 1) {
        return "Enabled";
      } else {
        return "DISABLED";
      }
    };
  }
}
