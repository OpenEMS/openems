import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection } from './../../service/connection.service';
import { Router } from '@angular/router';

class Device {
  name: string;
  soc: number;
  activePower: number;
}

@Component({
  selector: 'app-monitor-overview',
  templateUrl: './overview.component.html'
})
export class MonitorOverviewComponent implements OnInit {
  private devices: Device[] = [];
  //private devices: { [url: string]: Device } = {};

  constructor(
    private connectionService: ConnectionService,
    private router: Router
  ) { }

  ngOnInit() {
    /*var connection: Connection = this.connectionService.getDefault();
    if (!(connection instanceof ActiveConnection)) {
      this.router.navigate(['login']);

    } else {
      connection.subject.subscribe((message: any) => {
        if ("data" in message) {
          var msg: any = JSON.parse(message.data);
          if ("data" in msg) {
            if (connection instanceof ActiveConnection) {
              for (let thing in connection.natures) {
                if (thing in msg.data) {
                  let n: string[] = connection.natures[thing];
                  if (this.contains(n, "EssNature")) {
                    this.devices.push({
                      name: thing,
                      soc: msg.data[thing]["Soc"],
                      activePower: null
                    });
                  }
                }
              }
            }
          }
        }
      });
    }*/


    /*  this.connectionService.connectionsChanged.subscribe(() => {
        console.log("subscribe");
        this.devices = [];
        for (var url in this.connectionService.connections) {
          var connection: Connection = this.connectionService.connections[url];
          if(connection instanceof ActiveConnection) {
            var device: Device = {
              name: null,
              soc: null,
              activePower: null
            };
            this.devices.push(device);
            connection.subject.subscribe((value: any) => {
              console.log(value);
            });
          }
        }
      }, error => {
        this.devices = []
      }, () => {
        this.devices = []
      });*/
  }

  private contains(array: string[], tag: string): boolean {
    return array.indexOf(tag) != -1
  }
}
