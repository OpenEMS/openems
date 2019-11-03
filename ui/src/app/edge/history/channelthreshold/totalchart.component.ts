import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { TranslateService } from '@ngx-translate/core';
import { getTime, differenceInMinutes } from 'date-fns/esm';

@Component({
  selector: 'channelthresholdTotalChart',
  templateUrl: '../abstracthistorychart.html'
})
export class ChannelthresholdTotalChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() private period: DefaultTypes.HistoryPeriod;

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
      let periodTime = getTime(this.period.to) - getTime(this.period.to);
      // convert labels
      let labels: Date[] = [];
      for (let timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;
      // show Channel-ID if there is more than one Channel
      let showChannelId = Object.keys(result.data).length > 1 ? true : false;

      // CALCULCATE TIME ACTIVE IN %&
      // Object.values(result.timestamps).forEach(timestamp => {
      //   let newDate = new Date(timestamp);
      // })

      // Object.values(result.data).forEach(data => {
      // })

      let datasets = [];
      // convert datasets
      Object.keys(result.data).forEach((channel, index) => {
        let address = ChannelAddress.fromString(channel);
        let data = result.data[channel].map((value, index) => {
          if (value == null) {
            return null
          } else {
            return value * 100; // convert to % [0,100]
          }
        })
        switch (index % 2) {
          case 0:
            datasets.push({
              label: "Ausgang" + (showChannelId ? ' (' + address.channelId + ')' : ''),
              data: data
            });
            this.colors.push({
              backgroundColor: 'rgba(0,191,255,0.05)',
              borderColor: 'rgba(0,191,255,1)',
            })
            break;
          case 1:
            datasets.push({
              label: "Ausgang" + (showChannelId ? ' (' + address.channelId + ')' : ''),
              data: data
            });
            this.colors.push({
              backgroundColor: 'rgba(0,0,139,0.05)',
              borderColor: 'rgba(0,0,139,1)',
            })
            break;
        }
        this.datasets = datasets;
        this.loading = false;
      })
    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      this.service.getConfig().then(config => {
        let channeladdresses = [];
        // find all ChannelThresholdControllers
        for (let controllerId of
          config.getComponentIdsImplementingNature("io.openems.impl.controller.channelthreshold.ChannelThresholdController")
            .concat(config.getComponentIdsByFactory("Controller.ChannelThreshold"))) {
          const outputChannel = ChannelAddress.fromString(config.getComponentProperties(controllerId)['outputChannelAddress']);
          channeladdresses.push(outputChannel);
        }
        resolve(channeladdresses);
      }).catch(reason => reject(reason));
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