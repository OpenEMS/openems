import { Component, OnInit, OnDestroy, ElementRef } from '@angular/core';
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
  private dataEnergy = [];
  private dateToday: Date = new Date();

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
    private elRef: ElementRef
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device != null) {
        // start with loading "today"
        if (this.activePeriod == null) {
          this.setPeriod("today");
        }
        device.historyData.subscribe((newData) => {
          if (newData != null) {
            let dataSoc = {
              name: "Ladezustand",
              series: []
            }
            let dataEnergy = {
              name: "Erzeugung",
              series: []
            }
            for (let newDatum of newData) {
              let timestamp = moment(newDatum["time"]);
              let soc = newDatum.summary.storage.soc != null ? newDatum.summary.storage.soc : 0;
              dataSoc.series.push({ name: timestamp, value: soc });
              let production = newDatum.summary.production.activePower != null ? newDatum.summary.production.activePower : 0;
              dataEnergy.series.push({ name: timestamp, value: production });
            }
            this.dataSoc = [dataSoc];
            this.dataEnergy = [dataEnergy];
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

  /**
   * later: needed data for energychart.component.ts
   * current: storage ActivePower is shown on energychart.component.ts
   */
  // private dataEnergy = [
  //   {
  //     "name": "Eigene PV-Produktion",
  //     "series": [
  //       { name: "2017-03-21T15:21", value: 47.0 }, { name: "2017-03-21T15:22", value: 47.0 }, { name: "2017-03-21T15:23", value: 63.0 }
  //     ]
  //   },
  //   {
  //     "name": "Durchschnittliche PV-Produktion",
  //     "series": [
  //       { name: "2017-03-21T15:21", value: 25.0 }, { name: "2017-03-21T15:22", value: 35.0 }, { name: "2017-03-21T15:23", value: 30.0 }
  //     ]
  //   },
  //   {
  //     "name": "Eigener Verbrauch",
  //     "series": [
  //       { name: "2017-03-21T15:21", value: 50.0 }, { name: "2017-03-21T15:22", value: 70.0 }, { name: "2017-03-21T15:23", value: 60.0 }
  //     ]
  //   },
  //   {
  //     "name": "Durchschnittlicher Verbrauch",
  //     "series": [
  //       { name: "2017-03-21T15:21", value: 12.0 }, { name: "2017-03-21T15:22", value: 15.0 }, { name: "2017-03-21T15:23", value: 17.0 }
  //     ]
  //   },
  //   {
  //     "name": "Eigene Netzeinspeisung",
  //     "series": [
  //       { name: "2017-03-21T15:21", value: 15.0 }, { name: "2017-03-21T15:22", value: 20.0 }, { name: "2017-03-21T15:23", value: 25.0 }
  //     ]
  //   },
  //   {
  //     "name": "Durchschnittliche Netzeinspeisung",
  //     "series": [
  //       { name: "2017-03-21T15:21", value: 17.0 }, { name: "2017-03-21T15:22", value: 21.0 }, { name: "2017-03-21T15:23", value: 23.0 }
  //     ]
  //   },
  //   {
  //     "name": "Eigener Netzbezug",
  //     "series": [
  //       { name: "2017-03-21T15:21", value: 5.0 }, { name: "2017-03-21T15:22", value: 10.0 }, { name: "2017-03-21T15:23", value: 15.0 }
  //     ]
  //   },
  //   {
  //     "name": "Durchschnittlicher Netzbezug",
  //     "series": [
  //       { name: "2017-03-21T15:21", value: 7.0 }, { name: "2017-03-21T15:22", value: 10.0 }, { name: "2017-03-21T15:23", value: 12.0 }
  //     ]
  //   }
  // ];

  private setOtherTimespan() {
    this.activePeriod = "otherTimespan";
  }

  private setTimespan(from: any, to: any) {
    if (from != "" || to != "") {
      this.setPeriod('otherTimespan', from, to);
    }
  }

  private setPeriod(period: string, from?: any, to?: any) {
    if (!this.device) {
      period = null;
    }
    this.activePeriod = period;
    this.dataEnergy = this.dataSoc = [];
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
      case "otherTimespan":
        fromDate = moment(from);
        toDate = moment(to);
        break;
      default:
        this.activePeriod = null;
        return;
    }
    // let labelText = document.getElementById("currTimespan");

    // if (period == "today" || period == "yesterday") {
    //   labelText.innerText = "" + fromDate;
    // } else {
    //   labelText.innerText = fromDate + " bis " + toDate;
    // }

    this.device.query(fromDate, toDate);
  }
}