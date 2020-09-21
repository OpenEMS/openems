import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils, Websocket } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from './../shared';
import { Component, Input, OnChanges } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { EnergyModalComponent } from './modal/modal.component';
import { format, isSameDay, isSameMonth, isSameYear } from 'date-fns';
import { addDays, subDays } from 'date-fns/esm';
import { ModalController } from '@ionic/angular';
import { QueryHistoricTimeseriesDataResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { QueryHistoricTimeseriesExportXlxsRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs';
import { TranslateService } from '@ngx-translate/core';
import * as FileSaver from 'file-saver';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { queryHistoricTimeseriesEnergyPerPeriodResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse';
import { ChartData, ChartDataSets, ChartLegendItem, ChartLegendLabelItem } from 'chart.js';
import { formatNumber } from '@angular/common';

@Component({
  selector: 'energy',
  templateUrl: './energy.component.html'
})
export class EnergyComponent extends AbstractHistoryChart implements OnChanges {

  private static readonly EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8';
  private static readonly EXCEL_EXTENSION = '.xlsx';

  public chartType: string = "line";

  @Input() private period: DefaultTypes.HistoryPeriod;

  ngOnChanges() {
    this.updateChart();
  };

  constructor(
    protected service: Service,
    protected translate: TranslateService,
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    private websocket: Websocket,
    private unitpipe: UnitvaluePipe,
  ) {
    super(service, translate);
  }

  // EXPORT WILL MOVE TO MODAL WHEN KWH ARE READY

  /**
   * Export historic data to Excel file.
   */
  public exportToXlxs() {
    this.service.getCurrentEdge().then(edge => {
      // TODO the order of these channels should be reflected in the excel file
      let dataChannels = [
        new ChannelAddress('_sum', 'EssActivePower'),
        // Grid
        new ChannelAddress('_sum', 'GridActivePower'),
        // Production
        new ChannelAddress('_sum', 'ProductionActivePower'),
        // Consumption
        new ChannelAddress('_sum', 'ConsumptionActivePower')
      ];
      let energyChannels = [
        // new ChannelAddress('_sum', 'EssSoc'),
        // new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
        // new ChannelAddress('_sum', 'GridSellActiveEnergy'),
        // new ChannelAddress('_sum', 'ProductionActiveEnergy'),
        // new ChannelAddress('_sum', 'ConsumptionActiveEnergy'),
        // new ChannelAddress('_sum', 'EssActiveChargeEnergy'),
        // new ChannelAddress('_sum', 'EssActiveDischargeEnergy')
      ];
      edge.sendRequest(this.websocket, new QueryHistoricTimeseriesExportXlxsRequest(this.service.historyPeriod.from, this.service.historyPeriod.to, dataChannels, energyChannels)).then(response => {
        let r = response as Base64PayloadResponse;
        var binary = atob(r.result.payload.replace(/\s/g, ''));
        var len = binary.length;
        var buffer = new ArrayBuffer(len);
        var view = new Uint8Array(buffer);
        for (var i = 0; i < len; i++) {
          view[i] = binary.charCodeAt(i);
        }
        const data: Blob = new Blob([view], {
          type: EnergyComponent.EXCEL_TYPE
        });

        let fileName = "Export-" + edge.id + "-";
        let dateFrom = this.service.historyPeriod.from;
        let dateTo = this.service.historyPeriod.to;
        if (isSameDay(dateFrom, dateTo)) {
          fileName += format(dateFrom, "dd.MM.yyyy");
        } else if (isSameMonth(dateFrom, dateTo)) {
          fileName += format(dateFrom, "dd.") + "-" + format(dateTo, "dd.MM.yyyy");
        } else if (isSameYear(dateFrom, dateTo)) {
          fileName += format(dateFrom, "dd.MM.") + "-" + format(dateTo, "dd.MM.yyyy");
        } else {
          fileName += format(dateFrom, "dd.MM.yyyy") + "-" + format(dateTo, "dd.MM.yyyy");
        }
        fileName += EnergyComponent.EXCEL_EXTENSION;
        FileSaver.saveAs(data, fileName);

      }).catch(reason => {
        console.warn(reason);
      })
    })
  }

  ngOnInit() {
    this.spinnerId = "energy-chart";
    this.service.setCurrentComponent('', this.route);
    this.service.startSpinner(this.spinnerId);
    // Timeout is used to prevent ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => this.getChartHeight(), 500);
    this.subscribeChartRefresh()
  }

  ngOnDestroy() {
    this.unsubscribeChartRefresh()
  }

  private isKwhChart(service: Service): boolean {
    let edge: Edge | null = null;
    service.getCurrentEdge().then(currentEdge => {
      edge = currentEdge;
    })

    if ((service.periodString != "week" && service.periodString != "month") || service.isKwhAllowed(edge) == false) {
      return false;
    } else if ((service.periodString == "week" || service.periodString == "month") && service.isKwhAllowed(edge) == true) {
      return true;
    } else {
      return false;
    }
  }

  protected updateChart() {
    this.loading = true;
    this.service.startSpinner(this.spinnerId);
    this.service.getCurrentEdge().then(edge => {
      this.service.getConfig().then(config => {
        if (this.isKwhChart(this.service) == false) {
          this.chartType = "line";
          this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
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

            // push data for right y-axis
            if ('_sum/EssSoc' in result.data) {
              let socData = result.data['_sum/EssSoc'].map(value => {
                if (value == null) {
                  return null
                } else if (value > 100 || value < 0) {
                  return null;
                } else {
                  return value;
                }
              })
              datasets.push({
                label: this.translate.instant('General.soc'),
                data: socData,
                hidden: false,
                yAxisID: 'yAxis2',
                position: 'right',
                borderDash: [10, 10]
              })
              this.colors.push({
                backgroundColor: 'rgba(189, 195, 199,0.05)',
                borderColor: 'rgba(189, 195, 199,1)',
              })
            }

            // push data for left y-axis
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
                label: this.translate.instant('General.production'),
                data: productionData,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'left'
              });
              this.colors.push({
                backgroundColor: 'rgba(45,143,171,0.05)',
                borderColor: 'rgba(45,143,171,1)'
              })
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
                label: this.translate.instant('General.gridBuy'),
                data: buyFromGridData,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'left'
              });
              this.colors.push({
                backgroundColor: 'rgba(0,0,0,0.05)',
                borderColor: 'rgba(0,0,0,1)'
              })

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
                label: this.translate.instant('General.gridSell'),
                data: sellToGridData,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'left'
              });
              this.colors.push({
                backgroundColor: 'rgba(0,0,200,0.05)',
                borderColor: 'rgba(0,0,200,1)',
              })
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
                label: this.translate.instant('General.consumption'),
                data: consumptionData,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'left'
              });
              this.colors.push({
                backgroundColor: 'rgba(253,197,7,0.05)',
                borderColor: 'rgba(253,197,7,1)',
              })
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
                label: this.translate.instant('General.chargePower'),
                data: chargeData,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'left'
              });
              this.colors.push({
                backgroundColor: 'rgba(0,223,0,0.05)',
                borderColor: 'rgba(0,223,0,1)',
              })
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
                label: this.translate.instant('General.dischargePower'),
                data: dischargeData,
                hidden: false,
                yAxisID: 'yAxis1',
                position: 'left'
              });
              this.colors.push({
                backgroundColor: 'rgba(200,0,0,0.05)',
                borderColor: 'rgba(200,0,0,1)',
              })
            }

            if (this.service.isKwhAllowed(edge) == true) {
              let kwhChannels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'ProductionActiveEnergy'),
                new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
                new ChannelAddress('_sum', 'GridSellActiveEnergy'),
                new ChannelAddress('_sum', 'EssActiveChargeEnergy'),
                new ChannelAddress('_sum', 'EssActiveDischargeEnergy'),
                new ChannelAddress('_sum', 'ConsumptionActiveEnergy')
              ]
              this.service.queryEnergy(this.period.from, this.period.to, kwhChannels).then(response => {
                let kwhProductionValue = response.result.data["_sum/ProductionActiveEnergy"];
                let kwhGridBuyValue = response.result.data["_sum/GridBuyActiveEnergy"];
                let kwhGridSellValue = response.result.data["_sum/GridSellActiveEnergy"];
                let kwhChargeValue = response.result.data["_sum/EssActiveChargeEnergy"];
                let kwhDischargeValue = response.result.data["_sum/EssActiveDischargeEnergy"];
                let kwhConsumptionValue = response.result.data["_sum/ConsumptionActiveEnergy"];

                datasets.forEach(dataset => {
                  switch (dataset.label) {
                    case this.translate.instant('General.production'): {
                      dataset.label = this.translate.instant('General.production') + " " + this.unitpipe.transform(kwhProductionValue, "kWh").toString();
                      break;
                    }
                    case this.translate.instant('General.gridBuy'): {
                      dataset.label = this.translate.instant('General.gridBuy') + " " + this.unitpipe.transform(kwhGridBuyValue, "kWh").toString();
                      break;
                    }
                    case this.translate.instant('General.gridSell'): {
                      dataset.label = this.translate.instant('General.gridSell') + " " + this.unitpipe.transform(kwhGridSellValue, "kWh").toString();
                      break;
                    }
                    case this.translate.instant('General.chargePower'): {
                      dataset.label = this.translate.instant('General.chargePower') + " " + this.unitpipe.transform(kwhChargeValue, "kWh").toString();
                      break;
                    }
                    case this.translate.instant('General.dischargePower'): {
                      dataset.label = this.translate.instant('General.dischargePower') + " " + this.unitpipe.transform(kwhDischargeValue, "kWh").toString()
                      break;
                    }
                    case this.translate.instant('General.consumption'): {
                      dataset.label = this.translate.instant('General.consumption') + " " + this.unitpipe.transform(kwhConsumptionValue, "kWh").toString()
                      break;
                    }
                  }
                })
                this.datasets = datasets;
                this.loading = false;
                this.service.stopSpinner(this.spinnerId);
              })
            } else {
              this.datasets = datasets;
              this.loading = false;
              this.service.stopSpinner(this.spinnerId);
            }
          }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
          });
        } else if (this.isKwhChart(this.service) == true) {
          this.chartType = "bar";
          this.getEnergyChannelAddresses(edge, config).then(channelAddresses => {
            let resolution: number = 0 // resolution for value per day
            switch (this.service.periodString) {
              case "week": {
                resolution = 86400;
              }
              case "month": {
                resolution = 2629746;
              }
            }
            this.queryHistoricTimeseriesEnergyPerPeriod(addDays(this.period.from, 1), this.period.to, channelAddresses, resolution).then(response => {
              let result = (response as queryHistoricTimeseriesEnergyPerPeriodResponse).result;

              // convert datasets
              let datasets: ChartDataSets[] = [];

              // convert labels
              let labels: Date[] = [];
              for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
              }
              this.labels = labels;

              // Direct Consumption

              let directConsumptionData: null | number[] = null;

              if ('_sum/ProductionActiveEnergy' && '_sum/EssActiveChargeEnergy' && '_sum/GridSellActiveEnergy' in result.data) {
                let directConsumption = [];
                result.data['_sum/ProductionActiveEnergy'].forEach((value, index) => {
                  directConsumption.push(value - result.data['_sum/GridSellActiveEnergy'][index] - result.data['_sum/EssActiveChargeEnergy'][index]);
                });

                directConsumptionData = directConsumption.map(value => {
                  if (value == null) {
                    return null
                  } else {
                    return value / 1000; // convert to kWh
                  }
                });
              }

              // left stack

              /*
               * Direct Consumption
               */
              if (directConsumptionData != null) {
                datasets.push({
                  borderWidth: {
                    right: 2,
                    left: 2
                  },
                  label: "Direktverbrauch",
                  data: directConsumptionData,
                  borderColor: 'rgba(0,0,0,1)',
                  backgroundColor: 'rgba(244,164,96,0.6)',
                  barPercentage: 0.7,
                  categoryPercentage: 0.4,
                  stack: "0"
                })
              }

              /*
               * Storage Charge
               */
              if ('_sum/EssActiveChargeEnergy' in result.data) {
                let chargeData = result.data['_sum/EssActiveChargeEnergy'].map(value => {
                  if (value == null) {
                    return null
                  } else {
                    return value / 1000; // convert to kWh
                  }
                });
                datasets.push({
                  borderWidth: {
                    right: 2,
                    left: 2
                  },
                  label: "Beladung",
                  data: chargeData,
                  borderColor: 'rgba(0,0,0,1)',
                  backgroundColor: 'rgba(0,223,0,0.6)',
                  barPercentage: 0.7,
                  categoryPercentage: 0.4,
                  stack: "0"
                })
              }

              /*
               * Sell to Grid
               */
              if ('_sum/GridSellActiveEnergy' in result.data) {
                let gridSellData = result.data['_sum/GridSellActiveEnergy'].map(value => {
                  if (value == null) {
                    return null
                  } else {
                    return value / 1000; // convert to kWh
                  }
                });
                datasets.push({
                  borderWidth: {
                    top: 2,
                    right: 2,
                    left: 2
                  },
                  label: "Netzeinspeisung",
                  data: gridSellData,
                  borderColor: 'rgba(0,0,0,1)',
                  backgroundColor: 'rgba(0,0,200,0.6)',
                  barPercentage: 0.7,
                  categoryPercentage: 0.4,
                  stack: "0"
                })
              }

              // right stack

              /*
               * Direct Consumption
               */
              if (directConsumptionData != null) {
                datasets.push({
                  borderWidth: {
                    right: 2,
                    left: 2
                  },
                  label: "Direktverbrauch",
                  data: directConsumptionData,
                  borderColor: 'rgba(0,0,0,1)',
                  backgroundColor: 'rgba(244,164,96,0.6)',
                  barPercentage: 0.7,
                  categoryPercentage: 0.4,
                  stack: "1"
                })
              }

              /*
               * Storage Discharge
               */
              if ('_sum/EssActiveDischargeEnergy' in result.data) {
                let dischargeData = result.data['_sum/EssActiveDischargeEnergy'].map(value => {
                  if (value == null) {
                    return null
                  } else {
                    return value / 1000; // convert to kW
                  }
                });
                datasets.push({
                  borderWidth: {
                    right: 2,
                    left: 2
                  },
                  label: "Entladung",
                  data: dischargeData,
                  borderColor: 'rgba(0,0,0,1)',
                  backgroundColor: 'rgba(200,0,0,0.6)',
                  barPercentage: 0.7,
                  categoryPercentage: 0.4,
                  stack: "1"
                })
              }

              /*
               * Buy from Grid
               */
              if ('_sum/GridBuyActiveEnergy' in result.data) {
                let gridBuyData = result.data['_sum/GridBuyActiveEnergy'].map(value => {
                  if (value == null) {
                    return null
                  } else {
                    return value / 1000; // convert to kW
                  }
                });
                datasets.push({
                  borderWidth: {
                    top: 2,
                    right: 2,
                    left: 2
                  },
                  label: "Netzbezug",
                  data: gridBuyData,
                  borderColor: 'rgba(0,0,0,1)',
                  backgroundColor: 'rgba(0,0,0,0.6)',
                  barPercentage: 0.7,
                  categoryPercentage: 0.4,
                  stack: "1"
                })
              }
              this.datasets = datasets;
              this.colors = [];
              this.loading = false;
              this.service.stopSpinner(this.spinnerId);
            })
          })
        }
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

  private getEnergyChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      let result: ChannelAddress[] = [];
      config.widgets.classes.forEach(clazz => {
        switch (clazz.toString()) {
          case 'Grid':
            result.push(new ChannelAddress('_sum', 'GridBuyActiveEnergy'))
            result.push(new ChannelAddress('_sum', 'GridSellActiveEnergy'));
            break;
          case 'Storage':
            result.push(new ChannelAddress('_sum', 'EssActiveChargeEnergy'))
            result.push(new ChannelAddress('_sum', 'EssActiveDischargeEnergy'));
            break;
          case 'Production':
            result.push(
              new ChannelAddress('_sum', 'ProductionActiveEnergy'))
            break;
        };
        return false;
      });
      resolve(result)
    })
  }

  protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
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
              result.push(new ChannelAddress('_sum', 'EssSoc'))
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

  protected setLabel() {
    let translate = this.translate;
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    let currentEdge;
    this.service.getCurrentEdge().then(edge => {
      currentEdge = edge;
    })
    if (this.service.periodString == "week" && this.service.isKwhAllowed(currentEdge) == true) {
      options.responsive = true;
      options.scales.xAxes[0].time.unit = 'day';
      options.scales.xAxes[0].bounds = 'ticks';
      options.scales.xAxes[0].ticks.source = 'data';
      options.scales.xAxes[0].stacked = true;
      options.scales.xAxes[0].offset = true;
      options.legend.labels = {
        filter(legendItem: ChartLegendLabelItem, data: ChartData) {
          let index = legendItem.datasetIndex;
          let stack = data.datasets[index].stack;
          if (legendItem.text == "Direktverbrauch" && stack == "1") {
            return false;
          } else {
            return true;
          }
        }
      }
    } else {
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
      })
      options.scales.yAxes[0].id = "yAxis1"
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
      }
    }
    this.options = options;
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

  public getChartHeight(): number {
    return window.innerHeight / 2;
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: EnergyModalComponent,
    });
    return await modal.present();
  }
}