import { Component, OnInit, OnDestroy } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import {ISubscription} from 'rxjs/Subscription';

import { DataService } from '../../data/data.service';
import { CurrentData } from '../../data/current-data';

@Component({
  selector: 'app-current-monitor',
  templateUrl: './current-monitor.component.html',
  styleUrls: ['./current-monitor.component.css']
})
export class CurrentMonitorComponent implements OnInit, OnDestroy {
  private currentData: CurrentData;
  private currentDataSubscription: ISubscription;

  constructor(
    private dataService: DataService,
  ) { }

  ngOnInit() {
    this.currentDataSubscription = this.dataService.data.subscribe((data) => {
      this.currentData = data;
    });
  }

  ngOnDestroy() {
    this.currentDataSubscription.unsubscribe();
  }
}
