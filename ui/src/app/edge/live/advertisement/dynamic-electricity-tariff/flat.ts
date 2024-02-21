import { Component } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { ModalComponent } from "./modal";

@Component({
  selector: 'dynamic-electricity-tariff',
  template: `
    <ion-grid style="font-size: small; text-align: center; width: 100%; cursor: pointer;" button (click)="presentModal()">
  <ion-row>
    <ion-text class="full_width">
    Machen Sie es wie die Pilotkunden der FEMS App Dynamischer Stromtarif: sparen Sie bis zu 30 % Ihrer Stromkosten und nutzen Sie vermehrt grünen Netzstrom.
      <br>
      <hr>
    Jetzt neu für FENECON Home 10, 20 & 30.
    </ion-text>
  </ion-row>
</ion-grid>
  `,
})
export class FlatComponent {

  constructor(
    private modalCtrl: ModalController,
  ) { }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: ModalComponent,
    });
    return await modal.present();
  }
}
