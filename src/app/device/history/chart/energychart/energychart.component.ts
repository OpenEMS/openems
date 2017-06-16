import { Component, Input, OnInit, OnChanges, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import * as moment from 'moment';

import { Dataset, EMPTY_DATASET, Device, Config, QueryReply } from './../../../../shared/shared';

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
    backgroundColor: 'rgba(0,152,70,0.2)',
    borderColor: 'rgba(0,152,70,1)',
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
          beginAtZero: true,
          max: 100
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
    let channels = {};
    channels = { ess0: ["ActivePower"] };
    // this.essDevices.forEach(device => channels[device] = ['Soc']);
    // execute query
    let queryreplySubject = this.device.query(this.fromDate, this.toDate, channels);
    queryreplySubject.subscribe(queryreply => {
      // prepare datas array and prefill with each device
      let tmpData: {
        [thing: string]: number[];
      } = {};
      let labels: moment.Moment[] = [];
      console.log(queryreply);
      // this.essDevices.forEach(device => tmpData[device] = []);
      // for (let reply of queryreply.data) {
      //   // read timestamp and soc of each device' reply
      //   labels.push(moment(reply.time));
      //   this.essDevices.forEach(device => {
      //     let soc = 0;
      //     if (device in reply.channels && "Soc" in reply.channels[device] && reply.channels[device]["Soc"]) {
      //       soc = Math.round(reply.channels[device].Soc);
      //     }
      //     tmpData[device].push(soc);
      //   });
      // }
      // // refresh global datasets and labels
      // let datasets = [];
      // for (let device in tmpData) {
      //   datasets.push({
      //     label: "Ladezustand (" + device + ")",
      //     data: tmpData[device]
      //   });
      // }
      // this.datasets = datasets;
      // this.labels = labels;
      // setTimeout(() => {
      //   // Workaround, because otherwise chart data and labels are not refreshed...
      //   if (this.chart) {
      //     this.chart.ngOnChanges({} as SimpleChanges);
      //   }
      // }, 0);
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