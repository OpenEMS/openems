import { Component } from '@angular/core';

import { TabMenuModule, MenuItem } from 'primeng/primeng';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  private items: MenuItem[];

  ngOnInit() {
    this.items = [
      { label: 'Aktuelle Daten', icon: 'fa-bar-chart', routerLink: ['/monitor/current'] },
      { label: 'Historie', icon: 'fa-calendar', routerLink: ['/monitor/history'] }
    ];
  }
}
