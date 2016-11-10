import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';

@Component({
  selector: 'app-monitor-commercial-current',
  templateUrl: './commercial-current.component.html',
  styleUrls: ['./commercial-current.component.css']
})
export class MonitorCommercialCurrentComponent implements OnInit {
  private waitingForData = true;
  private data = {
    ess0: {
      Soc: null,
      ActivePower: null
    },
    meter0: {
      ActivePower: null
    },
    meter1: {
      ActivePower: null
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
              "Soc", "ActivePower"
            ],
            meter0: [
              "ActivePower"
            ],
            meter1: [
              "ActivePower"
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
          this.data.ess0.ActivePower = data.ess0.ActivePower ? data.ess0.ActivePower : null;
        }
        if (data.meter0) {
          this.data.meter0.ActivePower = data.meter0.ActivePower ? data.meter0.ActivePower : null;
        }
        if (data.meter1) {
          this.data.meter1.ActivePower = data.meter1.ActivePower ? data.meter1.ActivePower : null;
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
