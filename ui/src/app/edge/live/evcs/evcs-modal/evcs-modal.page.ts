import { Component, OnInit, HostListener, Input, OnChanges } from '@angular/core';
import { environment } from 'src/environments/openems-backend-dev-local';
import { PopoverController, ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket, Service, EdgeConfig, Edge } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { EvcsPopoverComponent } from './evcs-popover.page';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER';
type Priority = 'CAR' | 'STORAGE';

@Component({
  selector: 'evcs-modal',
  templateUrl: './evcs-modal.page.html'
})
export class EvcsModalComponent implements OnInit {

  @Input() edge: Edge;
  @Input() controller: EdgeConfig.Component = null;
  @Input() private componentId: string;

  public currChargingPower: number
  public chargeState: ChargeState;
  private chargePlug: ChargePlug;
  public env = environment;

  constructor(
    protected service: Service,
    public websocket: Websocket,
    public router: Router,
    protected translate: TranslateService,
    private modalCtrl: ModalController,
    public popoverController: PopoverController
  ) { }

  ngOnInit() {
  }

  cancel() {
    this.modalCtrl.dismiss();
  }

  /**  
  * Updates the Charge-Mode of the EVCS-Controller.
  * 
  * @param event 
  */
  updateChargeMode(event: CustomEvent, currentController: EdgeConfig.Component) {
    let oldChargeMode = currentController.properties.chargeMode;
    let newChargeMode: ChargeMode;

    switch (event.detail.value) {
      case 'FORCE_CHARGE':
        newChargeMode = 'FORCE_CHARGE';
        break;
      case 'EXCESS_POWER':
        newChargeMode = 'EXCESS_POWER';
        break;
    }

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'chargeMode', value: newChargeMode }
      ]).then(response => {
        currentController.properties.chargeMode = newChargeMode;
      }).catch(reason => {
        currentController.properties.chargeMode = oldChargeMode;
        console.warn(reason);
      });
    }
  }
  /**
   * Changed the Priority between the components of the charging session
   */
  priorityChanged(event: CustomEvent, currentController: EdgeConfig.Component) {
    let oldPriority = currentController.properties.priority;
    let newPriority: Priority;

    switch (event.detail.value) {
      case 'CAR':
        newPriority = 'CAR';
        break;
      case 'STORAGE':
        newPriority = 'STORAGE';
        break;
    }

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'priority', value: newPriority }
      ]).then(response => {
        currentController.properties.priority = newPriority;
      }).catch(reason => {
        currentController.properties.priority = oldPriority;
        console.warn(reason);
      });
    }
  }

  /**
   * Updates the Min-Power of force charging
   *
   * @param event
   */
  updateForceMinPower(event: CustomEvent, currentController: EdgeConfig.Component) {
    let oldMinChargePower = currentController.properties.forceChargeMinPower;
    let newMinChargePower = event.detail.value;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'forceChargeMinPower', value: newMinChargePower }
      ]).then(response => {
        currentController.properties.forceChargeMinPower = newMinChargePower;
      }).catch(reason => {
        currentController.properties.forceChargeMinPower = oldMinChargePower;
        console.warn(reason);
      });
    }
  }

  /**
   * Updates the Min-Power of default charging
   *
   * @param event
   */
  updateDefaultMinPower(event: CustomEvent, currentController: EdgeConfig.Component) {
    let oldMinChargePower = currentController.properties.defaultChargeMinPower;
    let newMinChargePower = event.detail.value;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'defaultChargeMinPower', value: newMinChargePower }
      ]).then(response => {
        currentController.properties.defaultChargeMinPower = newMinChargePower;
      }).catch(reason => {
        currentController.properties.defaultChargeMinPower = oldMinChargePower;
        console.warn(reason);
      });
    }
  }

  currentLimitChanged(event: CustomEvent, property: String) {

  }

  /**
   * uptdate the state of the toggle whitch renders the minimum charge power
   * 
   * @param event 
   * @param phases 
   */
  allowMinimumChargePower(event: CustomEvent, phases: number, currentController: EdgeConfig.Component) {

    let oldMinChargePower = currentController.properties.defaultChargeMinPower;

    let newMinChargePower = 0;
    if (oldMinChargePower == null || oldMinChargePower == 0) {
      newMinChargePower = phases != undefined ? 1400 * phases : 4200;
    }
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'defaultChargeMinPower', value: newMinChargePower }
      ]).then(response => {
        currentController.properties.defaultChargeMinPower = newMinChargePower;
      }).catch(reason => {
        currentController.properties.defaultChargeMinPower = oldMinChargePower;
        console.warn(reason);
      });
    }
  }

  /**
  * Aktivates or deaktivates the Charging
  * 
  * @param event 
  */
  enableOrDisableCharging(event: CustomEvent, currentController: EdgeConfig.Component) {

    let oldChargingState = currentController.properties.enabledCharging;
    let newChargingState = !oldChargingState;
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'enabledCharging', value: newChargingState }
      ]).then(response => {
        currentController.properties.enabledCharging = newChargingState;
      }).catch(reason => {
        currentController.properties.enabledCharging = oldChargingState;
        console.warn(reason);
      });
    }
  }
  /**
   * Gets the output for the current state or the current charging power
   * 
   * @param power 
   * @param state 
   * @param plug 
   */
  getState(power: Number, state: number, plug: number) {
    if (this.controller.properties.enabledCharging != null && this.controller.properties.enabledCharging == false) {
      return this.translate.instant('Edge.Index.Widgets.EVCS.ChargingStationDeactivated');
    }
    if (power == null || power == 0) {

      this.chargeState = state;
      this.chargePlug = plug;

      if (this.chargePlug == null) {
        return "0 W";
      } else if (this.chargePlug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
        return this.translate.instant('Edge.Index.Widgets.EVCS.CableNotConnected');
      }

      switch (this.chargeState) {
        case ChargeState.STARTING:
          return this.translate.instant('Edge.Index.Widgets.EVCS.Starting');
        case ChargeState.UNDEFINED:
        case ChargeState.ERROR:
          return this.translate.instant('Edge.Index.Widgets.EVCS.Error');
        case ChargeState.READY_FOR_CHARGING:
          return this.translate.instant('Edge.Index.Widgets.EVCS.CarFull');
        case ChargeState.NOT_READY_FOR_CHARGING:
          return this.translate.instant('Edge.Index.Widgets.EVCS.NotReadyForCharging');
        case ChargeState.AUTHORIZATION_REJECTED:
          return power + " W";
      }
    }
    return power + " W";
  }

  /**
   * Round to 100 and 
   * Round up (ceil)
   * 
   * @param i 
   */
  formatNumber(i: number) {
    let round = Math.ceil(i / 100) * 100;
    return round;
  }

  /**
   * Get Value or 3
   * 
   * @param i 
   */
  getValueOrThree(i: number) {
    if (i == null || i == undefined) {
      return 3;
    } else {
      return i;
    }
  }

  //TODO: Do it in the edge component
  currentChargingPower(): number {
    return this.sumOfChannel("ChargePower");
  }

  private sumOfChannel(channel: String): number {

    let sum = 0;/*
    this.evcsMap.forEach(station => {
      let channelValue = this.edge.currentData.value.channel[station.id + "/" + channel];
      if (channelValue != null) {
        sum += channelValue;
      };
    });
    */
    return sum;
  }

  async presentPopover(ev: any) {
    const popover = await this.popoverController.create({
      component: EvcsPopoverComponent,
      event: ev,
      translucent: true
    });
    return await popover.present();
  }
}

enum ChargeState {
  UNDEFINED = -1,           //Undefined
  STARTING,                 //Starting
  NOT_READY_FOR_CHARGING,   //Not ready for Charging e.g. unplugged, X1 or "ena" not enabled, RFID not enabled,...
  READY_FOR_CHARGING,       //Ready for Charging waiting for EV charging request
  CHARGING,                 //Charging
  ERROR,                    //Error
  AUTHORIZATION_REJECTED    //Authorization rejected
}

enum ChargePlug {
  UNDEFINED = -1,                           //Undefined
  UNPLUGGED,                                //Unplugged
  PLUGGED_ON_EVCS,                          //Plugged on EVCS
  PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
  PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
  PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7  //Plugged on EVCS and on EV and locked
}
