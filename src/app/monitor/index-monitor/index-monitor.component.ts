import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { DataService } from '../../data/data.service';

@Component({
  selector: 'app-index-monitor',
  templateUrl: './index-monitor.component.html',
  styleUrls: ['./index-monitor.component.css']
})
export class IndexMonitorComponent implements OnInit {
  private devices: any[] = [];

  constructor(
    private router: Router,
    private dataService: DataService
  ) { }

  ngOnInit() {
    this.dataService.getDevices().then(devices => {
      this.devices = devices.records;
      console.log(devices);
    });
  }

  gotoMonitor(device: any): void {
    let link = ['/monitor/0/', device.name_number];
    this.router.navigate(link);
  }

}
