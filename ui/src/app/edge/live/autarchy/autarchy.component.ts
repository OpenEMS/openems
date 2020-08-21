import { ActivatedRoute } from '@angular/router';
import { AutarchyModalComponent } from './modal/modal.component';
import { Component } from '@angular/core';
import { Edge, Service } from '../../../shared/shared';
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
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route)
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: AutarchyModalComponent,
    });
    return await modal.present();
  }
}
