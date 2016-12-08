import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection, ActiveConnection } from './../../service/connection.service';
import { Router } from '@angular/router';

class Device {
  name:string;
  soc:number;
  activePower:number;
}

@Component({
  selector: 'app-monitor-overview',
  templateUrl: './overview.component.html'
})
export class MonitorOverviewComponent implements OnInit {
  private devices: Device[] = [];

  constructor(
    private connectionService: ConnectionService,
    private router: Router
  ) {}

  ngOnInit() {
    var connection: Connection = this.connectionService.getDefault();
    if(!(connection instanceof ActiveConnection)) {
      this.router.navigate(['login']);
    } 

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
}
