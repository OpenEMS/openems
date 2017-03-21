import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../service/websocket.service';
import { Device } from '../../service/device';

@Component({
  selector: 'app-device-history',
  templateUrl: './history.component.html'
})
export class DeviceHistoryComponent implements OnInit, OnDestroy {
  private device: Device;
  private deviceSubscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device != null) {
        this.device.query(new Date(2017, 3, 21), new Date(2017, 3, 21), { ess0: ["Soc"] });
      }
    })
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
    if (this.device) {
      this.device.unsubscribe();
    }
  }

  view: any[] = [700, 400];

  // options
  showXAxis = true;
  showYAxis = true;
  gradient = false;
  showLegend = true;
  showXAxisLabel = true;
  xAxisLabel = 'Country';
  showYAxisLabel = true;
  yAxisLabel = 'Population';

  colorScheme = {
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA']
  };

  // line, area
  autoScale = true;

  private historicData = [
    {
      "name": "ess0/Soc",
      "series": [
        { name: "2017-03-21T08:55:20Z", value: 47.0 }, { name: "2017-03-21T08:55:30Z", value: 47.0 }, { name: "2017-03-21T08:56:20Z", value: 63.0 }
      ]
    }
  ];
}