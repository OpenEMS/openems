import { Component, OnInit, Input } from '@angular/core';
import { PopoverController, ModalController } from '@ionic/angular';
import { Websocket, Service, EdgeConfig, Edge } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { EvcsPopoverComponent } from './evcs-popover/evcs-popover.page';
import { environment } from 'src/environments';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER' | 'OFF';
type Priority = 'CAR' | 'STORAGE';

@Component({
  selector: 'evcs-modal',
  templateUrl: './evcs-modal.page.html'
})
export class EvcsModalComponent implements OnInit {

  @Input() edge: Edge;
  @Input() controller: EdgeConfig.Component;
  @Input() getState: () => String;
  @Input() public componentId: string;

  //boolean value to determine correct info text in popover
  public isPrioritization: boolean = null;
  public isCapacity: boolean = null;
  //chargeMode value to determine third state 'Off' (OFF State is not available in EDGE)
  public chargeMode: ChargeMode = null;

  constructor(
    protected service: Service,
    public websocket: Websocket,
    protected translate: TranslateService,
    private modalCtrl: ModalController,
    public popoverController: PopoverController
  ) { }

  ngOnInit() {
    if (this.controller.properties.enabledCharging) {
      this.chargeMode = this.controller.properties.chargeMode;
    }
    else {
      this.chargeMode = 'OFF';
    }
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
        { name: 'enabledCharging', value: newEnabledCharging },
      ]).then(() => {
        currentController.properties.enabledCharging = newEnabledCharging;
        currentController.properties.chargeMode = newChargeMode;
      }).catch(reason => {
        currentController.properties.enabledCharging = oldEnabledCharging;
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
      ]).then(() => {
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
      ]).then(() => {
        currentController.properties.forceChargeMinPower = newMinChargePower;
      }).catch(reason => {
        currentController.properties.forceChargeMinPower = oldMinChargePower;
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
      }).catch(reason => {
        currentController.properties.energySessionLimit = oldLimit;
      })
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
      }).catch(reason => {
        currentController.properties.defaultChargeMinPower = oldMinChargePower;
        console.warn(reason);
      });
    }
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
      ]).then(() => {
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
  enableOrDisableCharging(currentController: EdgeConfig.Component) {

    let oldChargingState = currentController.properties.enabledCharging;
    let newChargingState = !oldChargingState;
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, currentController.id, [
        { name: 'enabledCharging', value: newChargingState }
      ]).then(() => {
        currentController.properties.enabledCharging = newChargingState;
      }).catch(reason => {
        currentController.properties.enabledCharging = oldChargingState;
        console.warn(reason);
      });
    }
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

  async presentPopover(ev: any) {
    const popover = await this.popoverController.create({
      component: EvcsPopoverComponent,
      event: ev,
      translucent: true,
      componentProps: {
        isPrioritization: this.isPrioritization,
        isCapacity: this.isCapacity,
        controller: this.controller,
        componentId: this.componentId
      }
    });
    return await popover.present();
  }
}


