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
  selector: 'heatingelementChart',
  templateUrl: '../abstracthistorychart.html'
})
export class HeatingelementChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() private period: DefaultTypes.HistoryPeriod;
  @Input() public component: EdgeConfig.Component;


  ngOnChanges() {
    this.updateChart();
  };

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
      this.service.getCurrentEdge().then(() => {
        this.service.getConfig().then(config => {
          let result = (response as QueryHistoricTimeseriesDataResponse).result;
          // convert labels
          let labels: Date[] = [];
          for (let timestamp of result.timestamps) {
            labels.push(new Date(timestamp));
          }
          this.labels = labels;

          // convert datasets
          let datasets = [];
          let outputChannel1 = ChannelAddress.fromString(config.getComponentProperties(this.component.id)['outputChannelAddress1'])
          let outputChannel2 = ChannelAddress.fromString(config.getComponentProperties(this.component.id)['outputChannelAddress1'])
          let outputChannel3 = ChannelAddress.fromString(config.getComponentProperties(this.component.id)['outputChannelAddress1'])

          for (let channel in result.data) {
            if (channel == outputChannel1.toString()) {
              let data = result.data[channel].map(value => {
                if (value == null) {
                  return null
                } else {
                  return value * 100; // convert to % [0,100]
                }
              });
              datasets.push({
                label: this.translate.instant('General.phase') + ' L1',
                data: data
              });
              this.colors.push(this.phase1Color);
            }
            if (channel == outputChannel2.toString()) {
              let data = result.data[channel].map(value => {
                if (value == null) {
                  return null
                } else {
                  return value * 100; // convert to % [0,100]
                }
              });
              datasets.push({
                label: this.translate.instant('General.phase') + ' L2',
                data: data
              });
              this.colors.push(this.phase2Color);
            }
            if (channel == outputChannel3.toString()) {
              let data = result.data[channel].map(value => {
                if (value == null) {
                  return null
                } else {
                  return value * 100; // convert to % [0,100]
                }
              });
              datasets.push({
                label: this.translate.instant('General.phase') + ' L3',
                data: data
              });
              this.colors.push(this.phase3Color);
            }
          }
          this.datasets = datasets;
          this.loading = false;
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
    }).catch(reason => {
      console.error(reason); // TODO error message
      this.initializeChart();
      return;
    });
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      const outputChannel1 = ChannelAddress.fromString(config.getComponentProperties(this.component.id)['outputChannelAddress1']);
      const outputChannel2 = ChannelAddress.fromString(config.getComponentProperties(this.component.id)['outputChannelAddress2']);
      const outputChannel3 = ChannelAddress.fromString(config.getComponentProperties(this.component.id)['outputChannelAddress3']);
      let channeladdresses = [outputChannel1, outputChannel2, outputChannel3];
      resolve(channeladdresses);
    });
  }

  protected setLabel() {
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.percentage');
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
    }
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}