import { Component } from "@angular/core";

@Component({
  selector: 'eoy-winner',
  template: `
    <ion-grid style="font-size: small; text-align: center; width: 100%">
  <ion-row>
    <ion-text class="full_width">
      Wir haben gewonnen!
      <br>
      <b>
      EOY Audience Award 2023
      </b>
      <hr>
      Vielen Dank, dass Sie uns so zahlreich Ihre Stimme gegeben haben.<br>
      <hr>
      Mit Ihrem starken Support haben FENECON und Franz-Josef Feilmeier zusätzlich zum EY Entrepreneur of the Year Award, auch den Publikumspreis gewonnen.
      <br>
      <hr>
      Wir freuen uns sehr!
      <br>
      Herzlichen Dank
      <br>
      Ihr FENECON Team
    </ion-text>
  </ion-row>
</ion-grid>
  `,
})
export class EoYWinnerAdvertComponent {

}
