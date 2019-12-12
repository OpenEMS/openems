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

  // @Input() private componentId: string;

  public edge: Edge = null;
  public outputChannel: ChannelAddress = null;
  public component: EdgeConfig.Component = null;

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
      let channels = [];
      channels.push(
        new ChannelAddress('_sum', 'EssSoc'),
        new ChannelAddress('_sum', 'GridActivePower'),
        new ChannelAddress('_sum', 'ProductionActivePower'),
      )
      this.edge.subscribeChannels(this.websocket, SinglethresholdComponent.SELECTOR, channels);
    });
  }

  getState(): state {
    if (this.edge != null) {
      if (this.mode == 'OFF') {
        return 'OFF'
      } else if (this.mode == 'ON') {
        return 'ON'
      } else if (this.mode == 'AUTOMATIC') {
        switch (this.inputMode) {
          case 'SOC': {
            if (this.threshold < this.edge.currentData.value.channel['_sum/EssSoc']) {
              return 'ON';
            } else if (this.threshold > this.edge.currentData.value.channel['_sum/EssSoc']) {
              return 'OFF';
            } else {
              return 'OFF';
            }
          }
          case 'GRIDSELL': {
            if (this.edge.currentData.value.channel['_sum/GridActivePower'] * -1 >= 0) {
              if (this.threshold < this.edge.currentData.value.channel['_sum/GridActivePower'] * -1) {
                return 'ON';
              } else if (this.threshold > this.edge.currentData.value.channel['_sum/GridActivePower'] * -1) {
                return 'OFF';
              }
            } else {
              return 'OFF';
            }
          }
          case 'PRODUCTION': {
            if (this.threshold < this.edge.currentData.value.channel['_sum/ProductionActivePower']) {
              return 'ON';
            } else if (this.threshold > this.edge.currentData.value.channel['_sum/ProductionActivePower']) {
              return 'OFF';
            } else {
              return 'OFF';
            }
          }
        }
      }
    }
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: SinglethresholdModalComponent,
      componentProps: {
        edge: this.edge,
        mode: this.mode,
        inputMode: this.inputMode,
        threshold: this.threshold
      }
    });
    modal.onDidDismiss().then(value => {
      this.mode = value.data.mode;
      this.inputMode = value.data.inputMode;
      this.threshold = Number(value.data.threshold)
    })
    return await modal.present();
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, SinglethresholdComponent.SELECTOR);
    }
  }
}
