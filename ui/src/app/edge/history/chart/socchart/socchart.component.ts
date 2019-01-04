import { Component, Input, OnChanges, OnInit, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import { Dataset, EMPTY_DATASET } from '../../../../shared/chart';
import { Edge } from '../../../../shared/edge/edge';
import { QueryHistoricTimeseriesDataRequest } from '../../../../shared/service/jsonrpc/request/queryHistoricTimeseriesDataRequest';
import { QueryHistoricTimeseriesDataResponse } from '../../../../shared/service/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { Service } from '../../../../shared/service/service';
import { Utils } from '../../../../shared/service/utils';
import { Websocket } from '../../../../shared/service/websocket';
import { ChannelAddress } from '../../../../shared/type/channeladdress';
import { ChartOptions, DEFAULT_TIME_CHART_OPTIONS } from './../shared';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'socchart',
  templateUrl: './socchart.component.html'
})
export class SocChartComponent implements OnInit, OnChanges {

  @ViewChild('socChart') protected chart: BaseChartDirective;

  @Input() private fromDate: Date;
  @Input() private toDate: Date;

  ngOnChanges() {
    this.updateChart();
  };

  constructor(
    private service: Service,
    private route: ActivatedRoute,
    private websocket: Websocket,
    private translate: TranslateService
  ) { }

  protected labels: Date[] = [];
  protected datasets: Dataset[] = EMPTY_DATASET;
  protected loading: boolean = true;
  protected options: ChartOptions;
  protected colors = [{
    backgroundColor: 'rgba(0,152,70,0.05)',
    borderColor: 'rgba(0,152,70,1)',
  }, {
    backgroundColor: 'rgba(0,152,204,0.05)',
    borderColor: 'rgba(0,152,204,1)'
  }, {
    backgroundColor: 'rgba(107,207,0,0.05)',
    borderColor: 'rgba(107,207,0,1)'
  }, {
    backgroundColor: 'rgba(224,232,17,0.05)',
    borderColor: 'rgba(224,232,17,1)'
  }];

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  private updateChart() {
    this.loading = true;
    this.service.getCurrentEdge().then(edge => {
      this.getChannelAddresses(edge).then(channeladdresses => {
        let request = new QueryHistoricTimeseriesDataRequest(this.fromDate, this.toDate, channeladdresses);
        edge.sendRequest(this.websocket, request).then(response => {
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
              } else if (value > 100 || value < 0) {
                return null;
              } else {
                return Math.round(value);
              }
            });
            datasets.push({
              label: this.translate.instant('General.Soc') + (showComponentId ? ' (' + address.componentId + ')' : ''),
              data: data
            });
          }
          this.datasets = datasets;

          this.loading = false;

        }).catch(reason => {
          console.error(reason.message); // TODO error message
          this.initializeChart();
          return;
        })
      })
    })
  }

  private getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      if (edge.isVersionAtLeast('2018.8')) {
        resolve([new ChannelAddress('_sum', 'EssSoc')]);

      } else {
        // TODO: remove after full migration
        this.service.getConfig().then(config => {
          // get 'Soc'-Channel of all 'EssNatures'
          let channeladdresses = [];
          for (let componentId of config.getComponentsImplementingNature("EssNature")) {
            channeladdresses.push(new ChannelAddress(componentId, 'Soc'));
          }
          resolve(channeladdresses);
        }).catch(reason => reject(reason));
      }
    });
  }

  private initializeChart() {
    this.datasets = EMPTY_DATASET;
    this.labels = [];
    this.loading = false;
  }

}