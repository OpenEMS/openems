import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
  selector: 'fixDigitalOutputTotalChart',
  templateUrl: '../abstracthistorychart.html',
})
export class FixDigitalOutputTotalChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input() public period: DefaultTypes.HistoryPeriod;

  ngOnChanges() {
    this.updateChart();
  };

  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("fixdigitaloutput-total-chart", service, translate);
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


      let datasets = [];
      // convert datasets
      Object.keys(result.data).forEach((channel, index) => {
        let address = ChannelAddress.fromString(channel);
        let data = result.data[channel].map((value) => {
          if (value == null) {
            return null;
          } else {
            return value * 100; // convert to % [0,100]
          }
        });
        switch (index % 2) {
          case 0:
            datasets.push({
              label: address.channelId,
              data: data,
            });
            this.colors.push({
              backgroundColor: 'rgba(0,191,255,0.05)',
              borderColor: 'rgba(0,191,255,1)',
            });
            break;
          case 1:
            datasets.push({
              label: address.channelId,
              data: data,
            });
            this.colors.push({
              backgroundColor: 'rgba(0,0,139,0.05)',
              borderColor: 'rgba(0,0,139,1)',
            });
            break;
        }
        this.datasets = datasets;
        this.loading = false;
        this.stopSpinner();

      });
    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      this.service.getConfig().then(config => {
        let channeladdresses = [];
        // find all FixIoControllers
        config.getComponentsByFactory('Controller.Io.FixDigitalOutput').forEach(component => {
          const outputChannel = ChannelAddress.fromString(config.getComponentProperties(component.id)['outputChannelAddress']);
          channeladdresses.push(outputChannel);
        });
        resolve(channeladdresses);
      }).catch(reason => reject(reason));
    });
  }

  protected setLabel() {
    let options = this.createDefaultChartOptions();
    // options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.percentage');
    options.plugins.tooltip.callbacks.label = function (tooltipItem: Chart.TooltipItem<any>) {

      let label = tooltipItem.label;
      let value = tooltipItem.dataset[tooltipItem.dataIndex];
      return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
      // let label = data.datasets[tooltipItem.datasetIndex].label;
      // let value = tooltipItem.yLabel;
      // return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
    };
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}
