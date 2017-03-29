import { Component, Input, OnInit } from '@angular/core';

import { Device } from '../../../shared/shared';

import * as d3 from 'd3';
import * as d3shape from 'd3-shape';
import * as moment from 'moment';

@Component({
  selector: 'history',
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit {
  private dataSoc = [];

  @Input()
  private device: Device;

  ngOnInit() {
    if (this.device != null) {
      this.device.query(moment(), moment());

      this.device.historyData.subscribe((newData) => {
        if (newData != null) {
          let dataSoc = {
            name: "Ladezustand",
            series: []
          }
          for (let newDatum of newData) {
            let timestamp = moment(newDatum["time"]);
            let soc = newDatum.summary.storage.soc != null ? newDatum.summary.storage.soc : 0;
            dataSoc.series.push({ name: timestamp, value: soc });
          }
          this.dataSoc = [dataSoc];
        }
      })
    }
  }

  colorScheme = {
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA']
  };


}
