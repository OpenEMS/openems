import { Component, Input, OnChanges, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import { Subject } from 'rxjs';
import { DefaultTypes } from '../../../../shared/service/defaulttypes';
import { Edge, Utils } from '../../../../shared/shared';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from '../shared';

// TODO grid should be shown as "Netzeinspeisung"/"Netzbezug" instead of positive/negative value
@Component({
  selector: 'evcschart',
  templateUrl: './evcschart.component.html'
})
export class EvcsChartComponent implements OnChanges {

  @Input() private edge: Edge;
  // TODO
  @Input() private config: any;
  @Input() private channels: DefaultTypes.ChannelAddresses;
  @Input() private fromDate: Date;
  @Input() private toDate: Date;

  @ViewChild('evcsChart') private chart: BaseChartDirective;

  constructor(
    private translate: TranslateService
  ) {
    this.grid = this.translate.instant('General.Grid');
    this.gridBuy = this.translate.instant('General.GridBuy');
    this.gridSell = this.translate.instant('General.GridSell');
  }

  public labels: Date[] = [];
  public datasets: Dataset[] = EMPTY_DATASET;
  public loading: boolean = true;

  private ngUnsubscribe: Subject<void> = new Subject<void>();
  private grid: String = "";
  private gridBuy: String = "";
  private gridSell: String = "";

  private colors = [{
    // Actual Power
    backgroundColor: 'rgba(173,255,47,0.1)',
    borderColor: 'rgba(173,255,47,1)',
  }];
  private options: ChartOptions;

  ngOnInit() {
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = "kW";
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      return label + ": " + value.toPrecision(2) + " kW";
    }
    this.options = options;
  }

  ngOnChanges() {
    if (Object.keys(this.channels).length === 0) {
      this.loading = true;
      return;
    }
    this.loading = true;
    // this.edge.historicDataQuery(this.fromDate, this.toDate, this.channels)
    // .then(historicData => {
    //   // prepare datas array and prefill with each device

    //   // prepare datasets and labels
    //   let actualPowers: number[] = [];
    //   let labels: Date[] = [];
    //   for (let record of historicData.data) {
    //     for (let componentId in record.channels) {
    //       let d = record.channels[componentId];
    //       if ("ActualPower" in d) {
    //         actualPowers.push(Utils.divideSafely(d.ActualPower, 1000000));  // convert to kW
    //       }
    //     }
    //     labels.push(new Date(record.time));
    //   }
    //   this.datasets = [{
    //     label: this.translate.instant('General.ActualPower'),
    //     data: actualPowers,
    //     hidden: false
    //   }];
    //   this.labels = labels;
    //   // stop loading spinner
    //   this.loading = false;
    //   setTimeout(() => {
    //     // Workaround, because otherwise chart data and labels are not refreshed...
    //     if (this.chart) {
    //       this.chart.ngOnChanges({} as SimpleChanges);
    //     }
    //   });
    // }).catch(error => {
    //   this.datasets = EMPTY_DATASET;
    //   this.labels = [];
    // });
  }
}