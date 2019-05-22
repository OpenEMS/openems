import { Component, Input, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';
import { EvcsModalPage } from './evcs-modal/evcs-modal.page';
import { componentFactoryName } from '@angular/compiler';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER';
type Priority = 'CAR' | 'STORAGE';

@Component({
  selector: 'evcs',
  templateUrl: './evcs.component.html'
})
export class EvcsComponent {

  private static readonly SELECTOR = "evcs";


  @Input() private componentId: string;

  public edge: Edge = null;
  public controller: EdgeConfig.Component = null;
  public chargeState: ChargeState;
  private chargePlug: ChargePlug;
  public screenWidth: number = 0;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    protected translate: TranslateService,
    public modalController: ModalController
  ) { }

  ngOnInit() {

    // Subscribe to CurrentData
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      edge.subscribeChannels(this.websocket, EvcsComponent.SELECTOR + this.componentId, [
        // Evcs
        new ChannelAddress(this.componentId, 'ChargePower'),
        new ChannelAddress(this.componentId, 'HardwarePowerLimit'),
        new ChannelAddress(this.componentId, 'Phases'),
        new ChannelAddress(this.componentId, 'Plug'),
        new ChannelAddress(this.componentId, 'Status'),
        new ChannelAddress(this.componentId, 'State'),
        new ChannelAddress(this.componentId, 'EnergySession'),
        new ChannelAddress(this.componentId, 'MinimumPower'),
        new ChannelAddress(this.componentId, 'MaximumPower')
      ]);

    });

    // Gets the Controller for the given EVCS-Component.
    this.service.getConfig().then(config => {
      let controllers = config.getComponentsByFactory("Controller.Evcs");
      for (let controller of controllers) {
        let properties = controller.properties;
        if ("evcs.id" in properties && properties["evcs.id"] === this.componentId) {
          // this 'controller' is the Controller responsible for this EVCS
          this.controller = controller;
          return;
        }
      }
    });

  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EvcsComponent.SELECTOR + this.componentId);
    }
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: EvcsModalPage,
      componentProps: {
        controller: this.controller,
        edge: this.edge,
        componentId: this.componentId
      }
    });
    return await modal.present();
  }

  /**
  * Aktivates or deaktivates the Charging
  * 
  * @param event 
  */
  protected enableOrDisableCharging(event: CustomEvent) {

    let oldChargingState = this.controller.properties.enabledCharging;
    let newChargingState = !oldChargingState;
    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { name: 'enabledCharging', value: newChargingState }
      ]).then(response => {
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

      if (this.chargePlug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED)
        return this.translate.instant('Edge.Index.Widgets.EVCS.CableNotConnected');

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
