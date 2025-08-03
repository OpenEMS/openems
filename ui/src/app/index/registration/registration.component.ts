import { Component, inject } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { RegistrationModalComponent } from "./modal/modal.component";

@Component({
  selector: "registration",
  templateUrl: "./registration.component.html",
  standalone: false,
})
export class RegistrationComponent {
  private modalController = inject(ModalController);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);


  constructor() { }

  async presentModal() {
    const modal = await this.modalController.create({
      component: RegistrationModalComponent,
    });
    return await modal.present();
  }

}
