import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';

import { ChannelAddress, CurrentData, EdgeConfig, Utils } from '../../../../shared/shared';
import { Controller_EvcsModalComponent } from './modal/modal.page';

@Component({
  selector: 'Controller_Evcs',
  templateUrl: './Evcs.html'
})
export class Controller_EvcsComponent extends AbstractFlatWidget {

  public controller: EdgeConfig.Component = null;
  public evcsComponent: EdgeConfig.Component = null;
  public chargeMode: ChargeMode = null;
  public status: string;
  public isConnectionSuccessful: boolean = false;
  public isEnergySinceBeginningAllowed: boolean = false;
  public mode: string;
  public isChargingEnabled: boolean = false;
  public defaultChargeMinPower: number;
  public prioritization: string;
  public phases: number;
  public maxChargingValue: number;
  public energySessionLimit: number;
  public readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;
  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

  protected override getChannelAddresses() {
    return [
      new ChannelAddress(this.componentId, 'ChargePower'),
      new ChannelAddress(this.componentId, 'Phases'),
      new ChannelAddress(this.componentId, 'Plug'),
      new ChannelAddress(this.componentId, 'Status'),
      new ChannelAddress(this.componentId, 'State'),
      new ChannelAddress(this.componentId, 'EnergySession'),
      // channels for modal component, subscribe here for better UX
      new ChannelAddress(this.componentId, 'MinimumHardwarePower'),
      new ChannelAddress(this.componentId, 'MaximumHardwarePower'),
      new ChannelAddress(this.componentId, 'SetChargePowerLimit')
    ];
  }

  protected override onCurrentData(currentData: CurrentData) {

    // Gets the Controller & Component for the given EVCS - Component.
    let controllers = this.config.getComponentsByFactory("Controller.Evcs");
    for (let controller of controllers) {
      let properties = controller.properties;
      if ("evcs.id" in properties && properties["evcs.id"] === this.componentId) {
        this.controller = controller;
      }
    }
    this.evcsComponent = this.config.getComponent(this.componentId);
    this.isConnectionSuccessful = currentData.allComponents[this.componentId + '/State'] != 3 ? true : false;
    this.status = this.getState(currentData.allComponents[this.componentId + "/Status"], currentData.allComponents[this.componentId + "/Plug"]);

    // Check if Energy since beginning is allowed
    if (currentData.allComponents[this.componentId + '/ChargePower'] > 0 || currentData.allComponents[this.componentId + '/Status'] == 2 || currentData.allComponents[this.componentId + '/Status'] == 7) {
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
      this.isChargingEnabled = this.controller.properties['enabledCharging'] ? true : false;
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
  }

  /**
   * Returns the state of the EVCS
   * 
   * @param state 
   * @param plug 
   * 
   */
  private getState(state: number, plug: number): string {
    if (this.controller != null) {
      if (this.controller.properties.enabledCharging != null && this.controller.properties.enabledCharging == false) {
        return this.translate.instant('Edge.Index.Widgets.EVCS.chargingStationDeactivated');
      }
    }
    let chargeState = state;
    let chargePlug = plug;

    if (chargePlug == null) {
      if (chargeState == null) {
        return this.translate.instant('Edge.Index.Widgets.EVCS.notCharging');
      }
    } else if (chargePlug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
      return this.translate.instant('Edge.Index.Widgets.EVCS.cableNotConnected');
    }
    switch (chargeState) {
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

  async presentModal() {
    const modal = await this.modalController.create({
      component: Controller_EvcsModalComponent,
      componentProps: {
        controller: this.controller,
        edge: this.edge,
        componentId: this.componentId,
        evcsComponent: this.evcsComponent
        // getState: this.getState
      }
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

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER';
