// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "consumption",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  public evcss: EDGE_CONFIG.COMPONENT[] | null = null;
  public consumptionMeters: EDGE_CONFIG.COMPONENT[] | null = null;
  public sumActivePower: number = 0;
  public evcsSumOfChargePower: number;
  public otherPower: number;
  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

  async presentModal() {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: ModalComponent,
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses() {

    const channelAddresses: ChannelAddress[] = [
      new ChannelAddress("_sum", "ConsumptionActivePower"),

      // TODO should be moved to Modal
      new ChannelAddress("_sum", "ConsumptionActivePowerL1"),
      new ChannelAddress("_sum", "ConsumptionActivePowerL2"),
      new ChannelAddress("_sum", "ConsumptionActivePowerL3"),
    ];

    // Get consumptionMeterComponents
    THIS.CONSUMPTION_METERS = THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
      .filter(component => COMPONENT.IS_ENABLED && THIS.CONFIG.IS_TYPE_CONSUMPTION_METERED(component));

    for (const component of THIS.CONSUMPTION_METERS) {
      CHANNEL_ADDRESSES.PUSH(
        new ChannelAddress(COMPONENT.ID, "ActivePower"),
        new ChannelAddress(COMPONENT.ID, "ActivePowerL1"),
        new ChannelAddress(COMPONENT.ID, "ActivePowerL2"),
        new ChannelAddress(COMPONENT.ID, "ActivePowerL3"),
      );
    }

    // Get EVCSs
    THIS.EVCSS = THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.EVCS.API.EVCS")
      .filter(component =>
        !(COMPONENT.FACTORY_ID == "EVCS.CLUSTER.SELF_CONSUMPTION") &&
        !(COMPONENT.FACTORY_ID == "EVCS.CLUSTER.PEAK_SHAVING") &&
        !(THIS.CONFIG.FACTORIES[COMPONENT.FACTORY_ID].NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")) &&
        !COMPONENT.IS_ENABLED == false);

    for (const component of THIS.EVCSS) {
      CHANNEL_ADDRESSES.PUSH(
        new ChannelAddress(COMPONENT.ID, "ChargePower"),
      );
    }
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData) {

    THIS.EVCS_SUM_OF_CHARGE_POWER = 0;
    let consumptionMetersSumOfActivePower: number = 0;
    THIS.SUM_ACTIVE_POWER = CURRENT_DATA.ALL_COMPONENTS["_sum/ConsumptionActivePower"];

    // TODO move sums to Model
    // Iterate over evcsComponents to get ChargePower for every component
    for (const component of THIS.EVCSS) {
      if (CURRENT_DATA.ALL_COMPONENTS[COMPONENT.ID + "/ChargePower"]) {
        THIS.EVCS_SUM_OF_CHARGE_POWER += CURRENT_DATA.ALL_COMPONENTS[COMPONENT.ID + "/ChargePower"];
      }
    }

    // Iterate over evcsComponents to get ChargePower for every component
    for (const component of THIS.CONSUMPTION_METERS) {
      if (CURRENT_DATA.ALL_COMPONENTS[COMPONENT.ID + "/ActivePower"]) {
        consumptionMetersSumOfActivePower += CURRENT_DATA.ALL_COMPONENTS[COMPONENT.ID + "/ActivePower"];
      }
    }

    THIS.OTHER_POWER = UTILS.SUBTRACT_SAFELY(THIS.SUM_ACTIVE_POWER,
      UTILS.ADD_SAFELY(THIS.EVCS_SUM_OF_CHARGE_POWER, consumptionMetersSumOfActivePower));
  }

}
