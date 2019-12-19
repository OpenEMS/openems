import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { SinglethresholdModalComponent } from './modal/modal.component';
import { TranslateService } from '@ngx-translate/core';

type mode = 'ON' | 'AUTOMATIC' | 'OFF';
type inputMode = 'SOC' | 'GRIDSELL' | 'PRODUCTION' | 'OTHER'
type state = 'ON' | 'OFF'

@Component({
  selector: SinglethresholdComponent.SELECTOR,
  templateUrl: './singlethreshold.component.html'
})
export class SinglethresholdComponent {

  private static readonly SELECTOR = "singlethreshold";

  @Input() private componentId: string;

  public edge: Edge = null;

  public controller: EdgeConfig.Component = null;
  public inputChannel: ChannelAddress;
  public outputChannel: ChannelAddress;


  public threshold: number = 34;
  public mode: mode = 'ON';
  public inputMode: inputMode = 'SOC'

  constructor(
    public service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    protected translate: TranslateService,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.controller = config.getComponent(this.componentId);
        this.outputChannel = ChannelAddress.fromString(
          this.controller.properties['outputChannelAddress']);
        this.inputChannel = ChannelAddress.fromString(
          this.controller.properties['inputChannelAddress']);
        edge.subscribeChannels(this.websocket, SinglethresholdComponent.SELECTOR + this.componentId, [
          this.inputChannel,
          this.outputChannel
        ]);
        console.log("componento", config.getComponent(this.componentId), "inputChannel", ChannelAddress.fromString(
          this.controller.properties['inputChannelAddress']))
      })
    });
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: SinglethresholdModalComponent,
      componentProps: {
        controller: this.controller,
        edge: this.edge,
        outputChannel: this.outputChannel,
        inputChannel: this.inputChannel
      }
    });
    console.log("controller", this.controller)
    return await modal.present();
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, SinglethresholdComponent.SELECTOR);
    }
  }
}
