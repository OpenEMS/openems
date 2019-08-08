import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from '../shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
  selector: 'channelthreshold',
  templateUrl: './channelthreshold.component.html'
})
export class ChannelthresholdComponent extends AbstractHistoryChart implements OnInit, OnChanges {

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

  public loading: boolean = true;

  protected labels: Date[] = [];
  protected datasets: Dataset[] = EMPTY_DATASET;
  protected options: ChartOptions;
  protected colors = [{
    backgroundColor: 'rgba(204,204,204,0.1)',
    borderColor: 'rgba(204,204,204,1)',
  }, {
    backgroundColor: 'rgba(153,153,153,0.1)',
    borderColor: 'rgba(153,153,153,1)',
  }, {
    backgroundColor: 'rgba(102,102,102,0.1)',
    borderColor: 'rgba(102,102,102,1)',
  }, {
    backgroundColor: 'rgba(0,0,0,0.1)',
    borderColor: 'rgba(0,0,0,1)',
  }];

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = "%";
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      return label + ": " + formatNumber(value, 'de', '1.0-2') + " %";
    }
    this.options = options;
  }

  private updateChart() {
    this.loading = true;
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      let result = (response as QueryHistoricTimeseriesDataResponse).result;

      // convert labels
      let labels: Date[] = [];
      for (let timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;

      // show Channel-ID if there is more than one Channel
      let showChannelId = Object.keys(result.data).length > 1 ? true : false;

      // convert datasets
      let datasets = [];
      for (let channel in result.data) {
        let address = ChannelAddress.fromString(channel);
        let data = result.data[channel].map(value => {
          if (value == null) {
            return null
          } else {
            return value * 100; // convert to % [0,100]]
          }
        });
        datasets.push({
          label: "Ausgang" + (showChannelId ? ' (' + address.channelId + ')' : ''),
          data: data
        });
      }
      this.datasets = datasets;

      this.loading = false;

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

  private initializeChart() {
    this.datasets = EMPTY_DATASET;
    this.labels = [];
    this.loading = false;
  }

}