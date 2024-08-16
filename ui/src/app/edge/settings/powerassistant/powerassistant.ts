// @ts-strict-ignore
import { formatNumber } from '@angular/common';
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';
import { DataService } from 'src/app/shared/components/shared/dataservice';
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from '../../../shared/shared';
import { LiveDataService } from '../../live/livedataservice';

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
  selector: 'powerassistant',
  templateUrl: './powerassistant.html',
  providers: [{
    useClass: LiveDataService,
    provide: DataService,
  }],
})
export class PowerAssistantComponent extends AbstractFlatWidget {


  protected entries: Entry[] = [];
  protected ignoredControllers: OpenemsComponent[] = [];
  protected date: string = this.service?.historyPeriod?.value?.getText(this.translate, this.service) ?? "";

  protected override afterIsInitialized() {
    // Create map of ESSs to Controllers, ordered by Scheduler
    const esss: { [essId: string]: OpenemsComponent[] } = this.config.getComponentsByFactory("Scheduler.AllAlphabetically")[0]?.properties["controllers.ids"]
      .reduce((result, id) => {
        const component = this.config.components[id];
        const ctrl = this.mapController(component);
        if (!ctrl) {
          return result;
        }
        // Create Controller Component
        return {
          ...result,
          [ctrl.essId]: [...(result[ctrl.essId] || []), {
            id: id,
            alias: component.alias,
            factory: component.factoryId,
            channels: ctrl.channels,
          }],
        };
      },
        // Initialize ESS Keys
        this.config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
          .reduce((result, ess) => {
            return {
              ...result,
              [ess.id]: [],
            };
          }, {}),
      );

    this.entries = Object.entries(esss) //
      .map(e => {
        const ess = this.config.components[e[0]];
        const result = {
          // Create ESS Component
          ess: {
            id: ess.id,
            alias: ess.alias,
            factoryId: ess.factoryId,
            channels: [
              { title: "Allowed Charge Power", address: ess.id + "/AllowedChargePower", converter: Converter.unit("W") },
              { title: "Allowed Discharge Power", address: ess.id + "/AllowedDischargePower", converter: Converter.unit("W") },
              { title: "Max Apparent Power", address: ess.id + "/MaxApparentPower", converter: Converter.unit("W") },
              null,
              { title: "Result: Set Active Power", address: ess.id + "/DebugSetActivePower", converter: Converter.unit("W") },
              { title: "Result: Actual Active Power", address: ess.id + "/ActivePower", converter: Converter.unit("W") },
              { title: "Result: Set Reactive Power", address: ess.id + "/DebugSetReactivePower", converter: Converter.unit("var") },
              { title: "Result: Actual Reactive Power", address: ess.id + "/ReactivePower", converter: Converter.unit("var") },
            ],
          },
          controllers: e[1],
        };
        if (ess.factoryId === "Ess.Generic.ManagedSymmetric") {
          // Create optional Battery Component
          const battery = this.config.components[ess.properties["battery.id"]];
          result['battery'] = {
            id: battery.id,
            alias: battery.alias,
            factoryId: battery.factoryId,
            channels: [
              { title: "Charge Max Current", address: battery.id + "/ChargeMaxCurrent", converter: Converter.unit("A") },
              { title: "Battery-Protection: Charge-Limit from BMS", address: battery.id + "/BpChargeBms", converter: Converter.unit("A") },
              { title: "Battery-Protection: Charge-Limit from Min-Voltage", address: battery.id + "/BpChargeMinVoltage", converter: Converter.unit("A") },
              { title: "Battery-Protection: Charge-Limit from Max-Voltage", address: battery.id + "/BpChargeMaxVoltage", converter: Converter.unit("A") },
              { title: "Battery-Protection: Charge-Limit from Min-Temperature", address: battery.id + "/BpChargeMinTemperature", converter: Converter.unit("A") },
              { title: "Battery-Protection: Charge-Limit from Max-Temperature", address: battery.id + "/BpChargeMaxTemperature", converter: Converter.unit("A") },
              { title: "Battery-Protection: Charge-Limit from Increase-Ramp", address: battery.id + "/BpChargeIncrease", converter: Converter.unit("A") },
              null,
              { title: "Discharge Max Current", address: battery.id + "/DischargeMaxCurrent", converter: Converter.unit("A") },
              { title: "Battery-Protection: Discharge-Limit from BMS", address: battery.id + "/BpDischargeBms", converter: Converter.unit("A") },
              { title: "Battery-Protection: Discharge-Limit from Min-Voltage", address: battery.id + "/BpDischargeMinVoltage", converter: Converter.unit("A") },
              { title: "Battery-Protection: Discharge-Limit from Max-Voltage", address: battery.id + "/BpDischargeMaxVoltage", converter: Converter.unit("A") },
              { title: "Battery-Protection: Discharge-Limit from Min-Temperature", address: battery.id + "/BpDischargeMinTemperature", converter: Converter.unit("A") },
              { title: "Battery-Protection: Discharge-Limit from Max-Temperature", address: battery.id + "/BpDischargeMaxTemperature", converter: Converter.unit("A") },
              { title: "Battery-Protection: Discharge-Limit from Increase-Ramp", address: battery.id + "/BpDischargeIncrease", converter: Converter.unit("A") },
              { title: "Battery-Protection: Force-Charge", address: battery.id + "/BpForceCharge", converter: Converter.unit("A") },
              { title: "Battery-Protection: Force-Discharge", address: battery.id + "/BpForceDischarge", converter: Converter.unit("A") },
              null,
              { title: "Min-Cell-Voltage", address: battery.id + "/MinCellVoltage", converter: Converter.unit("mV") },
              { title: "Max-Cell-Voltage", address: battery.id + "/MaxCellVoltage", converter: Converter.unit("mV") },
              { title: "Min-Cell-Temperature", address: battery.id + "/MinCellTemperature", converter: Converter.unit("°C") },
              { title: "Max-Cell-Temperature", address: battery.id + "/MaxCellTemperature", converter: Converter.unit("°C") },
            ],
          };
          // Create optional Battery-Inverter Component
          const batteryInverter = this.config.components[ess.properties["batteryInverter.id"]];
          result['batteryInverter'] = {
            id: batteryInverter.id,
            alias: batteryInverter.alias,
            factoryId: batteryInverter.factoryId,
            channels: [],
          };
        }
        return result;
      });
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    return this.allChannels()
      .map(c => ChannelAddress.fromString(c.address));
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.allChannels()
      .reduce((arr, c) => {
        c.value = currentData.allComponents[c.address];
        arr.push(c);
        return arr;
      }, []);
  }

  private allChannels(): Channel[] {
    return this.entries
      .flatMap(e => e.ess.channels
        .concat(e.battery?.channels || [])
        .concat(e.batteryInverter?.channels || [])
        .concat(e.controllers.flatMap(c => c.channels)))
      .filter(c => c != null);
  }

  private mapController(controller: EdgeConfig.Component): { essId: string, channels: Channel[] } | null {
    const ctrlId = controller.id;
    const essId = controller.properties["ess.id"];

    const channels: Channel[] | null = [{ title: "Enabled", address: ctrlId + "/_PropertyEnabled", converter: Converter.enabled() }];

    switch (controller.factoryId) {
      case "Controller.Ess.FixActivePower":
        channels.push(
          { title: "Mode", address: ctrlId + "/_PropertyMode", converter: Utils.CONVERT_MANUAL_ON_OFF(this.translate) },
          { title: "Set-Point", address: ctrlId + "/_PropertyPower", converter: Converter.unit("W") },
        );
        break;

      case "Controller.Ess.EmergencyCapacityReserve":
        channels.push(
          { title: "Mode", address: ctrlId + "/_PropertyIsReserveSocEnabled", converter: Converter.enabled() },
          { title: "Set-Point", address: ctrlId + "/DebugSetActivePowerLessOrEquals", converter: Converter.unit("W") },
        );
        break;

      case "Controller.Ess.Time-Of-Use-Tariff":
        channels.push(
          { title: "StateMachine", address: ctrlId + "/StateMachine", converter: (value) => value },
        );
        break;

      case "Controller.Ess.GridOptimizedCharge":
        channels.push(
          { title: "Charge Limit", address: ctrlId + "/SellToGridLimitMinimumChargeLimit", converter: Converter.unit("W") },
        );
        break;

      case "Controller.Symmetric.Balancing":
      case "Controller.Ess.Hybrid.Surplus-Feed-To-Grid":
        break;

      case "Controller.Evcs":
        return null;

      default:
        this.ignoredControllers.push({
          id: controller.id,
          alias: controller.alias,
          factoryId: controller.factoryId,
          channels: [],
        });
        console.log("Ignore Controller: " + controller.id + " (" + controller.factoryId + ")", controller.properties);
        return null;
    }

    return { essId: essId, channels: channels };
  }

}

export namespace Converter {
  export function unit(unit: string): (value: any) => string {
    return function (value: any): string {
      if (value == null) {
        return '-';
      } else if (value >= 0) {
        return formatNumber(value, 'de', '1.0-0') + ' ' + unit;
      }
    };
  }

  export function enabled(): (value: any) => string {
    return function (value: any): string {
      if (value == null) {
        return '-';
      } else if (value == 1) {
        return "Enabled";
      } else {
        return "DISABLED";
      }
    };
  }
}
