import { ActivatedRoute } from '@angular/router';
import { EvcsUpgradeModalComponent } from './modal/modal.component';
import { Component } from '@angular/core';
import { Edge, Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
  selector: EvcsUpgradeComponent.SELECTOR,
  templateUrl: './evcsupgrade.component.html'
})
export class EvcsUpgradeComponent {

  private static readonly SELECTOR = "evcsupgrade";

  private edge: Edge = null;

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    })
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: EvcsUpgradeModalComponent,
      componentProps: {
        edge: this.edge,
      }
    });
    return await modal.present();
  }
}
