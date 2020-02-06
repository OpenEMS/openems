import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { differenceInHours } from 'date-fns';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';

@Component({
  selector: 'singlethresholdSingleChart',
  templateUrl: '../abstracthistorychart.html'
})
export class SinglethresholdSingleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() private period: DefaultTypes.HistoryPeriod;
  @Input() public controllerId: string;

  ngOnChanges() {
    this.updateChart();
  };

  private outputChannel = null;
  private inputChannel = null;

  constructor(
    protected service: Service,
    protected translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super(service, translate);
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
      // convert labels
      let labels: Date[] = [];
      for (let timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;
      let datasets = [];

      // convert datasets
      for (let channel in result.data) {
        if (channel == this.outputChannel) {
          let address = ChannelAddress.fromString(channel);
          let data = result.data[channel].map(value => {
            if (value == null) {
              return null
            } else {
              return value * 100; // convert to % [0,100]
            }
          });
          datasets.push({
            label: address.channelId,
            data: data,
            hidden: false,
            yAxisID: 'yAxis1',
            position: 'left'
          });
          this.colors.push({
            backgroundColor: 'rgba(0,191,255,0.05)',
            borderColor: 'rgba(0,191,255,1)',
          })
        }
        if (channel == this.inputChannel) {
          let address = ChannelAddress.fromString(channel);
          let data = result.data[channel].map(value => {
            if (value == null) {
              return null
            } else {
              return value / 1000; // convert to kW
            }
          });
          datasets.push({
            label: address.channelId,
            data: data,
            hidden: false,
            yAxisID: 'yAxis2',
            position: 'right',
            borderDash: [10, 10]
          });
          this.colors.push({
            backgroundColor: 'rgba(45,143,171,0.05)',
            borderColor: 'rgba(45,143,171,1)'
          })
        }
      }
      this.datasets = datasets;
      this.loading = false;
    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      this.outputChannel = config.getComponentProperties(this.controllerId)['outputChannelAddress'];
      this.inputChannel = config.getComponentProperties(this.controllerId)['inputChannelAddress'];
      const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress']);
      const inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['inputChannelAddress']);
      let channeladdresses = [outputChannel, inputChannel];
      resolve(channeladdresses);
    });
  }

  protected setLabel() {
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    // adds second y-axis to chart
    options.scales.yAxes.push({
      id: 'yAxis2',
      position: 'right',
      scaleLabel: {
        display: true,
        labelString: "kW"
      },
      gridLines: {
        display: false
      },
      ticks: {
        beginAtZero: false,
      }
    })
    let outputChannel = this.outputChannel;
    let inputChannel = this.inputChannel;
    options.scales.yAxes[0].id = "yAxis1"
    options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      // if (label == ChannelAddress.fromString(outputChannel).channelId) {
      //   return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
      // } else if (label == ChannelAddress.fromString(inputChannel).channelId) {
      //   return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
      // }
      // console.log("label", ChannelAddress.fromString(outputChannel).channelId)
      return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
    }
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}