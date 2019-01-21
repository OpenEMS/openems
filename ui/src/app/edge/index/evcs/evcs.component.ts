import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UpdateComponentConfigRequest } from '../../../shared/jsonrpc/request/updateComponentConfigRequest';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';

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

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    // Subscribe to CurrentData
    this.service.setCurrentEdge(this.route).then(edge => {
      this.edge = edge;
      edge.subscribeChannels(this.websocket, EvcsComponent.SELECTOR, [
        // Ess
        new ChannelAddress(this.componentId, 'ChargePower')
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
      this.edge.unsubscribeChannels(this.websocket, EvcsComponent.SELECTOR);
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
    if (event.detail.checked) {
      newChargeMode = 'FORCE_CHARGE';
    } else {
      newChargeMode = 'DEFAULT';
    }

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.controller.id, [
        { property: 'chargeMode', value: newChargeMode }
      ]).then(response => {
        console.log(response);
        this.controller.properties.chargeMode = newChargeMode;
      }).catch(reason => {
        this.controller.properties.chargeMode = oldChargeMode;
        console.warn(reason);
      });
    }
  }
}
