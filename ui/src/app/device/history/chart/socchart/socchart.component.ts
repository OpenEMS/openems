import { Component, Input, OnInit, OnChanges, OnDestroy, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import { TranslateService } from '@ngx-translate/core';
import * as moment from 'moment';

import { Device } from '../../../../shared/device/device';
import { DefaultTypes } from '../../../../shared/service/defaulttypes';
import { Dataset, EMPTY_DATASET, QueryReply } from './../../../../shared/shared';
import { DEFAULT_TIME_CHART_OPTIONS, ChartOptions } from './../shared';
import { Utils } from './../../../../shared/service/utils';

// spinner component
import { SpinnerComponent } from '../../../../shared/spinner.component';

@Component({
  selector: 'socchart',
  templateUrl: './socchart.component.html'
})
export class SocChartComponent implements OnInit, OnChanges {

  @Input() private device: Device;
  @Input() private socChannels: DefaultTypes.ChannelAddresses;
  @Input() private fromDate: moment.Moment;
  @Input() private toDate: moment.Moment;

  @ViewChild('socChart') private chart: BaseChartDirective;

  constructor(
    private utils: Utils,
    private translate: TranslateService
  ) { }

  public labels: moment.Moment[] = [];
  public datasets: Dataset[] = EMPTY_DATASET;
  public loading: boolean = true;

  private colors = [{
    backgroundColor: 'rgba(0,152,70,0.2)',
    borderColor: 'rgba(0,152,70,1)',
  }, {
    backgroundColor: 'rgba(23,93,20,0.2)',
    borderColor: 'rgba(23,93,20,1)'
  }, {
    backgroundColor: 'rgba(139,222,135,0.2)',
    borderColor: 'rgba(139,222,135,1)'
  }, {
    backgroundColor: 'rgba(53,192,78,0.2)',
    borderColor: 'rgba(53,192,78,1)'
  }
  ];
  private options: ChartOptions;

  ngOnInit() {
    let options = <ChartOptions>this.utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  ngOnChanges() {
    this.loading = true;
    this.device.historicDataQuery(this.fromDate, this.toDate, this.socChannels).then(historicData => {
      // stop loading spinner
      this.loading = false;
      // prepare datas array and prefill with each device
      let tmpData: {
        [thing: string]: number[];
      } = {};
      let labels: moment.Moment[] = [];
      for (let thing in this.socChannels) {
        tmpData[thing] = [];
      }
      for (let record of historicData.data) {
        // read timestamp and soc of each device
        labels.push(moment(record.time));
        for (let thing in this.socChannels) {
          let soc = 0;
          if (thing in record.channels && "Soc" in record.channels[thing] && record.channels[thing]["Soc"] != null) {
            soc = Math.round(record.channels[thing].Soc);
          }
          tmpData[thing].push(soc);
        }
      }
      // refresh global datasets and labels
      let datasets = [];
      for (let device in tmpData) {
        datasets.push({
          label: this.translate.instant('General.Soc') + " (" + device + ")",
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
    }).catch(error => {
      this.datasets = EMPTY_DATASET;
      this.labels = [];
      // TODO should be error message
      this.loading = true;
    });
  };
}