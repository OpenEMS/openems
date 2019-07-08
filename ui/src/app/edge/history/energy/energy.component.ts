import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from './../shared';

@Component({
  selector: 'energy',
  templateUrl: './energy.component.html'
})
export class EnergyComponent extends AbstractHistoryChart implements OnChanges {

  @Input() private period: DefaultTypes.HistoryPeriod;

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
    this.service.setCurrentComponent('', this.route);
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
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
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
                return null
              } else if (value < 0) {
                return value / -1000; // convert to kW;
              } else {
                return 0;
              }
            });
            datasets.push({
              label: this.translate.instant('General.ChargePower'),
              data: chargeData,
              hidden: false
            });
            /*
             * Storage Discharge
             */
            let dischargeData = effectivePower.map(value => {
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
              hidden: false
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

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      if (edge.isVersionAtLeast('2018.8')) {
        let result: ChannelAddress[] = [];
        config.widgets.classes.forEach(clazz => {
          switch (clazz.toString()) {
            case 'Grid':
              result.push(new ChannelAddress('_sum', 'GridActivePower'));
              break;
            case 'Consumption':
              result.push(new ChannelAddress('_sum', 'ConsumptionActivePower'));
              break;
            case 'Storage':
              result.push(new ChannelAddress('_sum', 'EssActivePower'));
              break;
            case 'Production':
              result.push(
                new ChannelAddress('_sum', 'ProductionActivePower'),
                new ChannelAddress('_sum', 'ProductionDcActualPower'));
              break;
          };
          return false;
        });
        resolve(result);

      } else {
        this.service.getConfig().then(config => {
          let ignoreIds = config.getComponentIdsImplementingNature("FeneconMiniConsumptionMeter");
          ignoreIds.push.apply(ignoreIds, config.getComponentIdsByFactory("io.openems.impl.device.system.asymmetricsymmetriccombinationess.AsymmetricSymmetricCombinationEssNature"));

          // TODO: remove after full migration
          let result: ChannelAddress[] = [];

          // Ess
          let asymmetricEssChannels = this.getAsymmetric(config.getComponentIdsImplementingNature("AsymmetricEssNature"), ignoreIds);
          if (asymmetricEssChannels.length > 0) {
            // this is an AsymmetricEss Nature
            result.push.apply(result, asymmetricEssChannels);
          } else {
            // this is a SymmetricEss Nature
            result.push.apply(result, this.getSymmetric(config.getComponentIdsImplementingNature("SymmetricEssNature"), ignoreIds));
          }

          // Chargers
          result.push.apply(result, this.getCharger(config.getComponentIdsImplementingNature("ChargerNature"), ignoreIds));

          // Meters
          let asymmetricMeterIds = config.getComponentIdsImplementingNature("AsymmetricMeterNature");
          result.push.apply(result, this.getAsymmetric(asymmetricMeterIds, ignoreIds));
          let symmetricMeterIds = config.getComponentIdsImplementingNature("SymmetricMeterNature").filter(id => !asymmetricMeterIds.includes(id));
          result.push.apply(result, this.getSymmetric(symmetricMeterIds, ignoreIds));

          resolve(result);
        })
      }
    })
  }

  private getAsymmetric(ids: string[], ignoreIds: string[]): ChannelAddress[] {
    let result: ChannelAddress[] = [];
    for (let id of ids) {
      if (ignoreIds.includes(id)) {
        continue;
      }
      result.push.apply(result, [
        new ChannelAddress(id, 'ActivePowerL1'),
        new ChannelAddress(id, 'ActivePowerL2'),
        new ChannelAddress(id, 'ActivePowerL3'),
      ]);
    }
    return result;
  }

  private getSymmetric(ids: string[], ignoreIds: string[]): ChannelAddress[] {
    let result: ChannelAddress[] = [];
    for (let id of ids) {
      if (ignoreIds.includes(id)) {
        continue;
      }
      result.push.apply(result, [
        new ChannelAddress(id, 'ActivePower'),
      ]);
    }
    return result;
  }

  private getCharger(ids: string[], ignoreIds: string[]): ChannelAddress[] {
    let result: ChannelAddress[] = [];
    for (let id of ids) {
      if (ignoreIds.includes(id)) {
        continue;
      }
      result.push.apply(result, [
        new ChannelAddress(id, 'ActualPower'),
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
    let sumEssActivePower = [];
    let sumGridActivePower = [];
    let sumProductionActivePower = [];
    let sumProductionAcActivePower = [];
    let sumProductionDcActualPower = [];

    for (let channel of Object.keys(data)) {
      let channelAddress = ChannelAddress.fromString(channel)
      let componentId = channelAddress.componentId;
      let channelId = channelAddress.channelId;
      let natureIds = config.getNatureIdsByComponentId(componentId);

      if (natureIds.includes('EssNature') && channelId.startsWith('ActivePower')) {
        if (sumEssActivePower.length == 0) {
          sumEssActivePower = data[channel];
        } else {
          sumEssActivePower = data[channel].map((value, index) => {
            return Utils.addSafely(sumEssActivePower[index], value);
          });
        }
      }

      if (natureIds.includes('MeterNature') && channelId.startsWith('ActivePower')) {
        if (componentId === 'meter0') {
          if (sumGridActivePower.length == 0) {
            sumGridActivePower = data[channel];
          } else {
            sumGridActivePower = data[channel].map((value, index) => {
              return Utils.addSafely(sumGridActivePower[index], value);
            });
          }
        } else {
          if (sumProductionActivePower.length == 0) {
            sumProductionActivePower = data[channel];
          } else {
            sumProductionActivePower = data[channel].map((value, index) => {
              return Utils.addSafely(sumProductionActivePower[index], value);
            });
          }
          if (sumProductionAcActivePower.length == 0) {
            sumProductionAcActivePower = data[channel];
          } else {
            sumProductionAcActivePower = data[channel].map((value, index) => {
              return Utils.addSafely(sumProductionAcActivePower[index], value);
            });
          }
        }
      }

      if (natureIds.includes('ChargerNature') && channelId === 'ActualPower') {
        if (sumProductionActivePower.length == 0) {
          sumProductionActivePower = data[channel];
        } else {
          sumProductionActivePower = data[channel].map((value, index) => {
            return Utils.addSafely(sumProductionActivePower[index], value);
          });
        }
        if (sumProductionDcActualPower.length == 0) {
          sumProductionDcActualPower = data[channel];
        } else {
          sumProductionDcActualPower = data[channel].map((value, index) => {
            return Utils.addSafely(sumProductionDcActualPower[index], value);
          });
        }
      }

      data['_sum/EssActivePower'] = sumEssActivePower;
      data['_sum/GridActivePower'] = sumGridActivePower;
      data['_sum/ProductionActivePower'] = sumProductionActivePower;
      data['_sum/ProductionDcActualPower'] = sumProductionDcActualPower;
      data['_sum/ConsumptionActivePower'] = sumEssActivePower.map((ess, index) => {
        return Utils.addSafely(ess, Utils.addSafely(sumProductionAcActivePower[index], sumGridActivePower[index]));
      });
    }
  }

  private initializeChart() {
    this.datasets = EMPTY_DATASET;
    this.labels = [];
    this.loading = false;
  }

}