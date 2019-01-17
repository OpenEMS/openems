import { Component, Input, OnChanges, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import { QueryHistoricTimeseriesDataResponse } from '../../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, Service, Utils, EdgeConfig } from '../../../../shared/shared';
import { AbstractHistoryChart } from '../../abstracthistorychart';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from './../shared';
import { formatNumber } from '@angular/common';

@Component({
  selector: 'energychart',
  templateUrl: './energychart.component.html'
})
export class EnergyChartComponent extends AbstractHistoryChart implements OnChanges {

  @ViewChild('energyChart') protected chart: BaseChartDirective;

  @Input() private fromDate: Date;
  @Input() private toDate: Date;

  ngOnChanges() {
    this.updateChart();
  };

  public loading: boolean = true;

  constructor(
    protected service: Service,
    private route: ActivatedRoute,
    private translate: TranslateService
  ) {
    super(service);
  }

  protected labels: Date[] = [];
  protected datasets: Dataset[] = EMPTY_DATASET;
  protected options: ChartOptions;
  protected colors = [{
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
      return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
    }
    this.options = options;
  }

  private updateChart() {
    this.loading = true;
    this.queryHistoricTimeseriesData(this.fromDate, this.toDate).then(response => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(config => {

          let result = (response as QueryHistoricTimeseriesDataResponse).result;

          // convert labels
          let labels: Date[] = [];
          for (let timestamp of result.timestamps) {
            labels.push(new Date(timestamp));
          }
          this.labels = labels;

          // convert datasets
          let datasets = [];

          if (!edge.isVersionAtLeast('2018.8')) {
            this.convertDeprecatedData(config, result.data); // TODO deprecated
          }

          if ('_sum/ProductionActivePower' in result.data) {
            /*
            * Production
            */
            let productionData = result.data['_sum/ProductionActivePower'].map(value => {
              if (value == null) {
                return null
              } else {
                return value / 1000; // convert to kW
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
                return value / 1000; // convert to kW
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
                return value / -1000; // convert to kW and invert value
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
                return value / 1000; // convert to kW
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
                return value / -1000; // convert to kW and invert value
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
                return value / 1000; // convert to kW
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
          console.error(reason); // TODO error message
          this.initializeChart();
          return;
        });
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

  protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      if (edge.isVersionAtLeast('2018.8')) {
        resolve([
          // Ess
          new ChannelAddress('_sum', 'EssActivePower'),
          // Grid
          new ChannelAddress('_sum', 'GridActivePower'),
          // Production
          new ChannelAddress('_sum', 'ProductionActivePower'),
          // Consumption
          new ChannelAddress('_sum', 'ConsumptionActivePower')
        ]);
      } else {
        this.service.getConfig().then(config => {
          // TODO: remove after full migration
          let result: ChannelAddress[] = [];
          result.push.apply(result, this.getAsymmetric(config.getComponentsImplementingNature("FeneconMiniEss")));
          result.push.apply(result, this.getAsymmetric(config.getComponentsImplementingNature("FeneconMiniGridMeter")));
          result.push.apply(result, this.getAsymmetric(config.getComponentsImplementingNature("FeneconMiniProductionMeter")));

          result.push.apply(result, this.getAsymmetric(config.getComponentsImplementingNature("FeneconProEss")));
          result.push.apply(result, this.getAsymmetric(config.getComponentsImplementingNature("FeneconProPvMeter")));

          result.push.apply(result, this.getSymmetric(config.getComponentsImplementingNature("FeneconCommercialAC")));
          result.push.apply(result, this.getSymmetric(config.getComponentsImplementingNature("FeneconCommercialDC")));

          result.push.apply(result, this.getAsymmetric(config.getComponentsImplementingNature("AsymmetricMeterNature")));
          result.push.apply(result, this.getSymmetric(config.getComponentsImplementingNature("SymmetricMeterNature")));
          resolve(result);
        })
      }
    })
  }

  private getAsymmetric(ids: string[]): ChannelAddress[] {
    let result: ChannelAddress[] = [];
    for (let id of ids) {
      result.push.apply(result, [
        new ChannelAddress(id, 'ActivePowerL1'),
        new ChannelAddress(id, 'ActivePowerL2'),
        new ChannelAddress(id, 'ActivePowerL3'),
      ]);
    }
    return result;
  }

  private getSymmetric(ids: string[]): ChannelAddress[] {
    let result: ChannelAddress[] = [];
    for (let id of ids) {
      result.push.apply(result, [
        new ChannelAddress(id, 'ActivePower'),
      ]);
    }
    return result;
  }

  /**
   * Calculates '_sum' values.
   * 
   * @param data 
   */
  private convertDeprecatedData(config: EdgeConfig, data: { [channelAddress: string]: any[] }) {
    let sumEssSoc = [];
    let sumEssActivePower = [];
    let sumGridActivePower = [];
    let sumProductionActivePower = [];
    for (let channel of Object.keys(data)) {
      let channelAddress = ChannelAddress.fromString(channel)
      let componentId = channelAddress.componentId;
      let channelId = channelAddress.channelId;
      let natures = config.getNaturesByComponentId(componentId);

      if (natures.includes('EssNature') && channelId === 'Soc') {
        if (sumEssSoc.length == 0) {
          sumEssSoc = data[channel];
        } else {
          sumEssSoc = data[channel].map((value, index) => {
            return (sumEssSoc[index] + value) / 2;
          });
        }
      }

      if (natures.includes('EssNature') && channelId.startsWith('ActivePower')) {
        if (sumEssActivePower.length == 0) {
          sumEssActivePower = data[channel];
        } else {
          sumEssActivePower = data[channel].map((value, index) => {
            return sumEssActivePower[index] + value;
          });
        }
      }

      if (natures.includes('MeterNature') && channelId.startsWith('ActivePower')) {
        if (componentId === 'meter0') {
          if (sumGridActivePower.length == 0) {
            sumGridActivePower = data[channel];
          } else {
            sumGridActivePower = data[channel].map((value, index) => {
              return sumGridActivePower[index] + value;
            });
          }
        } else {
          if (sumProductionActivePower.length == 0) {
            sumProductionActivePower = data[channel];
          } else {
            sumProductionActivePower = data[channel].map((value, index) => {
              return sumProductionActivePower[index] + value;
            });
          }
        }
      }

      data['_sum/EssSoc'] = sumEssSoc;
      data['_sum/EssActivePower'] = sumEssActivePower;
      data['_sum/GridActivePower'] = sumGridActivePower;
      data['_sum/ProductionActivePower'] = sumProductionActivePower;
      data['_sum/ConsumptionActivePower'] = sumEssActivePower.map((ess, index) => {
        return ess + Utils.addSafely(sumProductionActivePower[index], sumGridActivePower[index]);
      });
    }
  }

  private initializeChart() {
    this.datasets = EMPTY_DATASET;
    this.labels = [];
    this.loading = false;
  }

}