// @ts-strict-ignore
import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChartAxis, YAxisTitle } from 'src/app/shared/service/utils';

import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
  selector: 'singlethresholdChart',
  templateUrl: '../abstracthistorychart.html',
})
export class SinglethresholdChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
  @Input({ required: true }) public componentId!: string;
  @Input({ required: true }) public inputChannelUnit!: string;

  ngOnChanges() {
    this.updateChart();
  }

  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("singlethreshold-chart", service, translate);
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
      this.service.getConfig().then(config => {
        const outputChannel: string | string[] = config.getComponentProperties(this.componentId)['outputChannelAddress'];
        const inputChannel = config.getComponentProperties(this.componentId)['inputChannelAddress'];
        const result = (response as QueryHistoricTimeseriesDataResponse).result;
        let yAxisID;

        // set yAxis for % values (if there are no other % values: use left yAxis, if there are: use right yAxis - for percent values)
        if (result.data["_sum/EssSoc"]) {
          yAxisID = "yAxis1";
        } else {
          yAxisID = "yAxis2";
        }

        // convert labels
        const labels: Date[] = [];
        for (const timestamp of result.timestamps) {
          labels.push(new Date(timestamp));
        }
        this.labels = labels;
        const datasets = [];

        // convert datasets
        for (const channel in result.data) {
          if ((typeof outputChannel === 'string' && channel == outputChannel)
            || (typeof outputChannel !== 'string' && outputChannel.includes(channel))) {
            const address = ChannelAddress.fromString(channel);
            const data = result.data[channel].map(value => {
              if (value == null) {
                return null;
              } else {
                return value * 100; // convert to % [0,100]
              }
            });
            datasets.push({
              label: address.channelId,
              data: data,
              hidden: false,
              yAxisID: yAxisID,
              position: 'right',
            });
            this.colors.push({
              backgroundColor: 'rgba(0,191,255,0.05)',
              borderColor: 'rgba(0,191,255,1)',
            });
          }
          if (channel == inputChannel) {
            let inputLabel: string = null;
            const address = ChannelAddress.fromString(channel);
            switch (address.channelId) {
              case 'GridActivePower':
                inputLabel = this.translate.instant('GENERAL.GRID');
                break;
              case 'ProductionActivePower':
                inputLabel = this.translate.instant('GENERAL.PRODUCTION');
                break;
              case 'EssSoc':
                inputLabel = this.translate.instant('GENERAL.SOC');
                break;
              default:
                inputLabel = this.translate.instant('EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.OTHER');
                break;
            }
            let data;
            if (address.channelId == 'EssSoc') {
              data = result.data[channel].map(value => {
                if (value == null) {
                  return null;
                } else if (value > 100 || value < 0) {
                  return null;
                } else {
                  return value;
                }
              });
            } else if (address.channelId == 'ProductionActivePower' || address.channelId == 'GridActivePower') {
              data = result.data[channel].map(value => {
                if (value == null) {
                  return null;
                } else {
                  return value / 1000; // convert to kW
                }
              });
            } else {
              data = result.data[channel].map(value => {
                if (value == null) {
                  return null;
                } else {
                  return value;
                }
              });
            }
            if (address.channelId == 'EssSoc') {
              datasets.push({
                label: inputLabel,
                data: data,
                hidden: false,
                yAxisID: yAxisID,
                position: 'right',
                unit: YAxisTitle.PERCENTAGE,
              });

              this.colors.push({
                backgroundColor: 'rgba(189, 195, 199,0.05)',
                borderColor: 'rgba(189, 195, 199,1)',
              });
            } else {
              datasets.push({
                label: inputLabel,
                data: data,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'left',
              });

              this.colors.push({
                backgroundColor: 'rgba(0,0,0,0.05)',
                borderColor: 'rgba(0,0,0,1)',
              });
            }
          }
        }
        this.datasets = datasets;
        this.loading = false;
        this.stopSpinner();

      }).catch(reason => {
        console.error(reason); // TODO error message
        this.initializeChart();
        return;
      }).finally(async () => {
        this.unit = YAxisTitle.PERCENTAGE;
        await this.setOptions(this.options);
        this.addControllerSpecificOptions(this.options);
      });
    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      const inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['inputChannelAddress']);
      const result: ChannelAddress[] = [inputChannel];
      const outputChannelAddress: string | string[] = config.getComponentProperties(this.componentId)['outputChannelAddress'];
      if (typeof outputChannelAddress === 'string') {
        result.push(ChannelAddress.fromString(outputChannelAddress));
      } else {
        outputChannelAddress.forEach(c => result.push(ChannelAddress.fromString(c)));
      }
      resolve(result);
    });
  }

  protected setLabel(config: EdgeConfig) {
    this.options = this.createDefaultChartOptions();
  }

  protected addControllerSpecificOptions(options: Chart.ChartOptions) {

    this.service.getConfig().then(config => {

      const inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['inputChannelAddress']);
      const outputChannelAddress: string | string[] = config.getComponentProperties(this.componentId)['outputChannelAddress'];
      let outputChannel: ChannelAddress;
      if (typeof outputChannelAddress === 'string') {
        outputChannel = ChannelAddress.fromString(outputChannelAddress);
      } else {
        outputChannel = ChannelAddress.fromString(outputChannelAddress[0]);
      }

      let labelString;

      if (inputChannel.channelId == 'EssSoc') {
        labelString = '%';
        this.unit = YAxisTitle.PERCENTAGE;
        options.scales[ChartAxis.LEFT]['title'].text = labelString;
      } else if (inputChannel.channelId == 'GridActivePower' || inputChannel.channelId == 'ProductionActivePower') {
        labelString = 'kW';
        this.unit = YAxisTitle.ENERGY;
        options.scales[ChartAxis.LEFT]['title'].text = labelString;
      } else {
        labelString = this.inputChannelUnit;
        options.scales[ChartAxis.LEFT]['title'].text = labelString;
      }

      if (inputChannel.channelId != 'EssSoc') {
        // adds second y-axis to chart
        options.scales[ChartAxis.RIGHT] = {
          max: 100,
          position: 'right',
          title: {
            text: '%',
            display: true,
          },
          ticks: {
            padding: -5,
            stepSize: 20,
          },
        };
      }

      const translate = this.translate;
      options.plugins.tooltip.callbacks.label = function (item: Chart.TooltipItem<any>) {
        const label = item.dataset.label;
        const value = item.dataset.data[item.dataIndex];
        if (label == outputChannel.channelId || label == translate.instant('GENERAL.SOC')) {
          return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
        } else if (label == translate.instant('GENERAL.GRID') || label == translate.instant('GENERAL.PRODUCTION')) {
          return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        } else {
          return label + ": " + formatNumber(value, 'de', '1.0-2') + " " + labelString;
        }
      };

      this.options = options;
    });

  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}
