// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';

import { AbstractHistoryChart as NewAbstractHistoryChart } from '../../../shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
  selector: 'gridOptimizedChargeChart',
  templateUrl: '../abstracthistorychart.html',
})
export class GridOptimizedChargeChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
  @Input({ required: true }) public component!: EdgeConfig.Component;

  ngOnChanges() {
    this.updateChart();
  }

  constructor(
    protected override service: Service,
    protected override translate: TranslateService,
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
        const result = (response as QueryHistoricTimeseriesDataResponse).result;

        // convert labels
        const labels: Date[] = [];
        for (const timestamp of result.timestamps) {
          labels.push(new Date(timestamp));
        }
        this.labels = labels;

        // convert datasets
        const datasets = [];

        // Delay Charge Limit data
        if (this.component.id + '/DelayChargeMaximumChargeLimit' in result.data) {

          const delayChargeData = result.data[this.component.id + '/DelayChargeMaximumChargeLimit'].map(value => {
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
            borderDash: [3, 3],
          });
          this.colors.push({
            backgroundColor: 'rgba(253,197,7,0.05)',
            borderColor: 'rgba(253,197,7,1)',
          });
        }

        // Sell to grid limit - Minimum charge limit data
        if (this.component.id + '/SellToGridLimitMinimumChargeLimit' in result.data) {
          const sellToGridLimitData = result.data[this.component.id + '/SellToGridLimitMinimumChargeLimit'].map(value => {
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
            borderDash: [3, 3],
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

          const chargeData = effectivePower.map(value => {
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
            position: 'left',
          });
          this.colors.push({
            backgroundColor: 'rgba(0,223,0,0.05)',
            borderColor: 'rgba(0,223,0,1)',
          });

          // State of charge data
          if ('_sum/EssSoc' in result.data) {
            const socData = result.data['_sum/EssSoc'].map(value => {
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
              yAxisID: ChartAxis.RIGHT,
              position: 'right',
              borderDash: [10, 10],
              unit: YAxisTitle.PERCENTAGE,
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
    }).finally(async () => {
      await this.setOptions(this.options);
      this.applyControllerSpecificOptions();
    });
  }

  private applyControllerSpecificOptions() {
    const yAxisRight: HistoryUtils.yAxes = { unit: YAxisTitle.PERCENTAGE, position: 'right', yAxisId: ChartAxis.RIGHT, displayGrid: false };
    const yAxisLeft: HistoryUtils.yAxes = { position: 'left', unit: YAxisTitle.ENERGY, yAxisId: ChartAxis.LEFT };

    const locale = this.service.translate.currentLang;
    const showYAxisTitle = true;

    [yAxisRight, yAxisLeft].forEach(yAxis => {
      this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, yAxis, this.translate, 'line', locale, showYAxisTitle);
    });

    this.datasets = this.datasets.map((el, index, arr) => {

      // align last element to right yAxis
      if ((arr.length - 1) === index) {
        el['yAxisID'] = ChartAxis.RIGHT;
      }

      return el;
    });
  }

  protected getChannelAddresses(): Promise<ChannelAddress[]> {

    return new Promise((resolve) => {
      const result: ChannelAddress[] = [
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
    this.options = this.createDefaultChartOptions();
  }

  public getChartHeight(): number {
    return window.innerHeight / 21 * 9;
  }
}
