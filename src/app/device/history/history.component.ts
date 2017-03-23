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
  private activePeriod: string = null;
  private dataSoc = [];

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device != null) {
        // start with loading "today"
        if (this.activePeriod == null) {
          this.setPeriod("today");
        }
        // handle new data
        device.historicData.subscribe((newData) => {
          if (newData != null) {
            console.log("data", newData);
            let dataSoc = {
              name: "Ladezustand",
              series: []
            }
            for (let newDatum of newData["data"]) {
              let soc = newDatum["channels"]["ess0"]["Soc"] != null ? newDatum["channels"]["ess0"]["Soc"] : 0;
              dataSoc.series.push({ name: moment(newDatum["time"]), value: soc });
            }
            this.dataSoc = [dataSoc];
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

  view: any[] = [700, 400];
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

  /**
   * test data for third chart
   */
  private historicDataView = [
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

  private setPeriod(period: string) {
    if (!this.device) {
      period = null;
    }
    this.activePeriod = period;
    this.historicData = [];
    let fromDate;
    let toDate;
    switch (period) {
      case "today":
        fromDate = toDate = moment();
        break;
      case "yesterday":
        fromDate = toDate = moment().subtract(1, "days");
        break;
      case "lastWeek":
        fromDate = moment().subtract(1, "weeks");
        toDate = moment();
        break;
      case "lastMonth":
        fromDate = moment().subtract(1, "months");
        toDate = moment();
        break;
      case "lastYear":
        fromDate = moment().subtract(1, "years");
        toDate = moment();
        break;
      default:
        this.activePeriod = null;
        return;
    }
    this.device.query(fromDate, toDate, { ess0: ["Soc"] });
  }
}