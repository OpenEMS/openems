import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { WebsocketService } from '../../../service/websocket.service';
import { Device } from '../../../service/device';

@Component({
  selector: 'app-device-overview-energymonitor',
  templateUrl: './energymonitor.component.html'
})
export class DeviceOverviewEnergymonitorComponent {

  private device: Device;

  constructor(
    private websocketService: WebsocketService
  ) { }

  ngOnInit() {
    this.device = this.websocketService.currentDevice;
  }

  /*
    private getThingTag(thing: string): string {
      for (let thing in connection.natures) {
        if (thing in this.device) {
          let n: string[] = connection.natures[thing];
          if (this.contains(n, "FeneconProEss")) {
            ess = "FeneconPro";
          } else if (this.contains(n, "FeneconCommercialAC") || this.contains(n, "FeneconCommercialDC")) {
            ess = "FeneconCommercial";
          }
        }
      }

      // fill primary nature type
      if (thing in this.device.data) {
        let n: string[] = connection.natures[thing];
        let tag: string;
        let title: string = null;
        // Meter
        if (ess == "FeneconPro" && this.contains(n, "AsymmetricMeterNature")) {
          tag = "AsymmetricMeter";
          if (thing == "meter0") {
            title = "Netzzähler";
          } else if (thing == "meter1") {
            title = "PV-Zähler";
          }

        } else if (ess == "FeneconCommercial" && this.contains(n, "SymmetricMeterNature")) {
          tag = "SymmetricMeter";

        } else if (this.contains(n, "SimulatorMeter")) {
          tag = "SimulatorMeter";
          title = "Simulierter Zähler";

        } else if (this.contains(n, "SymmetricMeterNature")) {
          tag = "SymmetricMeter";

          // Ess
        } else if (this.contains(n, "FeneconProEss")) {
          tag = "FeneconProEss";
        } else if (this.contains(n, "FeneconCommercialEss")) {
          tag = "FeneconCommercialEss";
        } else if (this.contains(n, "SimulatorEss")) {
          tag = "SimulatorEss";
          title = "Simuliertes Speichersystem";

        } else {
          console.log("Not implemented: " + JSON.stringify(n));
        }
        this.data[thing]["_thing"] = thing;
        this.data[thing]["_title"] = title;
        this.data[thing]["_tag"] = tag;
      }
    }*/
}