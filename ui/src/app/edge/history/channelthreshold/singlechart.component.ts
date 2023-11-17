import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as  Chart from 'chart.js';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChartAxis, Utils } from 'src/app/shared/service/utils';

import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { calculateResolution, DEFAULT_TIME_CHART_OPTIONS } from '../shared';

@Component({
  selector: 'channelthresholdSingleChart',
  templateUrl: '../abstracthistorychart.html'
})
export class ChannelthresholdSingleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input() public period: DefaultTypes.HistoryPeriod;
  @Input() public componentId: string;

  ngOnChanges() {
    this.updateChart();
  }

  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
    private route: ActivatedRoute
  ) {
    super("channelthreshold-single-chart", service, translate);
  }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route);
  }

  ngOnDestroy() {
    this.unsubscribeChartRefresh();
  }

  protected updateChart() {
    this.autoSubscribeChartRefresh();
    this.startSpinner();
    this.colors = [];
    this.loading = true;
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      let result = (response as QueryHistoricTimeseriesDataResponse).result;
      // convert labels
      let labels: Date[] = [];
      for (let timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;

      // convert datasets
      let datasets = [];
      for (let channel in result.data) {
        let address = ChannelAddress.fromString(channel);
        let data = result.data[channel].map(value => {
          if (value == null) {
            return null;
          } else {
            return value * 100; // convert to % [0,100]
          }
        });
        datasets.push({
          label: address.channelId,
          data: data
        });
        this.colors.push({
          backgroundColor: 'rgba(0,191,255,0.05)',
          borderColor: 'rgba(0,191,255,1)'
        });
      }
      this.datasets = datasets;
      this.loading = false;
      this.stopSpinner();

    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
      let channeladdresses = [outputChannel];
      resolve(channeladdresses);
    });
  }

  protected setLabel() {
    let options: Chart.ChartOptions = Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);

    // Can be ignored when refactored
    options.scales['y'] = { display: false }

    options.plugins.legend.title = {
      text: 'test',
      position: 'start'
    }

    options.scales[ChartAxis.LEFT] = {
      max: 100,
      min: 0,
      position: 'left',
      type: 'linear',
      display: true,
      title: {
        text: this.translate.instant('General.percentage'),
        display: true
      },
      ticks: {
        padding: 5,
        stepSize: 20
      }
    };
    console.log("🚀 ~ file: singlechart.component.ts:102 ~ ChannelthresholdSingleChartComponent ~ setLabel ~ options.scales:", options.scales)

    options.plugins.legend.display = true;
    options.scales.x['time'].unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}