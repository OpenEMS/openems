import { Component } from "@angular/core";

@Component({
  selector: 'modal',
  template: `
  <oe-modal [title]="'Bis zu 30 % Stromkosten sparen'">
    <ion-row lines="full"
        style="line-height: 1.5em; padding-left: 1em; padding-right: 1em;">
        <ion-col size="12" class="ion-align-self-center">
            <p>Seit November 2023 läuft der BETA-Test für die FEMS App Dynamischer Stromtarif.
              Viele Pilotkunden haben bereits die "Aktive Beladung aus dem Netz (BETA-Test)" aktiviert
              und laden damit prognosebasiert den Speicher bei niedrigen Börsenstrompreisen aus dem Netz nach
               - in der Regel immer dann, wenn viel Grünstrom im Netz verfügbar ist.
            </p>
        </ion-col>

        <ion-col size="12">
          <b>
          Wie viel Potenzial steckt darin?
          </b>
        </ion-col>
        <ion-col size="12">
            <p>
          Unsere Auswertungen der Systeme zeigen, dass allein im Dezember 2023 die meisten Kunden je nach Speichergröße und Verbrauchsprofil
          Einsparungen von 10 bis 40 € pro System erzielen konnten.
          Da dynamische Tarife auch insgesamt in der Regel günstiger als klassisch-starre Stromtarife sind,
          betrug die Einsparung der meisten Kunden im Dezember insgesamt zwischen 40 und 140 € (Vergleichswert: Brutto-Strompreis von 32 Cent/kWh).
            </p>
        </ion-col>

        <ion-col size="12">
          <b>
          Verläuft der BETA-Test erfolgreich?
          </b>
        </ion-col>
        <ion-col size="12">
          <p>
            Ja: Wir konnten durch die vielen unterschiedlichen Anlagenkonfigurationen
            (Batteriekapazitäten, Größe der PV-Anlage, Verbraucher wie E-Autos, Wärmepumpen und Infrarotheizungen, etc.)
            die Algorithmen bereits deutlich verbessern.
          </p>
          <p>
            Weitere Details und Erklärungen zur FEMS App Dynamischer Stromtarif finden Sie in der <a href="https://docs.fenecon.de/_/de/fems/fems-app/OEM_App_TOU.html" target="_blank">neuen Anleitung</a>.
          </p>
        </ion-col>

        <ion-col size="12">
          <b>
          Beim Umstieg auf einen dynamischen Tarif 50 € Bonus erhalten
          </b>
        </ion-col>
        <ion-col size="12">
          <p>
          Smart-Meter sind eine wichtige Voraussetzung für dynamische Stromtarife.
          Leider läuft der Rollout in Deutschland aktuell noch schleppend an.
          Tibber bietet hier einen leichten Einstieg mit dem Tibber Pulse.
          Mit <a href="https://tibber.com/de/invite/FENECON" target="_blank">diesem Link</a> sichern Sie sich einen 50 € Gutschein für den Tibber Store.
          </p>
        </ion-col>
      </ion-row>
</oe-modal>
  `,
})
export class ModalComponent { }
