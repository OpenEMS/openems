import { Component } from "@angular/core";

@Component({
  selector: 'dynamic-electricity-tariff-existing-customer',
  template: `
    <ion-grid style="font-size: small; text-align: center; width: 100%">
  <ion-row>
    <ion-text class="full_width">
    Für Sie kostenlos: Jetzt die neuen App-Features ausprobieren!
      <br>
      <hr>
      Zeitvariabler Stromtarif wird zu Dynamischer Stromtarif.<br>
      <hr>
      Erfahren Sie
      <a target="_blank"
        href="https://fenecon.de/dynamische-stromtarife/">hier</a>
      mehr!
    </ion-text>
  </ion-row>
</ion-grid>
  `,
})
export class DynamicElectricityTarifAdvertForExistingCustomerComponent { }
