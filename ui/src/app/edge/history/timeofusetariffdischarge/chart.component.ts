import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Currency, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Unit } from '../shared';
import { TimeOfUseTariffUtils } from 'src/app/shared/service/utils';

// TODO rename folder; remove 'Discharge'
@Component({
  selector: 'timeOfUseTariffDischargeChart',
  templateUrl: '../abstracthistorychart.html',
})
export class TimeOfUseTariffDischargeChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input() public period: DefaultTypes.HistoryPeriod;
  @Input() public componentId: string;

  public override edge: Edge;
  protected component: EdgeConfig.Component | null = null;
  private currencyLabel: Currency.Label; // Default

  ngOnChanges() {
    this.edge = this.service.currentEdge.value;
    this.currencyLabel = Currency.getCurrencyLabelByEdgeId(this.edge.id);
    this.updateChart();
  };

  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("timeOfUseTariffDischarge-chart", service, translate);
  }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route);
    this.service.getConfig().then(config => {
      this.component = config.getComponent(this.componentId);
    });
  }

  ngOnDestroy() {
    this.unsubscribeChartRefresh();
  }

  protected updateChart() {
    this.autoSubscribeChartRefresh();
    this.startSpinner();
    this.colors = [];
    this.loading = true;

    this.queryHistoricTimeseriesData(this.period.from, this.period.to, { value: 15, unit: Unit.MINUTES }).then(response => {
      this.service.getConfig().then(config => {
        let result = (response as QueryHistoricTimeseriesDataResponse).result;

        // convert datasets
        let datasets = [];
        let quarterlyPrices = this.componentId + '/QuarterlyPrices';
        let timeOfUseTariffState = this.componentId + '/StateMachine';

        if (timeOfUseTariffState in result.data && quarterlyPrices in result.data) {
          // Size of the data
          const size = result.data[timeOfUseTariffState].length;
          // get chart data.
          const scheduleChartData = TimeOfUseTariffUtils.getScheduleChartData(size, result.data[quarterlyPrices], result.data[timeOfUseTariffState], result.timestamps, this.translate, this.component.factoryId);

          datasets = scheduleChartData.datasets;
          this.colors = scheduleChartData.colors;
          this.labels = scheduleChartData.labels;
        }

        // State of charge data
        if ('_sum/EssSoc' in result.data) {
          let socData = result.data['_sum/EssSoc'].map(value => {
            if (value == null) {
              return null;
            } else if (value > 100 || value < 0) {
              return null;
            } else {
              return value;
            }
          });
          datasets.push({
            type: 'line',
            label: this.translate.instant('General.soc'),
            data: socData,
            hidden: false,
            yAxisID: 'yAxis2',
            position: 'right',
            borderDash: [10, 10],
            order: 1,
          });
          this.colors.push({
            backgroundColor: 'rgba(189, 195, 199,0.2)',
            borderColor: 'rgba(189, 195, 199,1)',
          });
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
      let channels: ChannelAddress[] = [
        new ChannelAddress(this.componentId, 'QuarterlyPrices'),
        new ChannelAddress(this.componentId, 'StateMachine'),
        new ChannelAddress('_sum', 'EssSoc'),
      ];

      resolve(channels);
    });
  }

  protected setLabel(config: EdgeConfig) {
    let options = this.createDefaultChartOptions();
    let translate = this.translate;
    const currencyLabel: string = this.currencyLabel;

    // Scale prices y-axis between min-/max-values, not from zero
    // options.scales.yAxes[0].ticks.beginAtZero = false;

    // // Adds second y-axis to chart
    // options.scales.yAxes.push({
    //   id: 'yAxis2',
    //   position: 'right',
    //   scaleLabel: {
    //     display: true,
    //     labelString: "%",
    //     padding: -2,
    //     fontSize: 11,
    //   },
    //   gridLines: {
    //     display: false,
    //   },
    //   ticks: {
    //     beginAtZero: true,
    //     max: 100,
    //     padding: -5,
    //     stepSize: 20,
    //   },
    // });
    // options.layout = {
    //   padding: {
    //     left: 2,
    //     right: 2,
    //     top: 0,
    //     bottom: 0,
    //   },
    // };

    // options.scales.xAxes[0].stacked = true;

    // //x-axis
    // if (differenceInDays(this.service.historyPeriod.value.to, this.service.historyPeriod.value.from) >= 5) {
    //   options.scales.xAxes[0].time.unit = "day";
    // } else {
    //   options.scales.xAxes[0].time.unit = "hour";
    // }

    // //y-axis
    // options.scales.yAxes[0].id = "yAxis1";
    // options.scales.yAxes[0].scaleLabel.labelString = currencyLabel;
    // options.scales.yAxes[0].scaleLabel.padding = -2;
    // options.scales.yAxes[0].scaleLabel.fontSize = 11;
    // options.scales.yAxes[0].ticks.padding = -5;
    // options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
    //   let label = data.datasets[tooltipItem.datasetIndex].label;
    //   let value = tooltipItem.yLabel;

    //   if (!value) {
    //     return;
    //   }
    //   if (label == translate.instant('General.soc')) {
    //     return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
    //   } else {
    //     return label + ": " + formatNumber(value, 'de', '1.0-4') + ' ' + currencyLabel;
    //   }
    // };
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 1.3;
  }
}
