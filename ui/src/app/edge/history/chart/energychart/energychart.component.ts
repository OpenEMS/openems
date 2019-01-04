import { Component, Input, OnChanges, ViewChild } from '@angular/core';
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
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from './../shared';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'energychart',
  templateUrl: './energychart.component.html'
})
export class EnergyChartComponent implements OnChanges {

  @ViewChild('energyChart') protected chart: BaseChartDirective;

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
  private colors = [{
    // Production
    backgroundColor: 'rgba(45,143,171,0.1)',
    borderColor: 'rgba(45,143,171,1)',
  }, {
    // Grid Buy
    backgroundColor: 'rgba(0,0,0,0.1)',
    borderColor: 'rgba(0,0,0,1)',
  }, {
    // Grid Sell
    backgroundColor: 'rgba(0,0,200,0.1)',
    borderColor: 'rgba(0,0,200,1)',
  }, {
    // Consumption
    backgroundColor: 'rgba(221,223,1,0.1)',
    borderColor: 'rgba(221,223,1,1)',
  }, {
    // Storage Charge
    backgroundColor: 'rgba(0,223,0,0.1)',
    borderColor: 'rgba(0,223,0,1)',
  }, {
    // Storage Discharge
    backgroundColor: 'rgba(200,0,0,0.1)',
    borderColor: 'rgba(200,0,0,1)',
  }];

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = "kW";
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let label = data.datasets[tooltipItem.datasetIndex].label;
      let value = tooltipItem.yLabel;
      if (label == this.grid) {
        if (value < 0) {
          value *= -1;
          label = this.gridBuy;
        } else {
          label = this.gridSell;
        }
      }
      return label + ": " + value.toPrecision(3) + " kW";
    }
    this.options = options;
  }

  private updateChart() {
    this.loading = true;
    this.service.getCurrentEdge().then(edge => {
      let request = new QueryHistoricTimeseriesDataRequest(this.fromDate, this.toDate, this.getChannelAddresses(edge));
      edge.sendRequest(this.websocket, request).then(response => {
        let result = (response as QueryHistoricTimeseriesDataResponse).result;

        // convert labels
        let labels: Date[] = [];
        for (let timestamp of result.timestamps) {
          labels.push(new Date(timestamp));
        }
        this.labels = labels;

        // convert datasets
        let datasets = [];

        this.convertDeprecatedData(result.data); // TODO deprecated

        if ('_sum/ProductionActivePower' in result.data) {
          /*
           * Production
           */
          let productionData = result.data['_sum/ProductionActivePower'].map(value => {
            if (value == null) {
              return null
            } else {
              return Math.round(value / 1000); // convert to kW
            }
          });
          datasets.push({
            label: this.translate.instant('General.Production'),
            data: productionData,
            hidden: false
          });
        }

        if ('_sum/GridActivePower' in result.data) {
          /*
           * Buy From Grid
           */
          let buyFromGridData = result.data['_sum/GridActivePower'].map(value => {
            if (value == null) {
              return null
            } else if (value > 0) {
              return Math.round(value / 1000); // convert to kW
            } else {
              return 0;
            }
          });
          datasets.push({
            label: this.translate.instant('General.GridBuy'),
            data: buyFromGridData,
            hidden: false
          });

          /*
           * Sell To Grid
           */
          let sellToGridData = result.data['_sum/GridActivePower'].map(value => {
            if (value == null) {
              return null
            } else if (value < 0) {
              return Math.round(value / -1000); // convert to kW and invert value
            } else {
              return 0;
            }
          });
          datasets.push({
            label: this.translate.instant('General.GridSell'),
            data: sellToGridData,
            hidden: false
          });
        }

        if ('_sum/ConsumptionActivePower' in result.data) {
          /*
           * Consumption
           */
          let consumptionData = result.data['_sum/ConsumptionActivePower'].map(value => {
            if (value == null) {
              return null
            } else {
              return Math.round(value / 1000); // convert to kW
            }
          });
          datasets.push({
            label: this.translate.instant('General.Consumption'),
            data: consumptionData,
            hidden: false
          });
        }

        if ('_sum/EssActivePower' in result.data) {
          /*
           * Storage Charge
           */
          let chargeData = result.data['_sum/EssActivePower'].map(value => {
            if (value == null) {
              return null
            } else if (value < 0) {
              return Math.round(value / -1000); // convert to kW and invert value
            } else {
              return 0;
            }
          });
          datasets.push({
            label: this.translate.instant('General.ChargePower'),
            data: chargeData,
            hidden: true
          });

          /*
           * Storage Discharge
           */
          let dischargeData = result.data['_sum/EssActivePower'].map(value => {
            if (value == null) {
              return null
            } else if (value > 0) {
              return Math.round(value / 1000); // convert to kW
            } else {
              return 0;
            }
          });
          datasets.push({
            label: this.translate.instant('General.DischargePower'),
            data: dischargeData,
            hidden: true
          });
        }

        this.datasets = datasets;

        this.loading = false;

      }).catch(reason => {
        console.error(reason.message); // TODO error message
        this.initializeChart();
        return;
      });
    });
  }

  private getChannelAddresses(edge: Edge): ChannelAddress[] {
    if (edge.isVersionAtLeast('2018.8')) {
      return [
        // Ess
        new ChannelAddress('_sum', 'EssActivePower'),
        // Grid
        new ChannelAddress('_sum', 'GridActivePower'),
        // Production
        new ChannelAddress('_sum', 'ProductionActivePower'),
        // Consumption
        new ChannelAddress('_sum', 'ConsumptionActivePower')
      ];
    } else {
      // TODO: remove after full migration
      return [
        new ChannelAddress('ess0', 'ActivePower'),
        new ChannelAddress('meter0', 'ActivePower'),
        new ChannelAddress('meter1', 'ActivePower')
      ];
    }
  }

  /**
   * Calculates '_sum' values.
   * 
   * @param data 
   */
  private convertDeprecatedData(data: { [channelAddress: string]: any[] }) {
    if ('meter1/ActivePower' in data) {
      data['_sum/ProductionActivePower'] = data['meter1/ActivePower'];
    }
    if ('ess0/ActivePower' in data) {
      data['_sum/EssActivePower'] = data['ess0/ActivePower'];
    }
    if ('meter0/ActivePower' in data) {
      data['_sum/GridActivePower'] = data['meter0/ActivePower'];
    }
    if ('meter1/ActivePower' in data && 'ess0/ActivePower' in data && 'meter0/ActivePower' in data) {
      data['_sum/ConsumptionActivePower'] = data['meter1/ActivePower'].map((meter1, index) => {
        let meter0 = data['meter0/ActivePower'][index];
        let ess0 = data['ess0/ActivePower'][index];
        return Utils.addSafely(meter1, Utils.addSafely(meter0, ess0));
      });
    }
  }

  private initializeChart() {
    this.datasets = EMPTY_DATASET;
    this.labels = [];
    this.loading = false;
  }

}