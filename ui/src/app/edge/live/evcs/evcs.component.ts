import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input } from '@angular/core';
import { EvcsModalComponent } from './modal/modal.page';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';

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
    protected translate: TranslateService,
    public modalController: ModalController,
  ) { }

  ngOnInit() {
    // Subscribe to CurrentData
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
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
      edge.subscribeChannels(this.websocket, EvcsComponent.SELECTOR + this.componentId, [
        // Evcs
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
      ]);

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
