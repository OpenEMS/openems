import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { differenceInDays } from 'date-fns';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';

@Component({
  selector: 'gridOptimizedChargeChart',
  templateUrl: '../abstracthistorychart.html'
})
export class GridOptimizedChargeChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input() public period: DefaultTypes.HistoryPeriod;
  @Input() public component: EdgeConfig.Component;

  ngOnChanges() {
    this.updateChart();
  }

  constructor(
    protected service: Service,
    protected translate: TranslateService,
    private route: ActivatedRoute,
  ) {
    super("gridOptimizedCharge-chart", service, translate);
  }

  ngOnInit() {
    this.startSpinner();
    this.service.setCurrentComponent('', this.route);
    this.setLabel();
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

        // Delay Charge Limit data
        if (this.component.id + '/DelayChargeMaximumChargeLimit' in result.data) {

          let delayChargeData = result.data[this.component.id + '/DelayChargeMaximumChargeLimit'].map(value => {
            if (value == null) {
              return null;
            } else if (value <= 0) {
              return 0;
            } else {
              return value / 1000; // convert to kW
            }
          });
          datasets.push({
            label: this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.maximumCharge'),
            data: delayChargeData,
            hidden: false,
            borderDash: [3, 3]
          });
          this.colors.push({
            backgroundColor: 'rgba(253,197,7,0.05)',
            borderColor: 'rgba(253,197,7,1)',
          });
        }

        // Sell to grid limit - Minimum charge limit data
        if (this.component.id + '/SellToGridLimitMinimumChargeLimit' in result.data) {
          let sellToGridLimitData = result.data[this.component.id + '/SellToGridLimitMinimumChargeLimit'].map(value => {
            if (value == null) {
              return null;
            } else if (value == 0) {
              return 0;
            } else if (value < 0) {
              return 0;
            } else {
              return value / 1000; // convert to kW
            }
          });
          datasets.push({
            label: this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.minimumCharge'),
            data: sellToGridLimitData,
            hidden: false,
            borderDash: [3, 3]
          });
          this.colors.push({
            backgroundColor: 'rgba(200,0,0,0.05)',
            borderColor: 'rgba(200,0,0,1)',
          });
        }

        if ('_sum/EssActivePower' in result.data) {
          /*
           * Storage Charge
           */
          let effectivePower;
          if ('_sum/ProductionDcActualPower' in result.data && result.data['_sum/ProductionDcActualPower'].length > 0) {
            effectivePower = result.data['_sum/ProductionDcActualPower'].map((value, index) => {
              return Utils.subtractSafely(result.data['_sum/EssActivePower'][index], value);
            });
          } else {
            effectivePower = result.data['_sum/EssActivePower'];
          }

          let chargeData = effectivePower.map(value => {
            if (value == null) {
              return null;
            } else if (value < 0) {
              return value / -1000; // convert to kW;
            } else {
              return 0;
            }
          });

          datasets.push({
            label: this.translate.instant('General.chargePower'),
            data: chargeData,
            hidden: false,
            yAxisID: 'yAxis1',
            position: 'left'
          });
          this.colors.push({
            backgroundColor: 'rgba(0,223,0,0.05)',
            borderColor: 'rgba(0,223,0,1)',
          });

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
              label: this.translate.instant('General.soc'),
              data: socData,
              hidden: false,
              yAxisID: 'yAxis2',
              position: 'right',
              borderDash: [10, 10]
            });
            this.colors.push({
              backgroundColor: 'rgba(189, 195, 199,0.05)',
              borderColor: 'rgba(189, 195, 199,1)',
            });
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

  protected getChannelAddresses(): Promise<ChannelAddress[]> {

    return new Promise((resolve) => {
      let result: ChannelAddress[] = [
        new ChannelAddress('_sum', 'EssActivePower'),
        new ChannelAddress('_sum', 'ProductionDcActualPower'),
        new ChannelAddress('_sum', 'EssSoc'),
      ];
      if (this.component != null && this.component.id) {
        result.push(new ChannelAddress(this.component.id, 'DelayChargeMaximumChargeLimit'));
        result.push(new ChannelAddress(this.component.id, 'SellToGridLimitMinimumChargeLimit'));
      }
      resolve(result);
    });
  }

  protected setLabel() {
    let translate = this.translate;
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    // adds second y-axis to chart
    options.scales.yAxes.push({
      id: 'yAxis2',
      position: 'right',
      scaleLabel: {
        display: true,
        labelString: "%",
        padding: -2,
        fontSize: 11
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
    options.layout = {
      padding: {
        left: 2,
        right: 2,
        top: 0,
        bottom: 0
      }
    };
    //x-axis
    if (differenceInDays(this.service.historyPeriod.to, this.service.historyPeriod.from) >= 5) {
      options.scales.xAxes[0].time.unit = "day";
    } else {
      options.scales.xAxes[0].time.unit = "hour";
    }

    //y-axis
    options.scales.yAxes[0].id = "yAxis1";
    options.scales.yAxes[0].scaleLabel.labelString = "kW";
    options.scales.yAxes[0].scaleLabel.padding = -2;
    options.scales.yAxes[0].scaleLabel.fontSize = 11;
    options.scales.yAxes[0].ticks.padding = -5;
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      if (label.split(" ").length > 1) {
        label = label.split(" ").slice(0, 1).toString();

      }

      let value = tooltipItem.yLabel;
      if (label == translate.instant('General.soc')) {
        return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
      } else {
        return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
      }
    };
    this.options = options;
  }

  public getChartHeight(): number {
    return window.innerHeight / 21 * 9;
  }

}