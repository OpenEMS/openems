import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../../shared/shared';
import { ModalComponent } from './modal/modal.component';

@Component({
  selector: 'fixdigitaloutput',
  templateUrl: './fixdigitaloutput.component.html'
})
export class FixDigitalOutputComponent {

  private static readonly SELECTOR = "fixdigitaloutput";

  @Input() private componentId: string;

  public edge: Edge = null;
  public controller: EdgeConfig.Component = null;
  public outputChannel: string = null;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    private modalController: ModalController
  ) { }

  ngOnInit() {
    // Subscribe to CurrentData
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.controller = config.components[this.componentId];
        this.outputChannel = this.controller.properties['outputChannelAddress']
        edge.subscribeChannels(this.websocket, FixDigitalOutputComponent.SELECTOR + this.componentId, [
          ChannelAddress.fromString(this.outputChannel)
        ]);
      });
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, FixDigitalOutputComponent.SELECTOR + this.componentId);
    }
  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: ModalComponent,
      componentProps: {
        controllerId: this.controller.id
      }
    });
    return await modal.present();
  }
}
