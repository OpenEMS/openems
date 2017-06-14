import { Component, Input } from '@angular/core';
import * as moment from 'moment';

import { Dataset } from './../../../../shared/shared';

@Component({
  selector: 'chart-soc',
  templateUrl: './chartsoc.component.html'
})
export class ChartSocComponent {

  @Input()
  set labels(labels: moment.Moment[]) {
    if (labels == null || labels.length == 0) {
      this._labels = null;
    } else {
      this._labels = labels;
      // TODO: show full day in labels
      //this.options.scales.xAxes[0].ticks["max"] = labels[labels.length - 1].endOf('day');
    }
  }

  @Input()
  set datasets(datasets: Dataset[]) {
    if (datasets == null || datasets.length == 0) {
      this._datasets = null;
    } else {
      this._datasets = datasets;
    }
  }

  private _labels: moment.Moment[] = null;
  private _datasets: Dataset[] = null;

  private colors = [{
    backgroundColor: 'rgba(0,152,70,0.2)',
    borderColor: 'rgba(0,152,70,1)',
  }];

  private options = {
    maintainAspectRatio: false,
    legend: {
      position: 'right'
    },
    elements: {
      point: {
        radius: 0,
        hitRadius: 10,
        hoverRadius: 10
      }
    },
    scales: {
      yAxes: [{
        ticks: {
          beginAtZero: true,
          max: 100
        }
      }],
      xAxes: [{
        type: 'time',
        ticks: {}
      }]
    }
  };
}