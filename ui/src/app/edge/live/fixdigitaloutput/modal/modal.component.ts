import { Component, Input, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket } from '../../../../shared/shared';

@Component({
  selector: 'fixdigitaloutput-modal',
  templateUrl: './modal.component.html'
})
export class ModalComponent implements OnInit {

  @Input() controllerId: string;

  public state: 'on' | 'off' | 'disabled' = null;
  public alias: string = "";

  private edge: Edge = null;

  constructor(
    public websocket: Websocket,
    private modalCtrl: ModalController,
    private service: Service
  ) { }

  ngOnInit() {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      let controller = config.components[this.controllerId];
      this.alias = controller.alias;
      if (controller.isEnabled) {
        if (controller.properties['isOn']) {
          this.state = 'on';
        } else {
          this.state = 'off';
        }
      } else {
        this.state = 'disabled';
      }
    });
  }

  setValue(event: CustomEvent) {
    let nextState = event.detail.value;
    if (this.state == nextState) {
      // ignore
      return;
    }

    let properties: { name: string, value: any }[] = null;
    switch (event.detail.value) {
      case 'on':
        properties = [
          { name: 'enabled', value: true },
          { name: 'isOn', value: true },
        ];
        break;

      case 'off':
        properties = [
          { name: 'enabled', value: true },
          { name: 'isOn', value: false }
        ];
        break;

      case 'disable':
        properties = [
          { name: 'enabled', value: false }
        ];
        break;
    }

    if (properties == null) {
      return;
    }

    this.edge.updateComponentConfig(
      this.websocket, this.controllerId, properties
    ).then(response => {
      this.service.toast("Successfully updated " + this.controllerId + ".", 'success');
      this.state = nextState;
    }).catch(reason => {
      this.service.toast("Error updating " + this.controllerId + ":" + reason.error.message, 'danger');
      this.state = this.state;
    });
  }
}
