import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'heatingelementChart',
  templateUrl: '../abstracthistorychart.html'
})
export class HeatingelementChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() public period: DefaultTypes.HistoryPeriod;
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
    this.spinnerId = 'heatingelement-chart';
    this.service.startSpinner(this.spinnerId);
    this.service.setCurrentComponent('', this.route);
    this.setLabel()
  }

  ngOnDestroy() {
    this.unsubscribeChartRefresh()
  }

  protected updateChart() {
    this.autoSubscribeChartRefresh();
    this.service.startSpinner(this.spinnerId);
    this.colors = [];
    this.loading = true;
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      this.service.getCurrentEdge().then(() => {
        let result = (response as QueryHistoricTimeseriesDataResponse).result;
        // convert labels
        let labels: Date[] = [];
        for (let timestamp of result.timestamps) {
          labels.push(new Date(timestamp));
        }
        this.labels = labels;

        // convert datasets
        let datasets = [];
        let level = this.component.id + '/Level';

        if (level in result.data) {
          let levelData = result.data[level].map(value => {
            if (value == null) {
              return null
            } else {
              return value
            }
          })
          datasets.push({
            label: 'Level',
            data: levelData,
          });
          this.colors.push({
            backgroundColor: 'rgba(200,0,0,0.05)',
            borderColor: 'rgba(200,0,0,1)',
          })
        }
        this.datasets = datasets;
        this.loading = false;
        this.service.stopSpinner(this.spinnerId);
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
      let levels = new ChannelAddress(this.component.id, 'Level')
      let channeladdresses = [levels];
      resolve(channeladdresses);
    });
  }

  protected setLabel() {
    let options = this.createDefaultChartOptions();
    options.scales.yAxes[0].id = 'yAxis1'
    options.scales.yAxes[0].scaleLabel.labelString = 'Level';
    options.scales.yAxes[0].ticks.beginAtZero = true;
    options.scales.yAxes[0].ticks.max = 3;
    options.scales.yAxes[0].ticks.stepSize = 1;
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      return label + ": " + formatNumber(value, 'de', '1.0-1'); // TODO get locale dynamically
    }
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}