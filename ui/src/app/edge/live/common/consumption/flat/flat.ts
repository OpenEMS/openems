// @ts-strict-ignore
import { Component } from "@angular/core";
import { EvcsComponent } from "src/app/shared/components/edge/components/evcsComponent";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "consumption",
  templateUrl: "./flat.html",
  standalone: false,
})
export class CommonConsumptionGeneralComponent extends AbstractFlatWidget {

  public evcss: EvcsComponent[] | null = null;
  public consumptionMeters: EdgeConfig.Component[] | null = null;
  public sumActivePower: number = 0;
  public evcsSumOfChargePower: number;
  public otherPower: number;
  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  protected modalComponent: Modal | null = null;

  protected override afterIsInitialized(): void {
    this.modalComponent = this.getModalComponent();
  }

  protected getModalComponent(): Modal {
    return { component: ModalComponent };
  };

  protected override getChannelAddresses() {

    const channelAddresses: ChannelAddress[] = [
      new ChannelAddress("_sum", "ConsumptionActivePower"),

      // TODO should be moved to Modal
      new ChannelAddress("_sum", "ConsumptionActivePowerL1"),
      new ChannelAddress("_sum", "ConsumptionActivePowerL2"),
      new ChannelAddress("_sum", "ConsumptionActivePowerL3"),
    ];

    // Get consumptionMeterComponents
    this.consumptionMeters = this.config?.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => {
        const natureIds = this.config?.getNatureIdsByFactoryId(component.factoryId);
        const isEvcs = natureIds.includes("io.openems.edge.evcs.api.Evcs");

        return component.isEnabled && this.config?.isTypeConsumptionMetered(component) &&
          isEvcs === false;
      });

    for (const component of this.consumptionMeters) {
      channelAddresses.push(
        new ChannelAddress(component.id, "ActivePower"),
        new ChannelAddress(component.id, "ActivePowerL1"),
        new ChannelAddress(component.id, "ActivePowerL2"),
        new ChannelAddress(component.id, "ActivePowerL3"),
      );
    }

    // Get EVCSs
    this.evcss = EvcsComponent.getComponents(this.config, this.edge);

    for (const component of this.evcss) {
      channelAddresses.push(
        component.powerChannel,
      );
    }
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData) {

    this.evcsSumOfChargePower = 0;
    let consumptionMetersSumOfActivePower: number = 0;
    this.sumActivePower = currentData.allComponents["_sum/ConsumptionActivePower"];

    // TODO move sums to Model
    // Iterate over evcsComponents to get ChargePower for every component
    for (const component of this.evcss) {
      if (currentData.allComponents[component.powerChannel.toString()]) {
        this.evcsSumOfChargePower += currentData.allComponents[component.powerChannel.toString()];
      }
    }

    // Iterate over evcsComponents to get ChargePower for every component
    for (const component of this.consumptionMeters) {
      if (currentData.allComponents[component.id + "/ActivePower"]) {
        consumptionMetersSumOfActivePower += currentData.allComponents[component.id + "/ActivePower"];
      }
    }

    this.otherPower = Utils.subtractSafely(this.sumActivePower,
      Utils.addSafely(this.evcsSumOfChargePower, consumptionMetersSumOfActivePower));
  }

}
