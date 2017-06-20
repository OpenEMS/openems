import { Component, Input, OnInit, OnChanges, OnDestroy, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import * as moment from 'moment';

import { Dataset, EMPTY_DATASET, Device, Config, QueryReply, ChannelAddresses } from './../../../../shared/shared';
import { DEFAULT_TIME_CHART_OPTIONS, ChartOptions } from './../shared';
import { TemplateHelper } from './../../../../shared/service/templatehelper';

@Component({
  selector: 'socchart',
  templateUrl: './socchart.component.html'
})
export class SocChartComponent implements OnInit, OnChanges, OnDestroy {

  @Input() private device: Device;
  @Input() private socChannels: ChannelAddresses;
  @Input() private fromDate: moment.Moment;
  @Input() private toDate: moment.Moment;

  @ViewChild('socChart') private chart: BaseChartDirective;

  constructor(private tmpl: TemplateHelper) { }

  public labels: moment.Moment[] = [];
  public datasets: Dataset[] = EMPTY_DATASET;
  public loading: boolean = true;

  private ngUnsubscribe: Subject<void> = new Subject<void>();
  private queryreplySubject: Subject<QueryReply>;

  private colors = [{
    backgroundColor: 'rgba(0,152,70,0.2)',
    borderColor: 'rgba(0,152,70,1)',
  }];
  private options: ChartOptions;

  ngOnInit() {
    let options = <ChartOptions>this.tmpl.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = "Prozent";
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  ngOnChanges(changes: any) {
    // close old queryreplySubject
    if (this.queryreplySubject != null) {
      this.queryreplySubject.complete();
    }
    // show loading...
    this.loading = true;
    // execute query
    let queryreplySubject = this.device.query(this.fromDate, this.toDate, this.socChannels);
    queryreplySubject.subscribe(queryreply => {
      // prepare datas array and prefill with each device
      let tmpData: {
        [thing: string]: number[];
      } = {};
      let labels: moment.Moment[] = [];
      for (let thing in this.socChannels) {
        tmpData[thing] = [];
      }
      for (let reply of queryreply.data) {
        // read timestamp and soc of each device' reply
        labels.push(moment(reply.time));
        for (let thing in this.socChannels) {
          let soc = 0;
          if (thing in reply.channels && "Soc" in reply.channels[thing] && reply.channels[thing]["Soc"]) {
            soc = Math.round(reply.channels[thing].Soc);
          }
          tmpData[thing].push(soc);
        }
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
      this.loading = false;
      setTimeout(() => {
        // Workaround, because otherwise chart data and labels are not refreshed...
        if (this.chart) {
          this.chart.ngOnChanges({} as SimpleChanges);
        }
      });

    }, error => {
      this.datasets = EMPTY_DATASET;
      this.labels = [];
      // TODO should be error message
      this.loading = true;
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}