import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input } from '@angular/core';
import { FixDigitalOutputModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';
import { CurrentData } from 'src/app/shared/edge/currentdata';

@Component({
  selector: 'fixdigitaloutput',
  templateUrl: './fixdigitaloutput.component.html'
})
export class FixDigitalOutputComponent {

  private static readonly SELECTOR = "fixdigitaloutput";

  public selector = 'fixdigitaloutput';
  /** componentId needs to be set to get the components */
  @Input() private componentId: string;

  public edge: Edge = null;
  public component: EdgeConfig.Component = null;
  public outputChannel: string;
  public state: string;
  public channelAddress: ChannelAddress[] = [];

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
        this.service.getConfig().then(config => {
          this.component = config.components[this.componentId];
          this.outputChannel = this.component.properties['outputChannelAddress'];
          /** Subscribe on CurrentData to get the channel */
          this.edge.currentData.subscribe(currentData => {
            /** Prooving state variable with following content setting */
            let channel
            channel = currentData.channel[this.outputChannel];
            if (channel == null) {
              this.state = '-----';
            } else if (channel == 1) {
              this.state = 'on'
            } else if (channel == 0) {
              this.state = 'off'
            }
          });
          /** pushing the ChannelAddress for the OutputChannel in channelAddresses[]
        *  in order to send it later to FlatWidget */
          this.channelAddress.push(ChannelAddress.fromString(this.outputChannel));
        });
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
