import { Component, Input, OnInit, OnChanges, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';
import { Subject } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import { TranslateService } from '@ngx-translate/core';

import { Edge } from '../../../../shared/edge/edge';
import { ConfigImpl } from '../../../../shared/edge/config';
import { DefaultTypes } from '../../../../shared/service/defaulttypes';
import { Dataset, EMPTY_DATASET } from '../../../../shared/shared';
import { DEFAULT_TIME_CHART_OPTIONS, ChartOptions, TooltipItem, Data } from '../shared';
import { Utils } from '../../../../shared/service/utils';
import { CurrentDataAndSummary_2018_7 } from '../../../../shared/edge/currentdata.2018.7';
import { CurrentDataAndSummary_2018_8 } from '../../../../shared/edge/currentdata.2018.8';
import { ConfigImpl_2018_8 } from '../../../../shared/edge/config.2018.8';
import { ConfigImpl_2018_7 } from '../../../../shared/edge/config.2018.7';

// TODO grid should be shown as "Netzeinspeisung"/"Netzbezug" instead of positive/negative value
@Component({
  selector: 'evcschart',
  templateUrl: './evcschart.component.html'
})
export class EvcsChartComponent implements OnChanges {

  @Input() private edge: Edge;
  @Input() private config: ConfigImpl;
  @Input() private channels: DefaultTypes.ChannelAddresses;
  @Input() private fromDate: Date;
  @Input() private toDate: Date;

  @ViewChild('evcsChart') private chart: BaseChartDirective;

  constructor(
    private utils: Utils,
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
    let options = <ChartOptions>this.utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
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
    this.edge.historicDataQuery(this.fromDate, this.toDate, this.channels).then(historicData => {
      // prepare datas array and prefill with each device

      // prepare datasets and labels
      let actualPowers: number[] = [];
      let labels: Date[] = [];
      for (let record of historicData.data) {
        for (let componentId in record.channels) {
          let d = record.channels[componentId];
          if ("ActualPower" in d) {
            actualPowers.push(Utils.divideSafely(d.ActualPower, 1000000));  // convert to kW
          }
        }
        labels.push(new Date(record.time));
      }
      this.datasets = [{
        label: this.translate.instant('General.ActualPower'),
        data: actualPowers,
        hidden: false
      }];
      this.labels = labels;
      // stop loading spinner
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
    });
  }
}