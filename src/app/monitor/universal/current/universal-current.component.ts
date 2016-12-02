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
        for (let nature in this.natures) {
          console.log(nature + ": " + this.natures[nature]);
        }
      }
      if ("data" in message) {
        this.data = message.data;

        // fill primary nature type
        for (let nature in this.natures) {
          if (nature in this.data) {
            let n: string[] = this.natures[nature];
            console.log(nature + ": " + n.indexOf("SimulatorMeter"));
            let tag: string;
            if(this.contains(n, "SimulatorMeter")) {
              tag = "SimulatorMeter";
            } else if(this.contains(n, "SimulatorEss")) {
              tag = "SimulatorEss";
            }
            this.data[nature]["_tag"] = tag;
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

  /*
    ngOnInit() {
      //var ws = new WebSocket("ws://" + location.hostname + "/websocket");
      var ws = new WebSocket("ws://localhost:8085");
      ws.onopen = () => {
        ws.send(JSON.stringify({
          authenticate: {
            password: "owner"
          },
          subscribe: "fenecon_monitor_v1"
        }))
        ws.onmessage = (message) => {
          
        }
      }
      ws.onerror = (e) => {
        
      }
      ws.onclose = (e) => {
        this.data = null;
        this.error = "Verbindung wurde beendet.";
      }
    }
    */
}
