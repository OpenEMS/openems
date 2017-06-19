import { Component, Input, OnInit, OnChanges, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import * as moment from 'moment';

import { Dataset, EMPTY_DATASET, Device, Config, QueryReply, Summary } from './../../../../shared/shared';

@Component({
  selector: 'energychart',
  templateUrl: './energychart.component.html'
})
export class EnergyChartComponent implements OnChanges {

  @Input() private device: Device;
  @Input() private fromDate: moment.Moment;
  @Input() private toDate: moment.Moment;

  @ViewChild('energyChart') private chart: BaseChartDirective;

  public labels: moment.Moment[] = [];
  public datasets: Dataset[] = EMPTY_DATASET;

  private ngUnsubscribe: Subject<void> = new Subject<void>();
  private queryreplySubject: Subject<QueryReply>;

  private colors = [{
    backgroundColor: 'rgba(37,154,24,0.2)',
    borderColor: 'rgba(37,154,24,1)',
  }, {
    backgroundColor: 'rgba(221,223,1,0.2)',
    borderColor: 'rgba(221,223,1,1)',
  }, {
    backgroundColor: 'rgba(45,143,171,0.2)',
    borderColor: 'rgba(45,143,171,1)',
  }];

  private options: {} = {
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
          beginAtZero: true
        }
      }],
      xAxes: [{
        type: 'time',
        time: {}
      }]
    }
  };

  ngOnChanges(changes: any) {
    // close old queryreplySubject
    if (this.queryreplySubject != null) {
      this.queryreplySubject.complete();
    }
    // create channels for query
    let channels = this.device.config.getValue().getPowerChannels();
    // execute query
    let queryreplySubject = this.device.query(this.fromDate, this.toDate, channels);
    queryreplySubject.subscribe(queryreply => {
      // prepare datasets and labels
      let activePowers = {
        production: [],
        grid: [],
        consumption: []
      }
      let labels: moment.Moment[] = [];
      for (let reply of queryreply.data) {
        labels.push(moment(reply.time));
        let data = new Summary(this.device.config.getValue(), reply.channels);
        activePowers.grid.push(data.grid.activePower);
        activePowers.production.push(data.production.activePower);
        activePowers.consumption.push(data.consumption.activePower);
      }
      this.datasets = [{
        label: "Erzeugung",
        data: activePowers.production
      }, {
        label: "Netz",
        data: activePowers.grid
      }, {
        label: "Verbrauch",
        data: activePowers.consumption
      }];
      this.labels = labels;
      setTimeout(() => {
        // Workaround, because otherwise chart data and labels are not refreshed...
        if (this.chart) {
          this.chart.ngOnChanges({} as SimpleChanges);
        }
      });

    }, error => {
      this.datasets = EMPTY_DATASET;
      this.labels = [];
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}