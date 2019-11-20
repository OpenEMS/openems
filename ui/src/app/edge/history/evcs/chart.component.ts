import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, Service, Utils, EdgeConfig } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
  selector: 'evcsChart',
  templateUrl: '../abstracthistorychart.html'
})
export class EvcsChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() private period: DefaultTypes.HistoryPeriod;

  ngOnChanges() {
    this.updateChart();
  };

  private config: EdgeConfig

  constructor(
    protected service: Service,
    protected translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super(service, translate);
  }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    this.service.getConfig().then(config => { this.config = config });
    this.setLabel();
  }

  protected updateChart() {
    this.loading = true;
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      let result = (response as QueryHistoricTimeseriesDataResponse).result;

      // convert labels
      let labels: Date[] = [];
      for (let timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;

      // show Component-ID if there is more than one Channel
      let showComponentId = Object.keys(result.data).length > 1 ? true : false;

      let datasets = [];
      // convert datasets
      Object.keys(result.data).forEach((channel, index) => {
        let address = ChannelAddress.fromString(channel);
        let component = this.config.getComponent(address.componentId)
        let data = result.data[channel].map(value => {
          if (value == null) {
            return null
          } else {
            return value / 1000; // convert to kW
          }
        });
        if (this.config.components[address['componentId']].factoryId == 'Evcs.Cluster') {
          datasets.push({
            label: this.translate.instant('General.ActualPower') + (showComponentId ? ' (Cluster)' : ''),
            data: data
          });
          this.colors.push({
            backgroundColor: 'rgba(0,0,0,0.1)',
            borderColor: 'rgba(0,0,0,1)',
          });
        } else {
          switch (index % 2) {
            case 0:
              datasets.push({
                label: this.translate.instant('General.ActualPower') + (showComponentId ? ' (' + component.alias + ')' : ''),
                data: data
              });
              this.colors.push({
                backgroundColor: 'rgba(255,165,0,0.1)',
                borderColor: 'rgba(255,165,0,1)',
              });
              break;
            case 1:
              datasets.push({
                label: this.translate.instant('General.ActualPower') + (showComponentId ? ' (' + component.alias + ')' : ''),
                data: data
              });
              this.colors.push({
                backgroundColor: 'rgba(255,255,0,0.1)',
                borderColor: 'rgba(255,255,0,1)',
              });
              break;
          }
        }
      })
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
      let channeladdresses = [];
      // find all EVCS components
      for (let componentId of config.getComponentIdsImplementingNature("io.openems.edge.evcs.api.Evcs")) {
        channeladdresses.push(new ChannelAddress(componentId, 'ChargePower'));
      }
      resolve(channeladdresses);
    });
  }

  protected setLabel() {
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = "kW";
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
    }
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 2.5;
  }
}

