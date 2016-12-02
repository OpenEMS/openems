import { Component, OnInit } from '@angular/core';
import { Http, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { ISubscription } from 'rxjs/Subscription';

@Component({
  selector: 'app-monitor-test-current',
  templateUrl: './universal-current.component.html'
})
export class MonitorUniversalCurrentComponent implements OnInit {
  private data: JSON;
  private error: string;

  constructor(private http: Http) { }

  ngOnInit() {
    var ws = new WebSocket("ws://" + location.hostname + "/websocket");
    //var ws = new WebSocket("ws://localhost:8085");
    ws.onopen = () => {
      ws.send(JSON.stringify({
        authenticate: {
          password: "owner"
        },
        subscribe: "fenecon_monitor_v1"
      }))
      ws.onmessage = (message) => {
        this.error = null;
        var m = JSON.parse(message.data);
        if ("natures" in m) {
        }
        if ("data" in m) {
          this.data = m.data;
        }
      }
    }
    ws.onerror = (e) => {
      this.data = null;
      this.error = "Herstellen der Verbindung ist nicht möglich."; 
    }
    ws.onclose = (e) => {
      this.data = null;
      this.error = "Verbindung wurde beendet."; 
    }
  }
}
