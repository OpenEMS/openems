import { Component, Input, OnInit } from '@angular/core';
import { ModalController, PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { AdministrationComponent } from './administration/administration.component';
import { Controller_EvcsPopoverComponent } from './popover/popover.page';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER' | 'OFF';
type Priority = 'CAR' | 'STORAGE';

@Component({
  selector: 'Controller_Evcs-modal',
  templateUrl: './modal.page.html'
})
export class Controller_EvcsModalComponent implements OnInit {

  @Input() public edge: Edge;
  @Input() public controller: EdgeConfig.Component;
  @Input() public componentId: string;
  @Input() public evcsComponent: EdgeConfig.Component;

  //chargeMode value to determine third state 'Off' (OFF State is not available in EDGE)
  public chargeMode: ChargeMode = null;
  private oldNumberOfPhases: number = null;

  constructor(
    protected service: Service,
    protected translate: TranslateService,
    public modalCtrl: ModalController,
    public popoverController: PopoverController,
    public modalController: ModalController,
    public websocket: Websocket
  ) { }

  ngOnInit() {
    if (this.controller != null) {
      if (this.controller.properties.enabledCharging) {
        this.chargeMode = this.controller.properties.chargeMode;
      }
      else {
        this.chargeMode = 'OFF';
      }
    }
    this.oldNumberOfPhases = this.getNumberOfPhasesOrThree();
  }

  /**
   * Returns the state of the EVCS
   * 
   * @param state 
   * @param plug 
   * 
   */
  getState(state: number, plug: number) {

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
      case ChargeState.ERROR:
        return this.translate.instant('Edge.Index.Widgets.EVCS.error');
      case ChargeState.READY_FOR_CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.readyForCharging');
      case ChargeState.NOT_READY_FOR_CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.notReadyForCharging');
      case ChargeState.AUTHORIZATION_REJECTED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.notCharging');
      case ChargeState.UNDEFINED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.unknown');
      case ChargeState.CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.charging');
      case ChargeState.ENERGY_LIMIT_REACHED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.chargeLimitReached');
      case ChargeState.CHARGING_FINISHED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.carFull');
    }
  }

  /**  
  * Updates the Charge-Mode of the EVCS-Controller.
  * 
  * @param event 
  */
  updateChargeMode(event: CustomEvent, currentController: EdgeConfig.Component) {
    let oldChargeMode = currentController.properties.chargeMode;
    let newChargeMode: ChargeMode;

    let oldEnabledCharging = currentController.properties.enabledCharging;
    let newEnabledCharging: boolean;

    switch (event.detail.value) {
      case 'OFF':
        newChargeMode = 'FORCE_CHARGE';
        this.chargeMode = 'OFF';
        newEnabledCharging = false;
        break;
      case 'FORCE_CHARGE':
        newChargeMode = 'FORCE_CHARGE';
        newEnabledCharging = true;
        this.chargeMode = 'FORCE_CHARGE';

        break;
      case 'EXCESS_POWER':
        newChargeMode = 'EXCESS_POWER';
        newEnabledCharging = true;
        this.chargeMode = 'EXCESS_POWER';
        break;
    }

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'chargeMode', value: newChargeMode },
        { name: 'enabledCharging', value: newEnabledCharging }
      ]).then(() => {
        currentController.properties.enabledCharging = newEnabledCharging;
        currentController.properties.chargeMode = newChargeMode;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        currentController.properties.enabledCharging = oldEnabledCharging;
        currentController.properties.chargeMode = oldChargeMode;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
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
      ]).then(() => {
        currentController.properties.priority = newPriority;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        currentController.properties.priority = oldPriority;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }

  /**
   * Updates the Min-Power of force charging
   *
   * @param event
   */
  updateForceMinPower(event: CustomEvent, currentController: EdgeConfig.Component, numberOfPhases: number) {
    if (numberOfPhases != this.oldNumberOfPhases) {
      this.oldNumberOfPhases = numberOfPhases;
      return;
    }

    let oldMinChargePower = currentController.properties.forceChargeMinPower;
    let newMinChargePower = event.detail.value;

    newMinChargePower /= numberOfPhases;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'forceChargeMinPower', value: newMinChargePower }
      ]).then(() => {
        currentController.properties.forceChargeMinPower = newMinChargePower;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        currentController.properties.forceChargeMinPower = oldMinChargePower;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }


  /**
   * Updates the Energy Session Limit 
   *  
   * @param event 
   */
  updateEnergySessionLimit(event: CustomEvent, currentController: EdgeConfig.Component) {
    let oldLimit = currentController.properties.energySessionLimit;
    let newLimit = event.detail.value * 1000;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'energySessionLimit', value: newLimit }
      ]).then(() => {
        currentController.properties.energySessionLimit = newLimit;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        currentController.properties.energySessionLimit = oldLimit;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }

  /**
  * update the state of the toggle which renders the minimum charge power
  * 
  * @param event 
  */
  allowEnergySessionLimit(currentController: EdgeConfig.Component) {
    let oldLimit = currentController.properties['energySessionLimit'];
    let newLimit;

    if (this.edge != null) {
      if (oldLimit > 0) {
        newLimit = 0;
      }
      else {
        newLimit = 20000;
      }
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'energySessionLimit', value: newLimit }
      ]).then(() => {
        currentController.properties.energySessionLimit = newLimit;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        currentController.properties.energySessionLimit = oldLimit;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
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
      ]).then(() => {
        currentController.properties.defaultChargeMinPower = newMinChargePower;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        currentController.properties.defaultChargeMinPower = oldMinChargePower;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }

  /**
   * update the state of the toggle which renders the minimum charge power
   * 
   * @param event 
   * @param phases 
   */
  allowMinimumChargePower(phases: number, currentController: EdgeConfig.Component) {
    let oldMinChargePower = currentController.properties['defaultChargeMinPower'];
    let newMinChargePower = 0;
    if (oldMinChargePower == null || oldMinChargePower == 0) {
      newMinChargePower = phases != undefined ? 1400 * phases : 4200;
    }
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'defaultChargeMinPower', value: newMinChargePower }
      ]).then(() => {
        currentController.properties['defaultChargeMinPower'] = newMinChargePower;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        currentController.properties['defaultChargeMinPower'] = oldMinChargePower;
        this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }

  /**
  * Activates or deactivates the Charging
  * 
  * @param event 
  */
  enableOrDisableCharging(currentController: EdgeConfig.Component) {

    let oldChargingState = currentController.properties.enabledCharging;
    let newChargingState = !oldChargingState;
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'enabledCharging', value: newChargingState }
      ]).then(() => {
        currentController.properties.enabledCharging = newChargingState;
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        currentController.properties.enabledCharging = oldChargingState;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }

  /**
   * Updates the MinChargePower for Renault Zoe Charging Mode if activated in administration component
   */
  updateRenaultZoeConfig() {
    if (this.evcsComponent.properties['minHwCurrent'] == 10000) {

      let oldMinChargePower = this.controller.properties.forceChargeMinPower;
      let maxAllowedChargePower = 10 /* Ampere */ * 230; /* Volt */

      if (oldMinChargePower < maxAllowedChargePower) {
        if (this.edge != null) {
          let newMinChargePower = maxAllowedChargePower;
          this.edge.updateComponentConfig(this.websocket, this.controller.id, [
            { name: 'forceChargeMinPower', value: newMinChargePower }
          ]).then(() => {
            this.controller.properties.forceChargeMinPower = newMinChargePower;
          }).catch(reason => {
            this.controller.properties.forceChargeMinPower = oldMinChargePower;
            console.warn(reason);
          });
        }
      }
    }
  }

  /**
   * Returns the number of Phases or the default 3.
   */
  getNumberOfPhasesOrThree() {
    let numberOfPhases = this.edge.currentData['_value'].channel[this.componentId + "/Phases"];
    numberOfPhases = numberOfPhases == null ? 3 : numberOfPhases;
    return numberOfPhases;
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

  async presentPopover(ev: any) {
    const popover = await this.popoverController.create({
      component: Controller_EvcsPopoverComponent,
      event: ev,
      translucent: true,
      componentProps: {
        controller: this.controller,
        componentId: this.componentId
      }
    });
    return await popover.present();
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: AdministrationComponent,
      componentProps: {
        evcsComponent: this.evcsComponent,
        edge: this.edge
      }
    });
    modal.onDidDismiss().then(() => {
      this.updateRenaultZoeConfig();
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