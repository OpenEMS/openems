import { Component, OnInit } from '@angular/core';
import { Http, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';
import { DataService } from '../../../data/data.service';

@Component({
  selector: 'app-monitor-test-current',
  templateUrl: './universal-current.component.html'
})
export class MonitorUniversalCurrentComponent implements OnInit {
  private natures: Object;
  private data: Object;
  private error: string;

  constructor(private chatService: DataService) {
  }

  ngOnInit() {
    this.chatService.messages.subscribe(message => {
      this.error = null;
      if ("natures" in message) {
        this.natures = message.natures;
        /*for (let thing in this.natures) {
          console.log(thing + ": " + this.natures[thing]);
        }*/
      }

      if ("data" in message) {
        this.data = message.data;

        // filter general system type
        var ess = null;
        for (let thing in this.natures) {
          if (thing in this.data) {
            let n: string[] = this.natures[thing];
            if(this.contains(n, "FeneconProEss")) {
              ess = "FeneconPro";
            } else if(this.contains(n, "FeneconCommercialAC") || this.contains(n, "FeneconCommercialDC")) {
              ess = "FeneconCommercial";
            }
          }
        }
        
        // fill primary nature type
        for (let thing in this.natures) {
          if (thing in this.data) {
            let n: string[] = this.natures[thing];
            let tag: string;
            let title: string = null;
            // Meter
            if(ess == "FeneconPro" && this.contains(n, "AsymmetricMeterNature")) {
              tag = "AsymmetricMeter";
              if(thing == "meter0") {
                title = "Netzzähler";
              } else if(thing == "meter1") {
                title = "PV-Zähler";
              }

            } else if(ess == "FeneconCommercial" && this.contains(n, "SymmetricMeter")) {
              tag = "SymmetricMeter";
            } else if(this.contains(n, "SimulatorMeter")) {
              //tag = "SimulatorMeter";
              tag = "SymmetricMeter";
              title = "Simulierter Zähler";

            // Ess
            } else if(this.contains(n, "FeneconProEss")) {
              tag = "FeneconProEss";
            } else if(this.contains(n, "FeneconCommercialEss")) {
              tag = "FeneconCommercialEss";
            } else if(this.contains(n, "SimulatorEss")) {
              //tag = "SimulatorEss";
              tag = "FeneconCommercialEss";
              title = "Simuliertes Speichersystem";
            }
            this.data[thing]["_thing"] = thing;
            this.data[thing]["_title"] = title;
            this.data[thing]["_tag"] = tag;
          }
        }
      }
    }, error => {
      this.data = null;
      this.error = "Herstellen der Verbindung ist nicht möglich.";
    });
  }

  private contains(array: string[], tag: string): boolean {
    return array.indexOf(tag) != -1
  }
}
