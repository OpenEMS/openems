import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection } from './../../service/connection.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-monitor-overview',
  templateUrl: './overview.component.html'
})
export class MonitorOverviewComponent implements OnInit {

  constructor(
    private connectionService: ConnectionService,
    private router: Router
  ) { }

  ngOnInit() {
    this.connectionService.connectionsChanged.subscribe((/* value */) => {
      for(let conn in this.connectionService.connections) {
        if(this.connectionService.connections[conn].isConnected) {
          return;
        }
        this.router.navigate(['/login']);
      }
    })
  }

  private contains(array: string[], tag: string): boolean {
    return array.indexOf(tag) != -1
  }
}
