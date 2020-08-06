import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input } from '@angular/core';
import { FixDigitalOutputModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';

@Component({
  selector: 'fixdigitaloutput',
  templateUrl: './fixdigitaloutput.component.html'
})
export class FixDigitalOutputComponent {

  private static readonly SELECTOR = "fixdigitaloutput";

  @Input() private componentId: string;

  public edge: Edge = null;
  public component: EdgeConfig.Component = null;
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
        this.component = config.components[this.componentId];
        this.outputChannel = this.component.properties['outputChannelAddress']
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
      component: FixDigitalOutputModalComponent,
      componentProps: {
        component: this.component,
        edge: this.edge
      }
    });
    return await modal.present();
  }
}
