import { ActivatedRoute } from '@angular/router';
import { AutarchyModalComponent } from './modal/modal.component';
import { ChannelAddress, Edge, Service, Websocket } from '../../../shared/shared';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
  selector: AutarchyComponent.SELECTOR,
  templateUrl: './autarchy.component.html'
})
export class AutarchyComponent {

  private static readonly SELECTOR = "autarchy";

  private edge: Edge = null;

  constructor(
    private route: ActivatedRoute,
    private websocket: Websocket,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      edge.subscribeChannels(this.websocket, AutarchyComponent.SELECTOR, [
        // Grid
        new ChannelAddress('_sum', 'GridActivePower'),
        // Consumption
        new ChannelAddress('_sum', 'ConsumptionActivePower')
      ]);
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, AutarchyComponent.SELECTOR);
    }
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: AutarchyModalComponent,
    });
    return await modal.present();
  }
}
