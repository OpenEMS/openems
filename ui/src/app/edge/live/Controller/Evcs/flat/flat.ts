import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from 'src/app/shared/shared';

import { ModalComponent } from '../modal/modal';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER' | 'OFF';


@Component({
  selector: 'Controller_Evcs',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  public readonly CONVERT_MANUAL_ON_OFF = Utils.CONVERT_MANUAL_ON_OFF(this.translate);

  protected controller: EdgeConfig.Component;
  protected evcsComponent: EdgeConfig.Component = null;
  protected isConnectionSuccessful: boolean = false;
  protected isEnergySinceBeginningAllowed: boolean = false;
  protected mode: string;
  protected isChargingEnabled: boolean = false;
  protected defaultChargeMinPower: number;
  protected prioritization: string;
  protected phases: number;
  protected maxChargingValue: number;
  protected energySessionLimit: number;
  protected state: string = '';
  protected minChargePower: number;
  protected maxChargePower: number;
  protected forceChargeMinPower: string;
  protected chargeMode: ChargeMode = null;
  protected readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;
  protected readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;
  protected readonly CONVERT_MANUAL_ON_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
  protected chargeTarget: string;
  protected energySession: string;
  protected chargeDischargePower: { name: string, value: number };
  protected propertyMode: DefaultTypes.ManualOnOff = null;
  protected status: string;

  protected override getChannelAddresses(): ChannelAddress[] {
    let controllers = this.config.getComponentsByFactory("Controller.Evcs");
    for (let controller of controllers) {
      let properties = controller.properties;
      if ("evcs.id" in properties && properties["evcs.id"] === this.componentId) {
        this.controller = controller;
      }
    }
    return [
      new ChannelAddress(this.component.id, 'ChargePower'),
      new ChannelAddress(this.component.id, 'Phases'),
      new ChannelAddress(this.component.id, 'Plug'),
      new ChannelAddress(this.component.id, 'Status'),
      new ChannelAddress(this.component.id, 'State'),
      new ChannelAddress(this.component.id, 'EnergySession'),
      // channels for modal component, subscribe here for better UX
      new ChannelAddress(this.component.id, 'MinimumHardwarePower'),
      new ChannelAddress(this.component.id, 'MaximumHardwarePower'),
      new ChannelAddress(this.component.id, 'SetChargePowerLimit'),
      new ChannelAddress(this.controller.id, '_PropertyEnabledCharging'),
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {

    this.evcsComponent = this.config.getComponent(this.component.id);
    this.isConnectionSuccessful = currentData.allComponents[this.component.id + '/State'] != 3 ? true : false;
    this.status = this.getState(this.controller ? currentData.allComponents[this.controller.id + '/_PropertyEnabledCharging'] === 1 : null, currentData.allComponents[this.component.id + "/Status"], currentData.allComponents[this.component.id + "/Plug"]);

    // Check if Energy since beginning is allowed
    if (currentData.allComponents[this.component.id + '/ChargePower'] > 0 || currentData.allComponents[this.component.id + '/Status'] == 2 || currentData.allComponents[this.component.id + '/Status'] == 7) {
      this.isEnergySinceBeginningAllowed = true;
    }

    // Mode
    if (this.isChargingEnabled) {
      if (this.chargeMode == 'FORCE_CHARGE') {
        this.mode = this.translate.instant('General.manually');
      } else if (this.chargeMode == 'EXCESS_POWER') {
        this.mode = this.translate.instant('Edge.Index.Widgets.EVCS.OptimizedChargeMode.shortName');
      }
    }

    // Check if Controller is set
    if (this.controller) {

      // ChargeMode
      this.chargeMode = this.controller.properties['chargeMode'];
      // Check if Charging is enabled
      this.isChargingEnabled = currentData.allComponents[this.controller.id + '/_PropertyEnabledCharging'] === 1 ? true : false;
      // DefaultChargeMinPower
      this.defaultChargeMinPower = this.controller.properties['defaultChargeMinPower'];
      // Prioritization
      this.prioritization =
        this.controller.properties['priority'] in Prioritization
          ? 'Edge.Index.Widgets.EVCS.OptimizedChargeMode.ChargingPriority.' + this.controller.properties['priority'].toLowerCase()
          : '';
      // MaxChargingValue
      if (this.phases) {
        this.maxChargingValue = Utils.multiplySafely(this.controller.properties['forceChargeMinPower'], this.phases);
      } else {
        this.maxChargingValue = Utils.multiplySafely(this.controller.properties['forceChargeMinPower'], 3);
      }
      // EnergySessionLimit
      this.energySessionLimit = this.controller.properties['energySessionLimit'];
    }

    // Phases
    this.phases = currentData.allComponents[this.componentId + '/Phases'];

    this.chargeDischargePower = Utils.convertChargeDischargePower(this.translate, currentData.allComponents[this.component.id + "/ChargePower"]);
    this.chargeTarget = Utils.CONVERT_TO_WATT(this.formatNumber(currentData.allComponents[this.component.id + "/SetChargePowerLimit"]));
    this.energySession = Utils.CONVERT_TO_WATT(currentData.allComponents[this.component.id + "/EnergySession"]);

    this.minChargePower = this.formatNumber(currentData.allComponents[this.component.id + '/MinimumHardwarePower']);
    this.maxChargePower = this.formatNumber(currentData.allComponents[this.component.id + '/MaximumHardwarePower']);
    this.state = currentData.allComponents[this.component.id + "/Status"];
  }

  /**
 * Returns the state of the EVCS
 *
 * @param state the state
 * @param plug the plug
 *
 */
  private getState(enabledCharging: boolean, state: number, plug: number): string {

    if (enabledCharging === false) {
      return this.translate.instant('Edge.Index.Widgets.EVCS.chargingStationDeactivated');
    }

    if (plug == null) {
      if (state == null) {
        return this.translate.instant('Edge.Index.Widgets.EVCS.notCharging');
      }
    } else if (plug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
      return this.translate.instant('Edge.Index.Widgets.EVCS.cableNotConnected');
    }
    switch (state) {
      case ChargeState.STARTING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.starting');
      case ChargeState.UNDEFINED:
      case ChargeState.ERROR:
        return this.translate.instant('Edge.Index.Widgets.EVCS.error');
      case ChargeState.READY_FOR_CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.readyForCharging');
      case ChargeState.NOT_READY_FOR_CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.notReadyForCharging');
      case ChargeState.AUTHORIZATION_REJECTED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.notCharging');
      case ChargeState.CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.charging');
      case ChargeState.ENERGY_LIMIT_REACHED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.chargeLimitReached');
      case ChargeState.CHARGING_FINISHED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.carFull');
    }
  }

  formatNumber(i: number) {
    let round = Math.ceil(i / 100) * 100;
    return round;
  }


  async presentModal() {
    const modal = await this.modalController.create({
      component: ModalComponent,
      componentProps: {
        component: this.component,
      },
    });
    return await modal.present();
  }
}

enum ChargeState {
  UNDEFINED = -1,           //Undefined
  STARTING,                 //Starting
  NOT_READY_FOR_CHARGING,   //Not ready for Charging e.g. unplugged, X1 or "ena" not enabled, RFID not enabled,...
  READY_FOR_CHARGING,       //Ready for Charging waiting for EV charging request
  CHARGING,                 //Charging
  ERROR,                    //Error
  AUTHORIZATION_REJECTED,   //Authorization rejected
  ENERGY_LIMIT_REACHED,     //Energy limit reached
  CHARGING_FINISHED         //Charging has finished
}


enum ChargePlug {
  UNDEFINED = -1,                           //Undefined
  UNPLUGGED,                                //Unplugged
  PLUGGED_ON_EVCS,                          //Plugged on EVCS
  PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
  PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
  PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7  //Plugged on EVCS and on EV and locked
}
enum Prioritization {
  CAR,
  STORAGE
}
