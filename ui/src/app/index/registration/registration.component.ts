import { Component } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { RegistrationModalComponent } from "./modal/MODAL.COMPONENT";

@Component({
  selector: "registration",
  templateUrl: "./REGISTRATION.COMPONENT.HTML",
  standalone: false,
})
export class RegistrationComponent {

  constructor(private modalController: ModalController) { }

  async presentModal() {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: RegistrationModalComponent,
    });
    return await MODAL.PRESENT();
  }

}
