import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { SinglethresholdModalComponent } from './modal/modal.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: SinglethresholdComponent.SELECTOR,
  templateUrl: './singlethreshold.component.html'
})
export class SinglethresholdComponent {

  private static readonly SELECTOR = "singlethreshold";

  @Input() private componentId: string = '';

  public edge: Edge | null = null;
  public config: EdgeConfig | null = null;

  public component: EdgeConfig.Component | null = null;
  public inputChannel: ChannelAddress | null = null;
  public outputChannel: ChannelAddress | null = null;

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    protected translate: TranslateService,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      if (edge != null) {
        this.edge = edge;
        this.service.getConfig().then(config => {
          this.config = config;
          this.component = config.getComponent(this.componentId);
          this.outputChannel = ChannelAddress.fromString(
            this.component.properties['outputChannelAddress']);
          this.inputChannel = ChannelAddress.fromString(
            this.component.properties['inputChannelAddress']);
          edge.subscribeChannels(this.websocket, SinglethresholdComponent.SELECTOR + this.componentId, [
            this.inputChannel,
            this.outputChannel
          ]);
        })
      }
    });
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: SinglethresholdModalComponent,
      componentProps: {
        component: this.component,
        config: this.config,
        edge: this.edge,
        outputChannel: this.outputChannel,
        inputChannel: this.inputChannel
      }
    });
    return await modal.present();
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, SinglethresholdComponent.SELECTOR);
    }
  }
}
