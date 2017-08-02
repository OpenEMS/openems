import { Component, Input, OnInit, OnChanges, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import { TranslateService } from '@ngx-translate/core';
import * as moment from 'moment';

import { Dataset, EMPTY_DATASET, Device, Config, QueryReply, Summary } from './../../../../shared/shared';
import { DEFAULT_TIME_CHART_OPTIONS, ChartOptions, TooltipItem, Data } from './../shared';
import { TemplateHelper } from './../../../../shared/service/templatehelper';

// spinner component
import { SpinnerComponent } from '../../../../shared/spinner.component';

@Component({
  selector: 'energychart',
  templateUrl: './energychart.component.html'
})
export class EnergyChartComponent implements OnChanges {

  @Input() private device: Device;
  @Input() private fromDate: moment.Moment;
  @Input() private toDate: moment.Moment;

  @ViewChild('energyChart') private chart: BaseChartDirective;

  constructor(
    private tmpl: TemplateHelper,
    private translate: TranslateService
  ) { }

  public labels: moment.Moment[] = [];
  public datasets: Dataset[] = EMPTY_DATASET;
  public loading: boolean = true;

  private ngUnsubscribe: Subject<void> = new Subject<void>();
  private queryreplySubject: Subject<QueryReply>;

  private colors = [{
    backgroundColor: 'rgba(45,143,171,0.2)',
    borderColor: 'rgba(45,143,171,1)',
  }, {
    backgroundColor: 'rgba(0,0,0,0.2)',
    borderColor: 'rgba(0,0,0,1)',
  }, {
    backgroundColor: 'rgba(221,223,1,0.2)',
    borderColor: 'rgba(221,223,1,1)',
  }];
  private options: ChartOptions;

  ngOnInit() {
    let options = <ChartOptions>this.tmpl.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = "kW";
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      if (label == "Netz") {
        if (value < 0) {
          value *= -1;
          label = "Netzbezug";
        } else {
          label = "Netzeinspeisung";
        }
      }
      return label + ": " + value.toPrecision(2) + " kW";
    }
    this.options = options;
  }

  ngOnChanges(changes: any) {
    // close old queryreplySubject
    if (this.queryreplySubject != null) {
      this.queryreplySubject.complete();
    }
    // show loading...
    this.loading = true;
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
        activePowers.grid.push(data.grid.activePower / -1000); // convert to kW and invert value
        activePowers.production.push(data.production.activePower / 1000); // convert to kW
        activePowers.consumption.push(data.consumption.activePower / 1000); // convert to kW
      }
      this.datasets = [{
        label: this.translate.instant('General.Production'),
        data: activePowers.production
      }, {
        label: this.translate.instant('General.Grid'),
        data: activePowers.grid
      }, {
        label: this.translate.instant('General.Consumption'),
        data: activePowers.consumption
      }];
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