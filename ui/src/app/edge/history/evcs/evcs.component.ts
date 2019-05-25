import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from '../shared';

@Component({
  selector: 'evcs',
  templateUrl: './evcs.component.html'
})
export class EvcsComponent extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() private fromDate: Date;
  @Input() private toDate: Date;

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
    // Actual Power
    backgroundColor: 'rgba(173,255,47,0.1)',
    borderColor: 'rgba(173,255,47,1)',
  }];

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = "kW";
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
    }
    this.options = options;
  }

  private updateChart() {
    this.loading = true;
    this.queryHistoricTimeseriesData(this.fromDate, this.toDate).then(response => {
      let result = (response as QueryHistoricTimeseriesDataResponse).result;

      // convert labels
      let labels: Date[] = [];
      for (let timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;

      // show Component-ID if there is more than one Channel
      let showComponentId = Object.keys(result.data).length > 1 ? true : false;

      // convert datasets
      let datasets = [];
      for (let channel in result.data) {
        let address = ChannelAddress.fromString(channel);
        let data = result.data[channel].map(value => {
          if (value == null) {
            return null
          } else {
            return value / 1000; // convert to kW
          }
        });
        datasets.push({
          label: this.translate.instant('General.ActualPower') + (showComponentId ? ' (' + address.componentId + ')' : ''),
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
        // find all EVCS components
        for (let componentId of config.getComponentIdsImplementingNature("io.openems.edge.evcs.api.Evcs")) {
          channeladdresses.push(new ChannelAddress(componentId, 'ChargePower'));
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