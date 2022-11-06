import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { RegistrationModalComponent } from './modal/modal.component';

@Component({
  selector: 'registration',
  templateUrl: './registration.component.html'
})
export class RegistrationComponent {

  constructor(private modalController: ModalController) { }

  async presentModal() {
    const modal = await this.modalController.create({
      component: RegistrationModalComponent
    });
    return await modal.present();
  }

}
