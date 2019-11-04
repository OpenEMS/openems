import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, Service, Utils, EdgeConfig } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { TranslateService } from '@ngx-translate/core';
import { getTime, differenceInMinutes } from 'date-fns/esm';
import { differenceInHours } from 'date-fns';

@Component({
  selector: 'channelthresholdSingleChart',
  templateUrl: '../abstracthistorychart.html'
})
export class ChannelthresholdSingleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() private period: DefaultTypes.HistoryPeriod;
  @Input() private controllerId: string;

  ngOnChanges() {
    this.updateChart();
  };

  constructor(
    protected service: Service,
    private route: ActivatedRoute,
    private translate: TranslateService
  ) {
    super(service);
  }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    this.setLabel();
  }

  protected updateChart() {
    this.colors = [];
    this.loading = true;
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      let result = (response as QueryHistoricTimeseriesDataResponse).result;
      let periodTime = differenceInHours(this.period.from, this.period.to);
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
            return null
          } else {
            if (value * 100 > 50) {
              value = 1;
            } else if (value * 100 < 50) {
              value = 0;
            }
            return value * 100; // convert to % [0,100]
          }
        });
        datasets.push({
          label: "Ausgang" + ' (' + address.channelId + ')',
          data: data
        });
        this.colors.push({
          backgroundColor: 'rgba(0,191,255,0.05)',
          borderColor: 'rgba(0,191,255,1)',
        })
      }
      this.datasets = datasets;
      this.loading = false;

      // calculate the effective active time in percent for widget (TODO!!)
      let compareArray = []
      this.datasets.forEach(dataset => {
        Object.values(dataset.data).forEach(data => {
          if (data == 100) {
            compareArray.push(data)
          }
        })
      })
      let timeActiveEffective = (compareArray.length / (result.timestamps.length / 100));
    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress']);
      let channeladdresses = [outputChannel];
      resolve(channeladdresses);
    });
  }

  protected setLabel() {
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      if (label == this.grid) {
        if (value < 0) {
          value *= -1;
          label = this.gridBuy;
        } else {
          label = this.gridSell;
        }
      }
      return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
    }
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.2;
  }
}