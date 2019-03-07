import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { Timestamp } from 'rxjs/internal/operators/timestamp';

type ChargeMode = 'FORCE_CHARGE' | 'DEFAULT';

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

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    protected translate: TranslateService,
  ) { }

  ngOnInit() {
    // Subscribe to CurrentData
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge;
      edge.subscribeChannels(this.websocket, EvcsComponent.SELECTOR + this.componentId, [
        // Evcs
        new ChannelAddress(this.componentId, 'ChargePower'),
        new ChannelAddress(this.componentId, 'HardwarePowerLimit'),
        new ChannelAddress(this.componentId, 'Phases'),
        new ChannelAddress(this.componentId, 'Plug'),
        new ChannelAddress(this.componentId, 'Status'),
        new ChannelAddress(this.componentId, 'State'),
        new ChannelAddress(this.componentId, 'EnergySession')
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
      case 'DEFAULT':
        newChargeMode = 'DEFAULT';
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
   * Updates the Min-Power of force charging
   *
   * @param event
   */
  updateForceMinPower(event: CustomEvent) {
    let oldMinChargePower = this.controller.properties.forceChargeMinPower;
    let newMinChargePower = event.detail.value;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { property: 'forceChargeMinPower', value: newMinChargePower }
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
        { property: 'defaultChargeMinPower', value: newMinChargePower }
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
        { property: 'defaultChargeMinPower', value: newMinChargePower }
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

      if(this.chargePlug != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED)        
        return this.translate.instant('Edge.Index.Widgets.EVCS.CableNotConnected');

      switch(this.chargeState){
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
   * Round to 100
   * 
   * @param i 
   */
  formatNumber(i: number) {
    return Math.round(i / 100) * 100;
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
  UNDEFINED = -1,                         //Undefined
  UNPLUGGED,                              //Unplugged
  PLUGGED_ON_EVCS,                        //Plugged on EVCS
  PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
  PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
  PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7  //Plugged on EVCS and on EV and locked

}