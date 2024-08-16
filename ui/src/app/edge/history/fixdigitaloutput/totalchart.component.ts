// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { YAxisTitle } from 'src/app/shared/service/utils';

import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
  selector: 'fixDigitalOutputTotalChart',
  templateUrl: '../abstracthistorychart.html',
})
export class FixDigitalOutputTotalChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;

  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("fixdigitaloutput-total-chart", service, translate);
  }

  ngOnChanges() {
    this.updateChart();
  }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route);
  }

  ngOnDestroy() {
    this.unsubscribeChartRefresh();
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
  protected updateChart() {
    this.autoSubscribeChartRefresh();
    this.startSpinner();
    this.colors = [];
    this.loading = true;
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      const result = (response as QueryHistoricTimeseriesDataResponse).result;
      // convert labels
      const labels: Date[] = [];
      for (const timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;


      const datasets = [];
      // convert datasets
      Object.keys(result.data).forEach((channel, index) => {
        const address = ChannelAddress.fromString(channel);
        const data = result.data[channel]?.map((value) => {
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
    }).finally(async () => {
      this.unit = YAxisTitle.PERCENTAGE;
      this.formatNumber = '1.0-0';
      await this.setOptions(this.options);
    });
  }

  protected getChannelAddresses(): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      this.service.getConfig().then(config => {
        const channeladdresses = [];
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
    this.options = this.createDefaultChartOptions();
  }

}
