import { Component, OnInit, ElementRef } from '@angular/core';
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
export class HistoryComponent implements OnInit {

  public device: Device;

  private deviceSubscription: Subscription;
  private activePeriod: string = null;
  private dataSoc = [];
  private dataEnergy = [];
  private datakWh = [];
  private dateToday: Date = new Date();
  private timespanText: string;

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
        device.historykWh.subscribe((newkWh) => {
          if (newkWh != null) {
            let kWhGridBuy = {
              name: "",
              value: 0
            }
            let kWhGridSell = {
              name: "",
              value: 0
            }
            let kWhProduction = {
              name: "Erzeugung",
              value: 0
            }
            let kWhStorageCharge = {
              name: "",
              value: 0
            }
            let kWhStorageDischarge = {
              name: "",
              value: 0
            }
            for (let type in newkWh) {
              if (newkWh[type].type == "production") {
                let production = newkWh[type].value != null ? newkWh[type].value : 0;
                kWhProduction.value = production;
              } else if (newkWh[type].type == "grid") {
                let gridBuy = newkWh[type].buy != null ? newkWh[type].buy : 0;
                kWhGridBuy.name = "Netzbezug";
                kWhGridBuy.value = gridBuy;
                let gridSell = newkWh[type].sell != null ? newkWh[type].sell : 0;
                kWhGridSell.name = "Netzeinspeiung";
                kWhGridSell.value = (gridSell * (-1));
              } else {
                let storageCharge = newkWh[type].charge != null ? newkWh[type].charge : 0;
                kWhStorageCharge.name = "Batteriebeladung";
                kWhStorageCharge.value = storageCharge;
                let storageDischarge = newkWh[type].discharge != null ? newkWh[type].discharge : 0;
                kWhStorageDischarge.name = "Batterieentladung";
                kWhStorageDischarge.value = (storageDischarge * (-1));
              }
            }
            this.datakWh = [kWhProduction, kWhGridBuy, kWhGridSell, kWhStorageCharge, kWhStorageDischarge];
          }
        })
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
            let dataConsumption = {
              name: "Verbrauch",
              series: []
            }
            let dataToGrid = {
              name: "Netzeinspeisung",
              series: []
            }
            let dataFromGrid = {
              name: "Netzbezug",
              series: []
            }
            for (let newDatum of newData) {
              let timestamp = moment(newDatum["time"]);
              let soc = newDatum.summary.storage.soc != null ? newDatum.summary.storage.soc : 0;
              dataSoc.series.push({ name: timestamp, value: soc });
              let production = newDatum.summary.production.activePower != null ? newDatum.summary.production.activePower : 0;
              dataEnergy.series.push({ name: timestamp, value: production });
              let consumption = newDatum.summary.consumption.activePower != null ? newDatum.summary.consumption.activePower : 0;
              dataConsumption.series.push({ name: timestamp, value: consumption });
              let grid = newDatum.summary.grid.activePower != null ? newDatum.summary.grid.activePower : 0;
              if (newDatum.summary.grid.activePower < 0) {
                dataToGrid.series.push({ name: timestamp, value: (grid * (-1)) });
              } else {
                dataFromGrid.series.push({ name: timestamp, value: grid });
              }
            }
            this.dataSoc = [dataSoc];
            this.dataEnergy = [dataEnergy, dataConsumption, dataToGrid, dataFromGrid];
          }
        })
      }
    })
  }

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
        this.timespanText = "Heute, " + fromDate.format("DD.MM.YYYY");
        break;
      case "yesterday":
        fromDate = toDate = moment().subtract(1, "days");
        this.timespanText = "Gestern, " + fromDate.format("DD.MM.YYYY");
        break;
      case "lastWeek":
        fromDate = moment().subtract(1, "weeks");
        toDate = moment();
        this.timespanText = "Letzte Woche, " + fromDate.format("DD.MM.YYYY") + " bis " + toDate.format("DD.MM.YYYY");
        break;
      case "lastMonth":
        fromDate = moment().subtract(1, "months");
        toDate = moment();
        this.timespanText = "Letzter Monat, " + fromDate.format("DD.MM.YYYY") + " bis " + toDate.format("DD.MM.YYYY");
        break;
      case "lastYear":
        fromDate = moment().subtract(1, "years");
        toDate = moment();
        this.timespanText = "Letztes Jahr, " + fromDate.format("DD.MM.YYYY") + " bis " + toDate.format("DD.MM.YYYY");
        break;
      case "otherTimespan":
        fromDate = moment(from);
        toDate = moment(to);
        this.timespanText = "Zeitraum, " + fromDate.format("DD.MM.YYYY") + " bis " + toDate.format("DD.MM.YYYY");
        break;
      default:
        this.activePeriod = null;
        return;
    }
    this.device.query(fromDate, toDate);
  }
}