import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { BydModalComponent } from './modal/modal.component';

@Component({
  selector: BydComponent.SELECTOR,
  templateUrl: './byd.component.html'
})
export class BydComponent {

  private static readonly SELECTOR = "byd";

  private edge: Edge = null;

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route)
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: BydModalComponent,
    });
    return await modal.present();
  }
}
