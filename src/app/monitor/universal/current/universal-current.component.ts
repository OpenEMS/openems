import { Component, OnInit, OnDestroy } from '@angular/core';
import { Http, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { ConnectionService, Connection, ActiveConnection } from '../../../service/connection.service';

@Component({
  selector: 'app-monitor-test-current',
  templateUrl: './universal-current.component.html'
})
export class MonitorUniversalCurrentComponent implements OnInit, OnDestroy {
  private data: Object;
  private error: string;
  private subscription: ISubscription;

  constructor(
    private router: Router,
    private connectionService: ConnectionService) {
  }

  ngOnInit() {
    var connection: Connection = this.connectionService.getDefault();
    if (!(connection instanceof ActiveConnection)) {
      this.router.navigate(['login']);

    } else {
      connection.subject.subscribe((message: any) => {
        if ("data" in message) {
          var msg: any = JSON.parse(message.data);

          if ("authenticate" in msg && "failed" in msg.authenticate && msg.authenticate.failed == true) {
            // Authentication failed
            this.data = null;
            this.error = "Authentifizierung fehlgeschlagen.";
            setTimeout(() => this.router.navigate(['login']), 1000);
            return;
          }

          // Data
          if ("data" in msg) {
            this.data = msg.data;
            this.error = null;

            // filter general system type
            var ess = null;
            if (connection instanceof ActiveConnection) {
              for (let thing in connection.natures) {
                if (thing in this.data) {
                  let n: string[] = connection.natures[thing];
                  if (this.contains(n, "FeneconProEss")) {
                    ess = "FeneconPro";
                  } else if (this.contains(n, "FeneconCommercialAC") || this.contains(n, "FeneconCommercialDC")) {
                    ess = "FeneconCommercial";
                  }
                }
              }

              // fill primary nature type
              for (let thing in connection.natures) {
                if (thing in this.data) {
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
              }
            }
          }
        }
      }, (error: any) => {
        this.data = null;
        this.error = "Herstellen der Verbindung ist nicht möglich.";
        setTimeout(() => this.router.navigate(['login']), 1000);
      }, (/* complete */) => {
        this.data = null;
        setTimeout(() => this.router.navigate(['login']), 1000);
      });
    }
  }

  ngOnDestroy() {
    //TODO this.unsubscribeNatures();
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  private contains(array: string[], tag: string): boolean {
    return array.indexOf(tag) != -1
  }
}
