import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';
import { EvcsModalComponent } from './modal/modal.page';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER';

@Component({
  selector: 'evcs',
  templateUrl: './evcs.component.html'
})
export class EvcsComponent {

  private static readonly SELECTOR = "evcs";

  @Input() public componentId: string;

  public edge: Edge = null;
  public controller: EdgeConfig.Component = null;
  public evcsComponent: EdgeConfig.Component = null;
  public chargeMode: ChargeMode = null;

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
        new ChannelAddress(this.componentId, 'MinimumHardwarePower'),
        new ChannelAddress(this.componentId, 'MaximumHardwarePower')
      ]);

      // Gets the Controller & Component for the given EVCS-Component.
      this.service.getConfig().then(config => {
        let controllers = config.getComponentsByFactory("Controller.Evcs");
        this.evcsComponent = config.getComponent(this.componentId);
        for (let controller of controllers) {
          let properties = controller.properties;
          if ("evcs.id" in properties && properties["evcs.id"] === this.componentId) {
            this.controller = controller;
          }
        }
      });
    });
  }

  /**
   * Returns the state of the EVCS
   * 
   * @param state 
   * @param plug 
   * 
   */
  getState(state: number, plug: number) {
    if (this.controller.properties.enabledCharging != null && this.controller.properties.enabledCharging == false) {
      return this.translate.instant('Edge.Index.Widgets.EVCS.ChargingStationDeactivated');
    }
    let chargeState = state;
    let chargePlug = plug;

    if (chargePlug == null) {
      return this.translate.instant('Edge.Index.Widgets.EVCS.NotCharging');
    } else if (chargePlug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
      return this.translate.instant('Edge.Index.Widgets.EVCS.CableNotConnected');
    }
    switch (chargeState) {
      case ChargeState.STARTING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.Starting');
      case ChargeState.UNDEFINED:
      case ChargeState.ERROR:
        return this.translate.instant('Edge.Index.Widgets.EVCS.Error');
      // if the car is not charging but would be ready to charge, the car is fully charged (keba logic dependency)
      case ChargeState.READY_FOR_CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.CarFull');
      case ChargeState.NOT_READY_FOR_CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.NotReadyForCharging');
      case ChargeState.AUTHORIZATION_REJECTED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.NotCharging');
      case ChargeState.CHARGING:
        return this.translate.instant('Edge.Index.Widgets.EVCS.Charging');
      case ChargeState.ENERGY_LIMIT_REACHED:
        return this.translate.instant('Edge.Index.Widgets.EVCS.ChargeLimitReached');
    }
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: EvcsModalComponent,
      componentProps: {
        controller: this.controller,
        edge: this.edge,
        componentId: this.componentId,
        evcsComponent: this.evcsComponent,
        getState: this.getState
      }
    });
    return await modal.present();
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, EvcsComponent.SELECTOR + this.componentId);
    }
  }
}

enum ChargeState {
  UNDEFINED = -1,           //Undefined
  STARTING,                 //Starting
  NOT_READY_FOR_CHARGING,   //Not ready for Charging e.g. unplugged, X1 or "ena" not enabled, RFID not enabled,...
  READY_FOR_CHARGING,       //Ready for Charging waiting for EV charging request
  CHARGING,                 //Charging
  ERROR,                    //Error
  AUTHORIZATION_REJECTED,    //Authorization rejected
  ENERGY_LIMIT_REACHED
}

enum ChargePlug {
  UNDEFINED = -1,                           //Undefined
  UNPLUGGED,                                //Unplugged
  PLUGGED_ON_EVCS,                          //Plugged on EVCS
  PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
  PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
  PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7  //Plugged on EVCS and on EV and locked
}
