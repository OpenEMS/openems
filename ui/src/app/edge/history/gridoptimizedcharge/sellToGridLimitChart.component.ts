// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
  selector: 'sellToGridLimitChart',
  templateUrl: '../abstracthistorychart.html',
})
export class SellToGridLimitChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
  @Input({ required: true }) public component!: EdgeConfig.Component;

  private gridMeter: string;

  ngOnChanges() {
    this.gridMeter = this.component.properties['meter.id'];
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
    this.gridMeter = this.component.properties['meter.id'];
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

        /*
        * Sell To Grid
        */
        if (this.gridMeter + '/ActivePower' in result.data) {
          const sellToGridData = result.data[this.gridMeter + '/ActivePower'].map(value => {
            if (value == null) {
              return null;
            } else if (value < 0) {
              return value / -1000; // convert to kW and invert value
            } else {
              return 0;
            }
          });
          datasets.push({
            label: this.translate.instant('General.gridSell'),
            data: sellToGridData,
            hidden: false,
          });
          this.colors.push({
            backgroundColor: 'rgba(0,0,200,0.05)',
            borderColor: 'rgba(0,0,200,1)',
          });
        }

        /*
        * Maximum sell to grid limit
        */
        if (this.component.id + '/_PropertyMaximumSellToGridPower' in result.data) {

          const sellToGridLimitData = result.data[this.component.id + '/_PropertyMaximumSellToGridPower'].map(value => {
            if (value == null) {
              return null;
            } else if (value == 0) {
              return 0;
            } else {
              return value / 1000; // convert to kW
            }
          });

          datasets.push({
            label: this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.maximumGridFeedIn'),
            data: sellToGridLimitData,
            hidden: false,
            borderDash: [3, 3],
          });
          this.colors.push({
            backgroundColor: 'rgba(0,0,0,0.05)',
            borderColor: 'rgba(0,0,0,1)',
          });

          const batterySellToGridLimitData = result.data[this.component.id + '/_PropertyMaximumSellToGridPower'].map(value => {
            if (value == null) {
              return null;
            } else if (value == 0) {
              return 0;
            } else {
              //
              return value / 1000 * 0.95; // convert to kW
            }
          });

          datasets.push({
            // TODO: Translate
            label: "Maximale Netzeinspeisung durch Batteriebeladung",
            data: batterySellToGridLimitData,
            hidden: false,
            borderDash: [3, 3],
          });
          this.colors.push({
            backgroundColor: 'rgba(200,0,0,0.05)',
            borderColor: 'rgba(200,0,0,1)',
          });
        }

        /*
        * Production
        */
        if ('_sum/ProductionActivePower' in result.data) {

          const productionData = result.data['_sum/ProductionActivePower'].map(value => {
            if (value == null) {
              return null;
            } else {
              return value / 1000; // convert to kW
            }
          });
          datasets.push({
            label: this.translate.instant('General.production'),
            data: productionData,
            hidden: false,
          });
          this.colors.push({
            backgroundColor: 'rgba(45,143,171,0.05)',
            borderColor: 'rgba(45,143,171,1)',
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
    }).finally(async () => {
      await this.setOptions(this.options);
    });
  }

  protected getChannelAddresses(): Promise<ChannelAddress[]> {

    return new Promise((resolve) => {
      const result: ChannelAddress[] = [new ChannelAddress('_sum', 'ProductionActivePower')];
      if (this.component != null && this.gridMeter != null) {
        result.push(new ChannelAddress(this.gridMeter, 'ActivePower'));
      }
      if (this.component != null && this.component.id) {
        result.push(new ChannelAddress(this.component.id, '_PropertyMaximumSellToGridPower'));
      }
      resolve(result);
    });
  }

  protected setLabel() {
    this.options = this.createDefaultChartOptions();
  }

  public getChartHeight(): number {
    //return window.innerHeight / 1.3;
    return window.innerHeight / 21 * 9;
  }
}
