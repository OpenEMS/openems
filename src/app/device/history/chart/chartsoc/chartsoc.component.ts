import { Component, Input, OnInit, OnChanges, ViewChild, AfterViewInit } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import * as moment from 'moment';

import { Dataset, EMPTY_DATASET, Device, Config } from './../../../../shared/shared';

@Component({
  selector: 'chart-soc',
  templateUrl: './chartsoc.component.html'
})
export class ChartSocComponent implements OnInit, OnChanges, AfterViewInit {

  @Input() private device: Device;
  @Input() private essDevices: string[];
  @Input() private fromDate: moment.Moment;
  @Input() private toDate: moment.Moment;

  public labels: moment.Moment[] = [];
  public datasets: Dataset[] = EMPTY_DATASET;

  //@ViewChild('socChart') chart: BaseChartDirective;
  @ViewChild('socChart') chart: BaseChartDirective;

  ngAfterViewInit() {
    console.log(this.chart);
  }

  // private labels = [moment("2017-01-01 00:00"), moment("2017-01-02 00:00"), moment("2017-01-03 00:00")];
  // private datasets = [
  //   {
  //     label: "# of Votes",
  //     data: [12, 19, 3]
  //   }
  // ];

  private ngUnsubscribe: Subject<void> = new Subject<void>();

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
    // create channels for query
    let channels = {};
    this.essDevices.forEach(device => channels[device] = ['Soc']);
    // execute query
    this.device.query(this.fromDate, this.toDate, channels);
  }

  ngOnInit() {
    if (this.device != null) {
      /**
       * Receive data and prepare for chart
       */
      this.device.queryreply.takeUntil(this.ngUnsubscribe).subscribe(queryreplies => {
        if (queryreplies == null) {
          // reset datasets and labels
          this.datasets = EMPTY_DATASET;
          this.labels = [];
        } else {
          // prepare datas array and prefill with each device
          let tmpData: {
            [thing: string]: number[];
          } = {};
          let labels: moment.Moment[] = [];
          this.essDevices.forEach(device => tmpData[device] = []);
          for (let reply of queryreplies) {
            // read timestamp and soc of each device' reply
            labels.push(moment(reply.time));
            this.essDevices.forEach(device => {
              let soc = 0;
              if (device in reply.channels && "Soc" in reply.channels[device] && reply.channels[device]["Soc"]) {
                soc = Math.round(reply.channels[device].Soc);
              }
              tmpData[device].push(soc);
            });
          }
          // refresh global datasets and labels
          let datasets = [];
          for (let device in tmpData) {
            datasets.push({
              label: "Ladezustand (" + device + ")",
              data: tmpData[device]
            });
          }
          this.datasets = datasets;
          this.labels = labels;
        }
        console.log("count data: " + this.datasets[0].data.length, "count labels: " + this.labels.length);
        // this.chart.ngOnChanges({
        //   datasets: {
        //     currentValue: this.datasets,
        //     previousValue: null,
        //     firstChange: false,
        //     isFirstChange: () => false
        //   },
        //   labels: {
        //     currentValue: this.labels,
        //     previousValue: null,
        //     firstChange: false,
        //     isFirstChange: () => false
        //   },
        // });
        this.chart.ngOnChanges({});
      });
    }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}