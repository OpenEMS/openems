import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket } from '../../../../shared/shared';
import { MiniupgradeModalComponent } from './modal/modal.component';

@Component({
  selector: MiniupgradeComponent.SELECTOR,
  templateUrl: './miniupgrade.component.html'
})
export class MiniupgradeComponent {

  private static readonly SELECTOR = "miniupgrade";

  private edge: Edge = null;

  constructor(
    public service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });
  }

  ngOnDestroy() {
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: MiniupgradeModalComponent,
      componentProps: {
        edge: this.edge,
      }
    });
    return await modal.present();
  }
}
