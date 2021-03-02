import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { LukasModalComponent } from './modal/modal.component';

@Component({
  selector: LukasComponent.SELECTOR,
  templateUrl: './lukas.component.html'
})
export class LukasComponent {

  private static readonly SELECTOR = "lukas";

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
      component: LukasModalComponent,
    });
    return await modal.present();
  }
}
