import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from '../shared';

@Component({
  selector: 'singlethresholdChart',
  templateUrl: '../abstracthistorychart.html'
})
export class SinglethresholdChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input() public period: DefaultTypes.HistoryPeriod;
  @Input() public componentId: string;

  ngOnChanges() {
    this.updateChart();
  };

  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
    private route: ActivatedRoute
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
        let outputChannel: string | string[] = config.getComponentProperties(this.componentId)['outputChannelAddress'];
        let inputChannel = config.getComponentProperties(this.componentId)['inputChannelAddress'];
        let result = (response as QueryHistoricTimeseriesDataResponse).result;
        let yAxisID;

        // set yAxis for % values (if there are no other % values: use left yAxis, if there are: use right yAxis - for percent values)
        if (result.data["_sum/EssSoc"]) {
          yAxisID = "yAxis1";
        } else {
          yAxisID = "yAxis2";
        }

        // convert labels
        let labels: Date[] = [];
        for (let timestamp of result.timestamps) {
          labels.push(new Date(timestamp));
        }
        this.labels = labels;
        let datasets = [];

        // convert datasets
        for (let channel in result.data) {
          if ((typeof outputChannel === 'string' && channel == outputChannel)
            || (typeof outputChannel !== 'string' && outputChannel.includes(channel))) {
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
              data: data,
              hidden: false,
              yAxisID: yAxisID,
              position: 'right'
            });
            this.colors.push({
              backgroundColor: 'rgba(0,191,255,0.05)',
              borderColor: 'rgba(0,191,255,1)'
            });
          }
          if (channel == inputChannel) {
            let inputLabel: string = null;
            let address = ChannelAddress.fromString(channel);
            switch (address.channelId) {
              case 'GridActivePower':
                inputLabel = this.translate.instant('General.grid');
                break;
              case 'ProductionActivePower':
                inputLabel = this.translate.instant('General.production');
                break;
              case 'EssSoc':
                inputLabel = this.translate.instant('General.soc');
                break;
              default:
                inputLabel = this.translate.instant('Edge.Index.Widgets.Singlethreshold.other');
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
                position: 'right'
              });

              this.colors.push({
                backgroundColor: 'rgba(189, 195, 199,0.05)',
                borderColor: 'rgba(189, 195, 199,1)'
              });
            } else {
              datasets.push({
                label: inputLabel,
                data: data,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'left'
              });

              this.colors.push({
                backgroundColor: 'rgba(0,0,0,0.05)',
                borderColor: 'rgba(0,0,0,1)'
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
      let result: ChannelAddress[] = [inputChannel];
      let outputChannelAddress: string | string[] = config.getComponentProperties(this.componentId)['outputChannelAddress'];
      if (typeof outputChannelAddress === 'string') {
        result.push(ChannelAddress.fromString(outputChannelAddress));
      } else {
        outputChannelAddress.forEach(c => result.push(ChannelAddress.fromString(c)));
      }
      resolve(result);
    });
  }

  protected setLabel(config: EdgeConfig) {
    let inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['inputChannelAddress']);
    let outputChannelAddress: string | string[] = config.getComponentProperties(this.componentId)['outputChannelAddress'];
    let outputChannel: ChannelAddress;
    if (typeof outputChannelAddress === 'string') {
      outputChannel = ChannelAddress.fromString(outputChannelAddress);
    } else {
      outputChannel = ChannelAddress.fromString(outputChannelAddress[0]);
    }
    let labelString;
    let options = this.createDefaultChartOptions();
    let translate = this.translate;

    if (inputChannel.channelId == 'EssSoc') {
      labelString = '%';
      options.scales.yAxes[0].id = "yAxis1";
      options.scales.yAxes[0].scaleLabel.labelString = labelString;
    } else if (inputChannel.channelId == 'GridActivePower' || inputChannel.channelId == 'ProductionActivePower') {
      labelString = 'kW';
      options.scales.yAxes[0].id = "yAxis1";
      options.scales.yAxes[0].scaleLabel.labelString = labelString;
    } else {
      labelString = config.getChannel(inputChannel)['unit'];
      options.scales.yAxes[0].id = "yAxis1";
      options.scales.yAxes[0].scaleLabel.labelString = labelString;
    }

    if (inputChannel.channelId != 'EssSoc') {
      // adds second y-axis to chart
      options.scales.yAxes.push({
        id: 'yAxis2',
        position: 'right',
        scaleLabel: {
          display: true,
          labelString: "%"
        },
        gridLines: {
          display: false
        },
        ticks: {
          beginAtZero: true,
          max: 100,
          padding: -5,
          stepSize: 20
        }
      });
    }
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      if (label == outputChannel.channelId || label == translate.instant('General.soc')) {
        return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
      } else if (label == translate.instant('General.grid') || label == translate.instant('General.production')) {
        return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
      } else {
        return label + ": " + formatNumber(value, 'de', '1.0-2') + " " + labelString;
      }
    };
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}