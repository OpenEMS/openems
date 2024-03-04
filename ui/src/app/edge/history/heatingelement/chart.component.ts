import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import type { ChartOptions } from 'chart.js';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChartAxis, YAxisTitle } from 'src/app/shared/service/utils';

import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
  selector: 'heatingelementChart',
  templateUrl: '../abstracthistorychart.html',
})
export class HeatingelementChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input() public period: DefaultTypes.HistoryPeriod;
  @Input() public component: EdgeConfig.Component;

  ngOnChanges() {
    this.updateChart();
  }

  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("heatingelement-chart", service, translate);
  }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route);
    this.setLabel();
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
      this.service.getCurrentEdge().then(() => {
        let result = (response as QueryHistoricTimeseriesDataResponse).result;
        // convert labels
        let labels: Date[] = [];
        for (let timestamp of result.timestamps) {
          labels.push(new Date(timestamp));
        }
        this.labels = labels;

        // convert datasets
        let datasets = [];
        let level = this.component.id + '/Level';

        if (level in result.data) {
          let levelData = result.data[level].map(value => {
            if (value == null) {
              return null;
            } else {
              return value;
            }
          });
          datasets.push({
            label: 'Level',
            data: levelData,
          });
          this.colors.push({
            backgroundColor: 'rgba(200,0,0,0.05)',
            borderColor: 'rgba(200,0,0,1)',
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

    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    }).finally(async () => {
      this.formatNumber = '1.0-1';
      this.unit = YAxisTitle.NONE;
      await this.setOptions(this.options);
      this.applyControllerSpecificOptions(this.options);
    });;
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      let levels = new ChannelAddress(this.component.id, 'Level');
      let channeladdresses = [levels];
      resolve(channeladdresses);
    });
  }

  protected applyControllerSpecificOptions(options: ChartOptions) {
    const translate = this.translate;
    options.scales[ChartAxis.LEFT]['title'].text = 'Level';
    options.scales[ChartAxis.LEFT]['beginAtZero'] = true;
    options.scales[ChartAxis.LEFT].max = 3;
    options.scales[ChartAxis.LEFT].ticks['stepSize'] = 1;
    this.options = options;
  }

  protected setLabel() {
    this.options = this.createDefaultChartOptions();
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}
