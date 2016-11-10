import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';

@Component({
  selector: 'app-monitor-pro-current',
  templateUrl: './pro-current.component.html',
  styleUrls: ['./pro-current.component.css']
})
export class MonitorProCurrentComponent implements OnInit {
  private waitingForData = true;
  private data = {
    ess0: {
      Soc: null,
      ActivePowerL1: null,
      ActivePowerL2: null,
      ActivePowerL3: null,
      ReactivePowerL1: null,
      ReactivePowerL2: null,
      ReactivePowerL3: null
    },
    meter0: {
      ActivePowerL1: null,
      ActivePowerL2: null,
      ActivePowerL3: null,
      ReactivePowerL1: null,
      ReactivePowerL2: null,
      ReactivePowerL3: null
    },
    meter1: {
      ActivePowerL1: null,
      ActivePowerL2: null,
      ActivePowerL3: null
    }
  }

  constructor() { }

  ngOnInit() {
    var ws = new WebSocket("ws://" + location.hostname + "/websocket");
    ws.onopen = () => {
      ws.send(JSON.stringify({
        subscription: {
          add: {
            ess0: [
              "Soc", "ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3"
            ],
            meter0: [
              "ActivePowerL1", "ActivePowerL2", "ActivePowerL3", "ReactivePowerL1", "ReactivePowerL2", "ReactivePowerL3"
            ],
            meter1: [
              "ActivePowerL1", "ActivePowerL2", "ActivePowerL3"
            ]
          }
        }
      }))
    }

    ws.onmessage = (message) => {
      var data = JSON.parse(message.data).data;
      if (data) {
        if (data.ess0) {
          this.data.ess0.Soc = data.ess0.Soc ? data.ess0.Soc : null;
          this.data.ess0.ActivePowerL1 = data.ess0.ActivePowerL1 ? data.ess0.ActivePowerL1 : null;
          this.data.ess0.ActivePowerL2 = data.ess0.ActivePowerL2 ? data.ess0.ActivePowerL2 : null;
          this.data.ess0.ActivePowerL3 = data.ess0.ActivePowerL3 ? data.ess0.ActivePowerL3 : null;
          this.data.ess0.ReactivePowerL1 = data.ess0.ReactivePowerL1 ? data.ess0.ReactivePowerL1 : null;
          this.data.ess0.ReactivePowerL2 = data.ess0.ReactivePowerL2 ? data.ess0.ReactivePowerL2 : null;
          this.data.ess0.ReactivePowerL3 = data.ess0.ReactivePowerL3 ? data.ess0.ReactivePowerL3 : null;
        }
        if (data.meter0) {
          this.data.meter0.ActivePowerL1 = data.meter0.ActivePowerL1 ? data.meter0.ActivePowerL1 : null;
          this.data.meter0.ActivePowerL2 = data.meter0.ActivePowerL2 ? data.meter0.ActivePowerL2 : null;
          this.data.meter0.ActivePowerL3 = data.meter0.ActivePowerL3 ? data.meter0.ActivePowerL3 : null;
          this.data.meter0.ReactivePowerL1 = data.meter0.ReactivePowerL1 ? data.meter0.ReactivePowerL1 : null;
          this.data.meter0.ReactivePowerL2 = data.meter0.ReactivePowerL2 ? data.meter0.ReactivePowerL2 : null;
          this.data.meter0.ReactivePowerL3 = data.meter0.ReactivePowerL3 ? data.meter0.ReactivePowerL3 : null;
        }
        if (data.meter1) {
          this.data.meter1.ActivePowerL1 = data.meter1.ActivePowerL1 ? data.meter1.ActivePowerL1 : null;
          this.data.meter1.ActivePowerL2 = data.meter1.ActivePowerL2 ? data.meter1.ActivePowerL2 : null;
          this.data.meter1.ActivePowerL3 = data.meter1.ActivePowerL3 ? data.meter1.ActivePowerL3 : null;
        }
        this.waitingForData = false;
      }
    }

    /*
    REST
        return this.http
          .get("http://localhost:8081/rest/thing/ess0/channel/Soc/current")
          .toPromise()
          .then(response => console.log(response))
          .catch(error => console.log(error));
    */
  }

}
