import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import * as d3 from 'd3';
import * as d3shape from 'd3-shape';

import { WebsocketService, Device } from '../../shared/shared';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class DeviceHistoryComponent implements OnInit, OnDestroy {
  private device: Device;
  private deviceSubscription: Subscription;
  private dateString: string;

  constructor(
    private route: ActivatedRoute,
    // private websocketService: WebsocketService
  ) { }

  ngOnInit() {
    // let date = new Date();
    // this.dateString = date.getFullYear() + "-" + (date.getMonth() + 1) + "-" + date.getDate();

    // this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
    //   this.device = device;
    //   if (device != null) {
    //     device.historicData.subscribe((newData) => {
    //       if (newData != null) {
    //         console.log("data", newData);
    //         let historicData = {
    //           name: "ess0/Soc",
    //           series: []
    //         }
    //         for (let newDatum of newData["data"]) {
    //           if (newDatum["channels"]["ess0"]["Soc"] != null) {
    //             historicData.series.push({ name: new Date(newDatum["time"]), value: newDatum["channels"]["ess0"]["Soc"] });
    //           } else {
    //             historicData.series.push({ name: new Date(newDatum["time"]), value: 0 });
    //           }
    //         }
    //         this.historicData = [historicData];
    //       }
    //     })
    //   }
    // })
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
    if (this.device) {
      this.device.unsubscribe();
    }
  }

  query(dateString: string) {
    if (this.device != null) {
      let date = new Date(dateString);
      this.device.query(date, date, { ess0: ["Soc"] });
    }
  }

  view: any[] = [700, 400];

  // options
  xAxisLabel = 'Country';
  yAxisLabel = 'Population';
  curve = d3shape.curveBasis;

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