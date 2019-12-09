import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Websocket, ChannelAddress, EdgeConfig } from 'src/app/shared/shared';
import { HttpClient } from '@angular/common/http';
import { CorrentlyModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';

@Component({
  selector: CorrentlyComponent.SELECTOR,
  templateUrl: './corrently.component.html'
})
export class CorrentlyComponent {

  private static readonly SELECTOR = "corrently";

  public edge: Edge = null;
  private config: EdgeConfig = null;
  public zipCode: string;

  constructor(
    public service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
  ) { }

  ngOnInit() {
    let channels = [];
    this.service.getConfig().then(config => {
      this.config = config;
    })
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      channels.push(
        new ChannelAddress('corrently0', 'BestHourEpochtime'),
        new ChannelAddress('corrently0', 'BestHourGsi'),
      )
      this.edge.subscribeChannels(this.websocket, CorrentlyComponent.SELECTOR, channels);
    });
  }

  public getEpochTime() {
    return new Date(this.edge.currentData['_value'].channel['corrently0/BestHourEpochtime'] * 1000)
      .toLocaleString('de-DE', { year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).toString() + ' Uhr';
  }

  pickZipCode() {
    if (this.edge != null) {
      let oldZipCode = this.config.getComponent("corrently0").properties["zipCode"]
      this.edge.updateComponentConfig(this.websocket, this.config.getComponent("corrently0").id, [
        { name: 'zipCode', value: this.zipCode }
      ]).then(() => {
        this.config.getComponent("corrently0").properties.zipCode = this.zipCode;
        this.service.toast('Änderung übernommen', 'success');
      }).catch(reason => {
        this.config.getComponent("corrently0").properties.zipCode = oldZipCode;
        this.service.toast('Änderung fehlgeschlagen' + '\n' + reason, 'danger');
        console.warn(reason);
      });
    }
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, CorrentlyComponent.SELECTOR);
    }
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: CorrentlyModalComponent,
      cssClass: 'wide-modal'
    });
    return await modal.present();
  }
}
