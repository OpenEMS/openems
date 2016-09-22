import { Component, OnInit } from '@angular/core';

import { DataService } from '../../data/data-service';
//import { OpenemsService } from '../../data/openems/openems.service';
import { CurrentData } from '../../data/current-data';

@Component({
  selector: 'app-current-monitor',
  templateUrl: './current-monitor.component.html',
  styleUrls: ['./current-monitor.component.css']
})
export class CurrentMonitorComponent implements OnInit {
  private currentData: CurrentData;

  constructor(
    private dataService: DataService,
  ) { }

  ngOnInit() {
    this.getData();
  }

  getData(): void {
    this.dataService.getCurrentData()
      .then(data => this.currentData = data);
  }
}
