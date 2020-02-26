import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
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
          let inputLabel: string = null;
          let address = ChannelAddress.fromString(channel);
          switch (address.channelId) {
            case 'GridActivePower':
              inputLabel = this.translate.instant('General.Grid');
              break;
            case 'ProductionActivePower':
              inputLabel = this.translate.instant('General.Production');
              break;
            case 'EssSoc':
              inputLabel = this.translate.instant('General.Soc');
              break;
            default:
              inputLabel = this.translate.instant('Edge.Index.Widgets.Singlethreshold.other');
              break;
          }
          let data
          if (address.channelId == 'EssSoc') {
            data.result.data[channel].map(value => {
              if (value == null) {
                return null
              } else if (value > 100 || value < 0) {
                return null;
              } else {
                return value;
              }
            })
          } else if (address.channelId == 'ProductionActivePower' || address.channelId == 'GridActivePower') {
            data = result.data[channel].map(value => {
              if (value == null) {
                return null
              } else {
                return value / 1000; // convert to kW
              }
            });
          } else {
            data = result.data[channel].map(value => {
              if (value == null) {
                return null
              } else {
                return value;
              }
            });
          }
          datasets.push({
            label: inputLabel,
            data: data,
            hidden: false,
            yAxisID: 'yAxis2',
            position: 'right',
          });
          if (address.channelId == 'EssSoc') {
            this.colors.push({
              backgroundColor: 'rgba(189, 195, 199,0.05)',
              borderColor: 'rgba(189, 195, 199,1)',
            })
          } else {
            this.colors.push({
              backgroundColor: 'rgba(0,0,0,0.05)',
              borderColor: 'rgba(0,0,0,1)'
            })
          }
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
    return new Promise((resolve) => {
      this.outputChannel = config.getComponentProperties(this.controllerId)['outputChannelAddress'];
      this.inputChannel = config.getComponentProperties(this.controllerId)['inputChannelAddress'];
      const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['outputChannelAddress']);
      const inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['inputChannelAddress']);
      let channeladdresses = [outputChannel, inputChannel];
      resolve(channeladdresses);
    });
  }

  protected setLabel(config: EdgeConfig) {
    let inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['inputChannelAddress']);
    let labelString;
    if (inputChannel.channelId == 'Soc') {
      labelString = '%';
    } else if (inputChannel.channelId == 'GridActivePower' || inputChannel.channelId == 'ProductionActivePower') {
      labelString = 'kW';
    } else {
      labelString = config.getChannel(ChannelAddress.fromString(config.getComponentProperties(this.controllerId)['inputChannelAddress']))['unit'];
    }
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    let translate = this.translate;
    // adds second y-axis to chart
    options.scales.yAxes.push({
      id: 'yAxis2',
      position: 'right',
      scaleLabel: {
        display: true,
        labelString: labelString
      },
      gridLines: {
        display: false
      },
      ticks: {
        beginAtZero: false,
      }
    })
    options.scales.yAxes[0].id = "yAxis1"
    options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      if (label.includes('Relay') || label == translate.instant('General.Soc')) {
        return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
      } else if (label == translate.instant('General.Grid') || label == translate.instant('General.Production')) {
        return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
      } else {
        return label + ": " + formatNumber(value, 'de', '1.0-2') + " " + labelString;
      }
    }
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}