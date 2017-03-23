import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import * as d3 from 'd3';
import * as d3shape from 'd3-shape';
import * as moment from 'moment';

import { WebsocketService, Device } from '../../shared/shared';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit, OnDestroy {
  private device: Device;
  private deviceSubscription: Subscription;
  private dateString: string;
  private clazzActive: string = "";

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService
  ) { }

  ngOnInit() {
    let date = new Date();
    this.dateString = date.getFullYear() + "-" + (date.getMonth() + 1) + "-" + date.getDate();

    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device != null) {
        device.socData.subscribe((newData) => {
          if (newData != null) {
            console.log("data", newData);
            let socData = {
              name: "ess0/Soc",
              series: []
            }
            let socActivepowerData = {
              name: "ess0/ActivePower",
              series: []
            }
            for (let newDatum of newData["data"]) {
              if (newDatum["channels"]["ess0"]["Soc"] != null) {
                socData.series.push({ name: moment(newDatum["time"]), value: newDatum["channels"]["ess0"]["Soc"] });
              } else {
                socData.series.push({ name: moment(newDatum["time"]), value: 0 });
              }
              if (newDatum["channels"]["ess0"]["ActivePower"] != null) {
                socActivepowerData.series.push({ name: new Date(newDatum["time"]), value: newDatum["channels"]["ess0"]["ActivePower"] });
              } else {
                socActivepowerData.series.push({ name: new Date(newDatum["time"]), value: 0 });
              }
            }
            this.socData = [socData];
            this.socActivepowerData = [socActivepowerData];
          }
        })
      }
    })
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
      this.device.query(date, date, { ess0: ["Soc", "ActivePower"] });
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

  private socData = [];/* = [
    {
      "name": "ess0/Soc",
      "series": [
        { name: "2017-03-21T08:55:20Z", value: 47.0 }, { name: "2017-03-21T08:55:30Z", value: 47.0 }, { name: "2017-03-21T08:56:20Z", value: 63.0 }
      ]
    }
  ];*/

  /**
   * test data for third chart
   */
  private socActivepowerData = [
    {
      "name": "Eigene PV-Produktion",
      "series": [
        { name: "2017-03-21T15:21", value: 47.0 }, { name: "2017-03-21T15:22", value: 47.0 }, { name: "2017-03-21T15:23", value: 63.0 }
      ]
    },
    {
      "name": "Durchschnittliche PV-Produktion",
      "series": [
        { name: "2017-03-21T15:21", value: 25.0 }, { name: "2017-03-21T15:22", value: 35.0 }, { name: "2017-03-21T15:23", value: 30.0 }
      ]
    },
    {
      "name": "Eigener Verbrauch",
      "series": [
        { name: "2017-03-21T15:21", value: 50.0 }, { name: "2017-03-21T15:22", value: 70.0 }, { name: "2017-03-21T15:23", value: 60.0 }
      ]
    },
    {
      "name": "Durchschnittlicher Verbrauch",
      "series": [
        { name: "2017-03-21T15:21", value: 12.0 }, { name: "2017-03-21T15:22", value: 15.0 }, { name: "2017-03-21T15:23", value: 17.0 }
      ]
    },
    {
      "name": "Eigene Netzeinspeisung",
      "series": [
        { name: "2017-03-21T15:21", value: 15.0 }, { name: "2017-03-21T15:22", value: 20.0 }, { name: "2017-03-21T15:23", value: 25.0 }
      ]
    },
    {
      "name": "Durchschnittliche Netzeinspeisung",
      "series": [
        { name: "2017-03-21T15:21", value: 17.0 }, { name: "2017-03-21T15:22", value: 21.0 }, { name: "2017-03-21T15:23", value: 23.0 }
      ]
    },
    {
      "name": "Eigener Netzbezug",
      "series": [
        { name: "2017-03-21T15:21", value: 5.0 }, { name: "2017-03-21T15:22", value: 10.0 }, { name: "2017-03-21T15:23", value: 15.0 }
      ]
    },
    {
      "name": "Durchschnittlicher Netzbezug",
      "series": [
        { name: "2017-03-21T15:21", value: 7.0 }, { name: "2017-03-21T15:22", value: 10.0 }, { name: "2017-03-21T15:23", value: 12.0 }
      ]
    }
  ];

  private getDataToday() {
    this.clazzActive = "btnToday";

    if (this.device != null) {
      let date = new Date();
      this.device.query(date, date, { ess0: ["Soc", "ActivePower"] });
    }
  }

  private getDataYesterday() {
    this.clazzActive = "btnYesterday";

    if (this.device != null) {
      let date = new Date();
      let yesterday = date;
      yesterday.setDate(date.getDate() - 1);
      this.device.query(yesterday, yesterday, { ess0: ["Soc", "ActivePower"] });
    }
  }

  private getDataLastWeek() {
    this.clazzActive = "btnLastWeek";
  }

  private getDataLastMonth() {
    this.clazzActive = "btnLastMonth";
  }

  private getDataLastYear() {
    this.clazzActive = "btnLastYear";
  }

  private setOtherTimespan() {
    this.clazzActive = "btnOtherTimespan";
  }
}