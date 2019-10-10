import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from './../shared';

@Component({
  selector: 'soc',
  templateUrl: './soc.component.html'
})
export class SocComponent extends AbstractHistoryChart implements OnInit, OnChanges {

  @Input() private period: DefaultTypes.HistoryPeriod;

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
    // State Of Charge
    backgroundColor: 'rgba(0,223,0,0.05)',
    borderColor: 'rgba(0,223,0,1)',
  }, {
    // Autarchy
    backgroundColor: 'rgba(0,152,204,0.05)',
    borderColor: 'rgba(0,152,204,1)'
  }, {
    // Self Consumption
    backgroundColor: 'rgba(253,197,7,0.05)',
    borderColor: 'rgba(253,197,7,1)'
  }];

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
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
      return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
    }
    options.scales.yAxes[0].ticks.max = 100;
    this.options = options;
  }

  private updateChart() {
    this.loading = true;
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
      this.service.getCurrentEdge().then(edge => {
        this.service.getConfig().then(config => {
          let result = response.result;
          // convert labels
          let labels: Date[] = [];
          for (let timestamp of result.timestamps) {
            labels.push(new Date(timestamp));
          }
          this.labels = labels;

          // convert datasets
          let datasets = [];

          // required data for autarchy and self consumption
          let buyFromGridData: number[] = [];
          let sellToGridData: number[] = [];
          let consumptionData: number[] = [];
          let dischargeData: number[] = [];
          let productionData: number[] = [];

          if (!edge.isVersionAtLeast('2018.8')) {
            this.convertDeprecatedData(config, result.data); // TODO deprecated
          }

          if ('_sum/ConsumptionActivePower' in result.data) {
            /*
             * Consumption
             */
            consumptionData = result.data['_sum/ConsumptionActivePower'].map(value => {
              if (value == null) {
                return null
              } else {
                return value;
              }
            });
          }

          if ('_sum/EssActivePower' in result.data) {
            /*
             * Storage Discharge
             */
            let effectivePower;
            if ('_sum/ProductionDcActualPower' in result.data && result.data['_sum/ProductionDcActualPower'].length > 0) {
              effectivePower = result.data['_sum/ProductionDcActualPower'].map((value, index) => {
                return Utils.subtractSafely(result.data['_sum/EssActivePower'][index], value);
              });
            } else {
              effectivePower = result.data['_sum/EssActivePower'];
            }
            dischargeData = effectivePower.map(value => {
              if (value == null) {
                return null
              } else if (value > 0) {
                return value;
              } else {
                return 0;
              }
            });
          };

          if ('_sum/GridActivePower' in result.data) {
            /*
             * Buy From Grid
             */
            buyFromGridData = result.data['_sum/GridActivePower'].map(value => {
              if (value == null) {
                return null
              } else if (value > 0) {
                return value;
              } else {
                return 0;
              }
            })

            /*
             * Sell To Grid
             */
            sellToGridData = result.data['_sum/GridActivePower'].map(value => {
              if (value == null) {
                return null
              } else if (value < 0) {
                return value * -1; // invert value
              } else {
                return 0;
              }
            });
          };

          if ('_sum/ProductionActivePower' in result.data) {
            /*
             * Production
             */
            productionData = result.data['_sum/ProductionActivePower'].map(value => {
              if (value == null) {
                return null
              } else {
                return value;
              }
            });
          }

          if ('_sum/EssSoc' in result.data) {
            /*
             * State-of-charge
             */
            let data = result.data['_sum/EssSoc'].map(value => {
              if (value == null) {
                return null
              } else if (value > 100 || value < 0) {
                return null;
              } else {
                return value;
              }
            })

            /*
            * Autarchy
            */
            if (config.hasProducer()) {
              let autarchy = consumptionData.map((value, index) => {
                if (value == null) {
                  return null
                } else {
                  return CurrentData.calculateAutarchy(buyFromGridData[index], value);
                }
              })

              datasets.push({
                label: this.translate.instant('General.Autarchy'),
                data: autarchy,
                hidden: false
              })

              /*
              * Self Consumption
              */
              let selfConsumption = productionData.map((value, index) => {
                if (value == null) {
                  return null
                } else {
                  return CurrentData.calculateSelfConsumption(sellToGridData[index], value, dischargeData[index]);
                }
              })

              datasets.push({
                label: this.translate.instant('General.SelfConsumption'),
                data: selfConsumption,
                hidden: false
              })
            }

            datasets.push({
              label: this.translate.instant('General.Soc'),
              data: data,
              hidden: false
            });
          };

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
        if (config.hasProducer()) {
          config.widgets.classes.forEach(clazz => {
            switch (clazz.toString()) {
              case 'Grid':
                result.push(new ChannelAddress('_sum', 'GridActivePower'));
                break;
              case 'Consumption':
                result.push(new ChannelAddress('_sum', 'ConsumptionActivePower'));
                break;
              case 'Storage':
                result.push(
                  new ChannelAddress('_sum', 'EssSoc'),
                  new ChannelAddress('_sum', 'EssActivePower'));
                break;
              case 'Production':
                result.push(
                  new ChannelAddress('_sum', 'ProductionActivePower'),
                  new ChannelAddress('_sum', 'ProductionDcActualPower'));
                break;
            };
            return false;
          })
          resolve(result);
        }
        else {
          resolve([new ChannelAddress('_sum', 'EssSoc')]);
        };
      } else {
        // TODO: remove after full migration
        this.service.getConfig().then(config => {
          let ignoreIds = config.getComponentIdsByFactory("io.openems.impl.device.system.asymmetricsymmetriccombinationess.AsymmetricSymmetricCombinationEssNature");

          let result: ChannelAddress[] = [];

          // get 'Soc'-Channel of all 'EssNatures'
          result.push.apply(result, this.getSoc(config.getComponentIdsImplementingNature("EssNature"), ignoreIds));

          resolve(result);
        }).catch(reason => reject(reason));
      }
    });
  }

  private getSoc(ids: string[], ignoreIds: string[]): ChannelAddress[] {
    let result: ChannelAddress[] = [];
    for (let id of ids) {
      if (ignoreIds.includes(id)) {
        continue;
      }
      result.push.apply(result, [
        new ChannelAddress(id, 'Soc'),
      ]);
    }
    return result;
  }

  private initializeChart() {
    this.datasets = EMPTY_DATASET;
    this.labels = [];
    this.loading = false;
  }

  /**
 * Calculates '_sum' values.
 * 
 * @param data 
 */
  private convertDeprecatedData(config: EdgeConfig, data: { [channelAddress: string]: any[] }) {
    let sumEssSoc = [];

    for (let channel of Object.keys(data)) {
      let channelAddress = ChannelAddress.fromString(channel)
      let componentId = channelAddress.componentId;
      let channelId = channelAddress.channelId;
      let natureIds = config.getNatureIdsByComponentId(componentId);

      if (natureIds.includes('EssNature') && channelId == 'Soc') {
        if (sumEssSoc.length == 0) {
          sumEssSoc = data[channel];
        } else {
          sumEssSoc = data[channel].map((value, index) => {
            return Utils.addSafely(sumEssSoc[index], value);
          });
        }
      }
    }
    data['_sum/EssSoc'] = sumEssSoc.map((value, index) => {
      return Utils.divideSafely(sumEssSoc[index], Object.keys(data).length);
    });

  }

}