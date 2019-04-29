import { Component, OnInit, HostListener, Input } from '@angular/core';
import { environment } from 'src/environments/openems-backend-dev-local';
import { PopoverController, ModalController } from '@ionic/angular';
import { Router, ActivatedRoute } from '@angular/router';
import { Websocket, ChannelAddress, Service, EdgeConfig, Edge } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { EvcsComponent } from '../evcs.component';
import { InfoPopoverComponent } from './info-popover/info-popover.component';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER';
type Priority = 'CAR' | 'STORAGE';

@Component({
  selector: 'app-evcs-modal',
  templateUrl: './evcs-modal.page.html',
  styleUrls: ['./evcs-modal.page.scss'],
})
export class EvcsModalPage implements OnInit {

  @Input() controller: EdgeConfig.Component;
  @Input() edge: Edge;
  @Input() componentId: number;
  public chargeState: ChargeState;
  private chargePlug: ChargePlug;
  public screenWidth: number = 0;
  public env = environment;

  constructor(
    public websocket: Websocket,
    public router: Router,
    protected translate: TranslateService,
    private modalCtrl: ModalController,
    private popoverController: PopoverController
  ) { }

  ngOnInit() {
    this.getScreenSize();
  }

  cancel() {
    this.modalCtrl.dismiss();
  }

  /**  
  * Updates the Charge-Mode of the EVCS-Controller.
  * 
  * @param event 
  */
  updateChargeMode(event: CustomEvent) {
    let oldChargeMode = this.controller.properties.chargeMode;
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
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'chargeMode', value: newChargeMode }
      ]).then(response => {
        console.log("HIER", response);
        this.controller.properties.chargeMode = newChargeMode;
      }).catch(reason => {
        this.controller.properties.chargeMode = oldChargeMode;
        console.warn(reason);
      });
    }
  }
  /**
   * Changed the Priority between the components of the charging session
   */
  priorityChanged(event: CustomEvent) {
    let oldPriority = this.controller.properties.priority;
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
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'priority', value: newPriority }
      ]).then(response => {
        console.log("HIER", response);
        this.controller.properties.priority = newPriority;
      }).catch(reason => {
        this.controller.properties.priority = oldPriority;
        console.warn(reason);
      });
    }
  }

  /**
   * Updates the Min-Power of force charging
   *
   * @param event
   */
  updateForceMinPower(event: CustomEvent) {
    let oldMinChargePower = this.controller.properties.forceChargeMinPower;
    let newMinChargePower = event.detail.value;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'forceChargeMinPower', value: newMinChargePower }
      ]).then(response => {
        console.log("HIER", response);
        this.controller.properties.forceChargeMinPower = newMinChargePower;
      }).catch(reason => {
        this.controller.properties.forceChargeMinPower = oldMinChargePower;
        console.warn(reason);
      });
    }
  }

  /**
   * Updates the Min-Power of default charging
   *
   * @param event
   */
  updateDefaultMinPower(event: CustomEvent) {
    let oldMinChargePower = this.controller.properties.defaultChargeMinPower;
    let newMinChargePower = event.detail.value;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'defaultChargeMinPower', value: newMinChargePower }
      ]).then(response => {
        console.log("HIER", response);
        this.controller.properties.defaultChargeMinPower = newMinChargePower;
      }).catch(reason => {
        this.controller.properties.defaultChargeMinPower = oldMinChargePower;
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
  allowMinimumChargePower(event: CustomEvent, phases: number) {

    let oldMinChargePower = this.controller.properties.defaultChargeMinPower;

    let newMinChargePower = 0;
    if (oldMinChargePower == null || oldMinChargePower == 0) {
      newMinChargePower = phases != undefined ? 4000 * phases : 4000;
    }
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'defaultChargeMinPower', value: newMinChargePower }
      ]).then(response => {
        console.log("HIER", response);
        this.controller.properties.defaultChargeMinPower = newMinChargePower;
      }).catch(reason => {
        this.controller.properties.defaultChargeMinPower = oldMinChargePower;
        console.warn(reason);
      });
    }
  }

  /**
  * Aktivates or deaktivates the Charging
  * 
  * @param event 
  */
  enableOrDisableCharging(event: CustomEvent) {

    let oldChargingState = this.controller.properties.enabledCharging;
    let newChargingState = !oldChargingState;
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'enabledCharging', value: newChargingState }
      ]).then(response => {
        console.log("HIER", response);
        this.controller.properties.enabledCharging = newChargingState;
      }).catch(reason => {
        this.controller.properties.enabledCharging = oldChargingState;
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
  outputPowerOrState(power: Number, state: number, plug: number) {

    if (power == null || power == 0) {

      this.chargeState = state;
      this.chargePlug = plug;

      if (this.chargePlug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
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

  async presentPopover(ev: any, mode: ChargeMode) {
    console.log("öffne das Popup");
    const popover = await this.popoverController.create({
      component: InfoPopoverComponent,
      event: ev,
      componentProps: {
        chargeMode: mode
      }
    });
    return await popover.present();
  }

  dismissPopover(ev: any) {
    console.log("mouse leaveeed ")
    this.popoverController.dismiss();
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

  @HostListener('window:resize', ['$event'])
  getScreenSize(event?) {
    this.screenWidth = window.innerWidth;
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
