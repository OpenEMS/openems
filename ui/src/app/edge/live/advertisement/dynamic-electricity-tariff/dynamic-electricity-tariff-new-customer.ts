import { Component } from "@angular/core";

@Component({
  selector: 'dynamic-electricity-tariff-new-customer',
  template: `
    <ion-grid style="font-size: small; text-align: center; width: 100%">
  <ion-row>
    <ion-text class="full_width">
    FEMS App <b>Dynamischer Stromtarif</b> - jetzt neue App-Features ausprobieren!
      <br>
      <hr>
      Weil wir uns so freuen, dass wir gleich zwei EOY Awards gewinnen konnten, verschenken wir 100 kostenlose Lizenzen der neuen FEMS App.<br>
      <hr>
      Einer der ersten 100 Anwender sein und die BETA-Version kostenlos testen:
      <br>
      <hr>
      <a target="_blank"
        href="https://fenecon.de/dynamische-stromtarife/">Jetzt anmelden</a>
    </ion-text>
  </ion-row>
</ion-grid>
  `,
})
export class DynamicElectricityTarifAdvertForNewCustomerComponent { }
