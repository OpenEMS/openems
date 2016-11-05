import { Component, OnInit, OnDestroy } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';

@Component({
  selector: 'app-current-monitor',
  templateUrl: './current-monitor.component.html',
  styleUrls: ['./current-monitor.component.css']
})
export class CurrentMonitorComponent implements OnInit {
  private data;

  constructor() { }

  ngOnInit() {
    var ws = new WebSocket("ws://localhost:8090");
    ws.onopen = () => {
      ws.send(JSON.stringify({
        "subscription": {
          "add": {
            "ess0": [
              "Soc"
            ]
          }
        }
      })
      )
    }

    ws.onmessage = (message) => {
      var data = JSON.parse(message.data).data;
      this.data = data;
    }

    /*
    REST
        return this.http
          .get("http://localhost:8081/rest/thing/ess0/channel/Soc/current")
          .toPromise()
          .then(response => console.log(response))
          .catch(error => console.log(error));
    */
    /*
          return this.http
          .get("http://localhost:8090/ws")
          .toPromise()
          .then(response => console.log("RESPONSE: " + response))
          .catch(error => console.log("ERROR: " + error));
          */
  }

}
