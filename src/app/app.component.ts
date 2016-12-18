import { Component, OnInit, ViewContainerRef } from '@angular/core';
import { ConnectionService, Connection } from './service/connection.service';
import { Router } from '@angular/router';
import { ToastsManager } from 'ng2-toastr/ng2-toastr';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private navCollapsed: boolean = true;
  private menuitems: any[];
  private connections: string;

  private options = {
    position: ["bottom", "left"],
    timeOut: 5000,
    lastOnBottom: true
  }

  constructor(
    private connectionService: ConnectionService,
    private router: Router,
    private toastr: ToastsManager,
    private vRef: ViewContainerRef
  ) {
    this.toastr.setRootViewContainerRef(vRef);
  }

  ngOnInit() {
    this.menuitems = [
      { label: 'Übersicht', routerLink: '/monitor' },
      { label: 'Aktuelle Daten', routerLink: '/monitor/current' },
      { label: 'Historie', routerLink: '/monitor/history' }
    ];
  }
}
