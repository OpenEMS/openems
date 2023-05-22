import { formatNumber } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController, Platform } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';
import { ChartDataSets, ChartLegendLabelItem, ChartTooltipItem } from 'chart.js';
import { differenceInCalendarMonths, format, isSameDay, isSameMonth, isSameYear } from 'date-fns';
import { saveAs } from 'file-saver-es';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { QueryHistoricTimeseriesExportXlxsRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils, Websocket } from 'src/app/shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { calculateResolution, ChartData, ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, isLabelVisible, setLabelVisible, TooltipItem, Unit } from '../shared';
import { EnergyModalComponent } from './modal/modal.component';

type EnergyChartLabels = {
  production: string,
  gridBuy: string,
  gridSell: string,
  charge: string,
  discharge: string,
  consumption: string,
  directConsumption: string,
  stateOfCharge: string
}

@Component({
  selector: 'energy',
  templateUrl: './energy.component.html'
})
export class EnergyComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

  private static readonly EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8';
  private static readonly EXCEL_EXTENSION = '.xlsx';

  private config: EdgeConfig | null = null;
  private stopOnDestroy: Subject<void> = new Subject<void>();

  public chartType: string = "line";
  public isExcelExportAllowed: boolean = true;

  @Output() setShowWarning: EventEmitter<boolean> = new EventEmitter()
  @Input() public period: DefaultTypes.HistoryPeriod;

  ngOnChanges() {
    this.updateChart();
  }

  constructor(
    protected service: Service,
    protected translate: TranslateService,
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    private websocket: Websocket,
    private unitpipe: UnitvaluePipe,
    private platform: Platform
  ) {
    super("energy-chart", service, translate);
  }

  // EXPORT WILL MOVE TO MODAL WHEN KWH ARE READY

  /**
   * Export historic data to Excel file.
   */
  public exportToXlxs() {
    this.isExcelExportAllowed = differenceInCalendarMonths(this.service.historyPeriod.value.to, this.service.historyPeriod.value.from) < 3;
    if (!this.isExcelExportAllowed) {
      return
    }

    this.startSpinner();
    this.service.getCurrentEdge().then(edge => {
      edge.sendRequest(this.websocket, new QueryHistoricTimeseriesExportXlxsRequest(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to)).then(response => {
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
        let dateFrom = this.service.historyPeriod.value.from;
        let dateTo = this.service.historyPeriod.value.to;
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
        saveAs(data, fileName);

      }).catch(reason => {
        console.warn(reason);
      })
    })
    this.stopSpinner();
  }


  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    this.startSpinner();

    this.platform.ready().then(() => {
      this.service.isSmartphoneResolutionSubject.pipe(takeUntil(this.stopOnDestroy)).subscribe(value => {
        this.updateChart();
      })
    })
    // Timeout is used to prevent ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => this.getChartHeight(), 500);
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
    this.unsubscribeChartRefresh();
  }

  /**
   * checks if kWh Chart is allowed to be shown
   */
  private isBarChart(service: Service): boolean {
    let unit = calculateResolution(this.service, this.period.from, this.period.to).resolution.unit;
    if (unit == Unit.DAYS || unit == Unit.MONTHS) {
      return true;
    }
    return false;
  }

  protected updateChart() {
    this.colors = [];
    this.datasets = [];
    this.loading = true;
    this.startSpinner();
    this.autoSubscribeChartRefresh();
    this.service.getCurrentEdge().then(edge => {
      this.service.getConfig().then(config => {
        this.edge = edge;
        this.config = config;
        this.edge = edge;
        this.datasets = [];

        // Load Linechart or BarChart 
        this.generateLabels().then(chartLabels => {
          if (this.isBarChart(this.service)) {
            this.loadBarChart(chartLabels, config);
          } else {
            this.loadLineChart(chartLabels);
          }
        })
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

  private loadLineChart(chartLabels: EnergyChartLabels) {
    this.chartType = "line";
    this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {

      if (Utils.isDataEmpty(response)) {
        return
      }

      let result = (response as QueryHistoricTimeseriesDataResponse).result;

      // convert labels
      let labels: Date[] = [];
      for (let timestamp of result.timestamps) {
        labels.push(new Date(timestamp));
      }
      this.labels = labels;

      // convert datasets
      let datasets = [];

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
          label: chartLabels.stateOfCharge,
          data: socData,
          hidden: !isLabelVisible(chartLabels.stateOfCharge),
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
          label: chartLabels.production,
          data: productionData,
          hidden: !isLabelVisible(chartLabels.production),
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
          label: chartLabels.gridBuy,
          data: buyFromGridData,
          hidden: !isLabelVisible(chartLabels.gridBuy),
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
          label: chartLabels.gridSell,
          data: sellToGridData,
          hidden: !isLabelVisible(chartLabels.gridSell),
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
          label: chartLabels.consumption,
          data: consumptionData,
          hidden: !isLabelVisible(chartLabels.consumption),
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
          label: chartLabels.charge,
          data: chargeData,
          hidden: !isLabelVisible(chartLabels.charge),
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
          label: chartLabels.discharge,
          data: dischargeData,
          hidden: !isLabelVisible(chartLabels.discharge),
          yAxisID: 'yAxis1',
          position: 'left'
        });
        this.colors.push({
          backgroundColor: 'rgba(200,0,0,0.05)',
          borderColor: 'rgba(200,0,0,1)',
        })
      }

      // Save Original OnClick because calling onClick overwrites default function
      var original = Chart.defaults.global.legend.onClick;
      Chart.defaults.global.legend.onClick = function (event: MouseEvent, legendItem: ChartLegendLabelItem) {
        let chart: Chart = this.chart;
        let legendItemIndex = legendItem.datasetIndex;

        // Set @Angular SessionStorage for Labels to check if they are hidden
        setLabelVisible(legendItem.text, !chart.isDatasetVisible(legendItemIndex));
        original.call(this, event, legendItem);
      }

      this.datasets = datasets;
      this.loading = false;
      this.stopSpinner();

    }).catch(reason => {
      console.error(reason); // TODO error message
      this.setShowWarning.emit(true)
      return;
    });
  }

  private loadBarChart(chartLabels: EnergyChartLabels, config: EdgeConfig) {
    this.chartType = "bar";
    this.getEnergyChannelAddresses(config).then(channelAddresses => {
      this.queryHistoricTimeseriesEnergyPerPeriod(this.period.from, this.period.to, channelAddresses).then(response => {

        if (Utils.isDataEmpty(response)) {
          return
        }

        let result = (response as QueryHistoricTimeseriesEnergyPerPeriodResponse).result;

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

        if ('_sum/ProductionActiveEnergy' in result.data && '_sum/EssDcChargeEnergy' in result.data && '_sum/GridSellActiveEnergy' in result.data) {
          let directConsumption = [];
          result.data['_sum/ProductionActiveEnergy'].forEach((value, index) => {
            directConsumption.push(value - result.data['_sum/GridSellActiveEnergy'][index] - result.data['_sum/EssDcChargeEnergy'][index]);
          });
          directConsumptionData = directConsumption.map(value => {
            if (value == null) {
              return null
            } else if (value < 0) {
              return 0
            } else {
              return value / 1000; // convert to kWh
            }
          });
        }

        // style for stacked + grouped bar chart
        let barWidthPercentage = 0;
        let categoryGapPercentage = 0;

        switch (this.service.periodString) {
          case DefaultTypes.PeriodString.CUSTOM: {
            barWidthPercentage = 0.7;
            categoryGapPercentage = 0.4;
          }
          case DefaultTypes.PeriodString.WEEK: {
            barWidthPercentage = 0.7;
            categoryGapPercentage = 0.4;
          }
          case DefaultTypes.PeriodString.MONTH: {
            if (this.service.isSmartphoneResolution == true) {
              barWidthPercentage = 1;
              categoryGapPercentage = 0.6;
            } else {
              barWidthPercentage = 0.9;
              categoryGapPercentage = 0.8;
            }
          }
          case "year": {
            if (this.service.isSmartphoneResolution == true) {
              barWidthPercentage = 1;
              categoryGapPercentage = 0.6;
            } else {
              barWidthPercentage = 0.8;
              categoryGapPercentage = 0.8;
            }
          }
        }

        // Production
        if ('_sum/ProductionActiveEnergy' in result.data) {
          let productionData = result.data['_sum/ProductionActiveEnergy'].map(value => {
            if (value == null) {
              return null
            } else {
              return value / 1000; // convert to kW
            }
          });
          datasets.push({
            label: chartLabels.production,
            data: productionData,
            hidden: !isLabelVisible(chartLabels.production, false),
            backgroundColor: 'rgba(45,143,171,0.25)',
            borderColor: 'rgba(45,143,171,1)',
            hoverBackgroundColor: 'rgba(45,143,171,0.5)',
            hoverBorderColor: 'rgba(45,143,171,1)',
            barPercentage: barWidthPercentage,
            categoryPercentage: categoryGapPercentage,
            stack: "PRODUCTION"
          });
        }

        // left stack

        /*
         * Direct Consumption
         */
        if (directConsumptionData != null) {
          datasets.push({
            label: chartLabels.directConsumption,
            data: directConsumptionData,
            hidden: !isLabelVisible(chartLabels.directConsumption),
            backgroundColor: 'rgba(244,164,96,0.25)',
            borderColor: 'rgba(244,164,96,1)',
            hoverBackgroundColor: 'rgba(244,164,96,0.5)',
            hoverBorderColor: 'rgba(244,164,96,1)',
            barPercentage: barWidthPercentage,
            categoryPercentage: categoryGapPercentage,
            stack: "0"
          })
        }

        /*
         * Storage Charge
         */
        if ('_sum/EssDcChargeEnergy' in result.data) {
          let chargeData = result.data['_sum/EssDcChargeEnergy'].map(value => {
            if (value == null) {
              return null
            } else {
              return value / 1000; // convert to kWh
            }
          });
          datasets.push({
            label: chartLabels.charge,
            data: chargeData,
            hidden: !isLabelVisible(chartLabels.charge),
            backgroundColor: 'rgba(0,223,0,0.25)',
            borderColor: 'rgba(0,223,0,1)',
            hoverBackgroundColor: 'rgba(0,223,0,0.5)',
            hoverBorderColor: 'rgba(0,223,0,1)',
            barPercentage: barWidthPercentage,
            categoryPercentage: categoryGapPercentage,
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
            label: chartLabels.gridSell,
            data: gridSellData,
            hidden: !isLabelVisible(chartLabels.gridSell),
            backgroundColor: 'rgba(0,0,200,0.25)',
            borderColor: 'rgba(0,0,200,1)',
            hoverBackgroundColor: 'rgba(0,0,200,0.5)',
            hoverBorderColor: 'rgba(0,0,200,1)',
            barPercentage: barWidthPercentage,
            categoryPercentage: categoryGapPercentage,
            stack: "0"
          })
        }

        // right stack

        /*
         * Direct Consumption
         */
        if (directConsumptionData != null) {
          datasets.push({
            label: chartLabels.directConsumption,
            data: directConsumptionData,
            hidden: !isLabelVisible(chartLabels.directConsumption),
            backgroundColor: 'rgba(244,164,96,0.25)',
            borderColor: 'rgba(244,164,96,1)',
            hoverBackgroundColor: 'rgba(244,164,96,0.5)',
            hoverBorderColor: 'rgba(244,164,96,1)',
            barPercentage: barWidthPercentage,
            categoryPercentage: categoryGapPercentage,
            stack: "1"
          })
        }

        /*
         * Storage Discharge
         */
        if ('_sum/EssDcDischargeEnergy' in result.data) {
          let dischargeData = result.data['_sum/EssDcDischargeEnergy'].map(value => {
            if (value == null) {
              return null
            } else {
              return value / 1000; // convert to kW
            }
          });
          datasets.push({
            label: chartLabels.discharge,
            data: dischargeData,
            hidden: !isLabelVisible(chartLabels.discharge),
            backgroundColor: 'rgba(200,0,0,0.25)',
            borderColor: 'rgba(200,0,0,1)',
            hoverBackgroundColor: 'rgba(200,0,0,0.5)',
            hoverBorderColor: 'rgba(200,0,0,1)',
            barPercentage: barWidthPercentage,
            categoryPercentage: categoryGapPercentage,
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
            label: chartLabels.gridBuy,
            data: gridBuyData,
            hidden: !isLabelVisible(chartLabels.gridBuy),
            backgroundColor: 'rgba(0,0,0,0.25)',
            borderColor: 'rgba(0,0,0,1)',
            hoverBackgroundColor: 'rgba(0,0,0,0.5)',
            hoverBorderColor: 'rgba(0,0,0,1)',
            barPercentage: barWidthPercentage,
            categoryPercentage: categoryGapPercentage,
            stack: "1"
          })
        }

        // Consumption
        if ('_sum/ConsumptionActiveEnergy' in result.data) {
          let consumptionData = result.data['_sum/ConsumptionActiveEnergy'].map(value => {
            if (value == null) {
              return null
            } else {
              return value / 1000; // convert to kW
            }
          });
          datasets.push({
            label: chartLabels.consumption,
            data: consumptionData,
            hidden: !isLabelVisible(chartLabels.consumption, false),
            backgroundColor: 'rgba(253,197,7,0.25)',
            borderColor: 'rgba(253,197,7,1)',
            hoverBackgroundColor: 'rgba(253,197,7,0.5)',
            hoverBorderColor: 'rgba(253,197,7,1)',
            barPercentage: barWidthPercentage,
            categoryPercentage: categoryGapPercentage,
            stack: "CONSUMPTION"
          });
        }
        this.setKwhLabel();
        this.datasets = datasets;
        this.colors = [];
        this.loading = false;
        this.stopSpinner();
      })
    })
  }

  private getEnergyChannelAddresses(config: EdgeConfig): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      let result: ChannelAddress[] = [];
      config.widgets.classes.forEach(clazz => {
        switch (clazz.toString()) {
          case 'Consumption':
            result.push(new ChannelAddress('_sum', 'ConsumptionActiveEnergy'));
          case 'Grid':
            result.push(new ChannelAddress('_sum', 'GridBuyActiveEnergy'));
            result.push(new ChannelAddress('_sum', 'GridSellActiveEnergy'));
            break;
          case 'Storage':
            result.push(new ChannelAddress('_sum', 'EssDcChargeEnergy'))
            result.push(new ChannelAddress('_sum', 'EssDcDischargeEnergy'));
            break;
          case 'Common_Production':
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
          case 'Common_Production':
            result.push(
              new ChannelAddress('_sum', 'ProductionActivePower'),
              new ChannelAddress('_sum', 'ProductionDcActualPower'));
            break;
        };
        return false;
      });
      resolve(result);
    })
  }


  private generateLabels(): Promise<EnergyChartLabels> {
    return new Promise((resolve) => {
      // Set regular labels
      let labels: EnergyChartLabels = {
        production: this.translate.instant('General.production'),
        gridBuy: this.translate.instant('General.gridBuy'),
        gridSell: this.translate.instant('General.gridSell'),
        charge: this.translate.instant('General.chargePower'),
        discharge: this.translate.instant('General.dischargePower'),
        consumption: this.translate.instant('General.consumption'),
        directConsumption: this.translate.instant('General.directConsumption'),
        stateOfCharge: this.translate.instant('General.soc')
      }

      // Generate kWh labels
      this.getEnergyChannelAddresses(this.config).then(channelAddresses => {
        this.service.queryEnergy(this.period.from, this.period.to, channelAddresses).then(response => {
          let result = (response as QueryHistoricTimeseriesEnergyResponse).result;
          if ('_sum/ProductionActiveEnergy' in result.data && response.result.data["_sum/ProductionActiveEnergy"] != null) {
            let kwhProductionValue = response.result.data["_sum/ProductionActiveEnergy"];
            labels.production += " " + this.unitpipe.transform(kwhProductionValue, "kWh").toString();
          }
          if ('_sum/GridBuyActiveEnergy' in result.data && response.result.data["_sum/GridBuyActiveEnergy"] != null) {
            let kwhGridBuyValue = response.result.data["_sum/GridBuyActiveEnergy"];
            labels.gridBuy += " " + this.unitpipe.transform(kwhGridBuyValue, "kWh").toString();
          }
          if ('_sum/GridSellActiveEnergy' in result.data && response.result.data["_sum/GridSellActiveEnergy"] != null) {
            let kwhGridSellValue = response.result.data["_sum/GridSellActiveEnergy"];
            labels.gridSell += " " + this.unitpipe.transform(kwhGridSellValue, "kWh").toString();
          }
          if ('_sum/EssDcChargeEnergy' in result.data && response.result.data["_sum/EssDcChargeEnergy"] != null) {
            let kwhChargeValue = response.result.data["_sum/EssDcChargeEnergy"];
            labels.charge += " " + this.unitpipe.transform(kwhChargeValue, "kWh").toString();
          }
          if ('_sum/EssDcDischargeEnergy' in result.data && response.result.data["_sum/EssDcDischargeEnergy"] != null) {
            let kwhDischargeValue = response.result.data["_sum/EssDcDischargeEnergy"];
            labels.discharge += " " + this.unitpipe.transform(kwhDischargeValue, "kWh").toString();
          }
          if ('_sum/ConsumptionActiveEnergy' in result.data && response.result.data["_sum/ConsumptionActiveEnergy"] != null) {
            let kwhConsumptionValue = response.result.data["_sum/ConsumptionActiveEnergy"];
            labels.consumption += " " + this.unitpipe.transform(kwhConsumptionValue, "kWh").toString();
          }
          if ('_sum/ProductionActiveEnergy' in result.data && '_sum/EssDcChargeEnergy' in result.data && '_sum/GridSellActiveEnergy' in result.data
            && response.result.data["_sum/ProductionActiveEnergy"] != null && response.result.data["_sum/EssDcChargeEnergy"] != null
            && response.result.data["_sum/GridSellActiveEnergy"]) {
            let kwhProductionValue = response.result.data["_sum/ProductionActiveEnergy"]
            let kwhChargeValue = response.result.data["_sum/EssDcChargeEnergy"];
            let kwhGridSellValue = response.result.data["_sum/GridSellActiveEnergy"];
            let directConsumptionValue = kwhProductionValue - kwhGridSellValue - kwhChargeValue;
            labels.directConsumption += " " + this.unitpipe.transform(directConsumptionValue, "kWh").toString();
          }
          resolve(labels)
        }).catch((error) => {
          resolve(error)
        })
      })
    })
  }

  private setKwhLabel() {
    let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    // general
    options.responsive = true;
    options.layout = {
      padding: {
        left: 2,
        right: 0,
        top: 0,
        bottom: 0
      }
    }

    // X-Axis for Chart: Calculate Time-Unit for normal sized window
    options.scales.xAxes[0].time.unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;

    options.scales.xAxes[0].bounds = 'ticks';
    options.scales.xAxes[0].stacked = true;
    options.scales.xAxes[0].offset = true;
    options.scales.xAxes[0].ticks.source = 'data';

    // Y-Axis for Labels
    options.scales.yAxes[0].scaleLabel.labelString = "kWh";
    options.scales.yAxes[0].scaleLabel.padding = -2;
    options.scales.yAxes[0].scaleLabel.fontSize = 11;

    //  +--------------------------------------------------------------------------------------------------+
    //  | EnergyChartLabels with kWh                                                                       |
    //  +--------------------------------------------------------------------------------------------------|
    //  | Production | GridSell | ChargePower | DirectConsumption | GridBuy | DischargePower | Consumption |
    //  +--------------------------------------------------------------------------------------------------+
    //      
    // this.translate is not available in legend methods
    let directConsumptionLabelText = this.translate.instant('General.directConsumption');
    let productionLabelText = this.translate.instant('General.production');
    let consumptionLabelText = this.translate.instant('General.consumption');
    let gridBuyLabelText = this.translate.instant('General.gridBuy');
    let gridSellLabelText = this.translate.instant('General.gridSell');
    let chargeLabelText = this.translate.instant('General.chargePower');
    let dischargeLabelText = this.translate.instant('General.dischargePower');

    options.legend.labels.generateLabels = function (chart: Chart) {
      let chartLegendLabelItems: ChartLegendLabelItem[] = [];
      let chartLegendLabelItemsOrder = [
        productionLabelText,
        gridSellLabelText,
        chargeLabelText,
        directConsumptionLabelText,
        consumptionLabelText,
        gridBuyLabelText,
        dischargeLabelText
      ]

      // set correct value (label + total kWh) for reorder
      chart.data.datasets.forEach((dataset, datasetIndex) => {

        if (dataset.label.includes(productionLabelText)) {
          chartLegendLabelItemsOrder[0] = dataset.label;
        }
        if (dataset.label.includes(gridSellLabelText)) {
          chartLegendLabelItemsOrder[1] = dataset.label;
        }
        if (dataset.label.includes(chargeLabelText)) {
          chartLegendLabelItemsOrder[2] = dataset.label;
        }
        if (dataset.label.includes(directConsumptionLabelText)) {
          chartLegendLabelItemsOrder[3] = dataset.label;
        }
        if (dataset.label.includes(gridBuyLabelText)) {
          chartLegendLabelItemsOrder[4] = dataset.label;
        }
        if (dataset.label.includes(dischargeLabelText)) {
          chartLegendLabelItemsOrder[5] = dataset.label;
        }
        if (dataset.label.includes(consumptionLabelText)) {
          chartLegendLabelItemsOrder[6] = dataset.label;
        }

        let text = dataset.label;
        let index = datasetIndex;
        let fillStyle = dataset.backgroundColor.toString();

        let lineWidth = 2;
        let strokeStyle = dataset.borderColor.toString();

        if (text.includes(directConsumptionLabelText) && dataset.stack == "1") {

          //skip ChartLegendLabelItem
        } else {
          chartLegendLabelItems.push({
            text: text,
            datasetIndex: index,
            fillStyle: fillStyle,
            // Consumption and Production should always be shown as visible, without line-through style
            hidden: ((text.includes(consumptionLabelText) || text.includes(productionLabelText)) ? false : !chart.isDatasetVisible(datasetIndex)),
            lineWidth: lineWidth,
            strokeStyle: strokeStyle,
          })
        }
      })
      chartLegendLabelItems.sort(function (a, b) {
        return chartLegendLabelItemsOrder.indexOf(a.text) - chartLegendLabelItemsOrder.indexOf(b.text);
      });
      return chartLegendLabelItems;
    }

    // used to hide both DirectConsumption-legend-Items by clicking one of them
    options.legend.onClick = function (event: MouseEvent, legendItem: ChartLegendLabelItem) {

      let chart: Chart = this.chart;
      let legendItemIndex = legendItem.datasetIndex;
      let datasets = chart.data.datasets;

      // Set Angulars SessionStorage for Labels to check if they are hidden
      setLabelVisible(legendItem.text, !chart.isDatasetVisible(legendItemIndex));

      let firstDirectConsumptionStackDatasetIndex: null | number = null;
      let secondDirectConsumptionStackDatasetIndex: null | number = null;

      chart.data.datasets.forEach((value, index) => {

        // seperated stack 0 and 1
        if (datasets[index].label.includes(directConsumptionLabelText) && datasets[index].stack == "0") {
          firstDirectConsumptionStackDatasetIndex = index;
        }
        if (datasets[index].label.includes(directConsumptionLabelText) && datasets[index].stack == "1") {
          secondDirectConsumptionStackDatasetIndex = index;
        }
      })
      datasets.forEach((value, datasetIndex) => {

        let meta = chart.getDatasetMeta(datasetIndex);
        let directConsumptionMetaArr = [
          chart.getDatasetMeta(firstDirectConsumptionStackDatasetIndex),
          chart.getDatasetMeta(secondDirectConsumptionStackDatasetIndex)
        ]

        if (legendItemIndex == datasetIndex &&
          (datasetIndex == firstDirectConsumptionStackDatasetIndex || datasetIndex == secondDirectConsumptionStackDatasetIndex)) {

          // hide/show both directConsumption bars
          directConsumptionMetaArr.forEach(meta => {
            meta.hidden = meta.hidden === null ?
              !datasets[firstDirectConsumptionStackDatasetIndex].hidden && !datasets[secondDirectConsumptionStackDatasetIndex].hidden : null;
          })
        } else if (legendItemIndex == datasetIndex) {
          meta.hidden = meta.hidden === null ? !datasets[datasetIndex].hidden : null;
        }
      })
      chart.update();
    }
    //  
    // Tooltips
    // +-----------------------------------------+
    // |Header |1    Date (Month | Day | Hour)   |
    // |       |                                 |
    // |       |2    Production / Consumption    |
    // |       |                                 |
    // +-------+---------------------------------|
    // |Content|3    EnergyLabels[]              |
    // +-----------------------------------------+

    // 3: EnergyLabels[]
    options.tooltips.mode = 'x';
    options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
      let value = tooltipItem.value;
      let label = data.datasets[tooltipItem.datasetIndex].label;
      if (isNaN(value) == false) {
        if (label.split(" ").length > 1) {
          label = label.split(" ").slice(0, 1).toString();
        }
        return label + ": " + formatNumber(value, 'de', '1.0-2') + " kWh";
      } else {
        return null;
      }
    }

    options.tooltips.itemSort = function (a: ChartTooltipItem, b: ChartTooltipItem) {
      return b.datasetIndex - a.datasetIndex;
    }

    // 1: Date
    options.tooltips.callbacks.title = (tooltipItems: TooltipItem[], data: Data): string => {
      let date = new Date(tooltipItems[0].xLabel);
      return this.toTooltipTitle(this.period.from, this.period.to, date);
    }

    // 2: Production / Consumption
    options.tooltips.callbacks.afterTitle = function (item: ChartTooltipItem[], data: ChartData) {
      if (item.length == 3) {
        let totalValue = item.reduce((a, e) => a + parseFloat(<string>e.yLabel), 0);
        let isProduction: boolean | null = null;
        item.forEach(item => {
          if (item.datasetIndex == 0 || item.datasetIndex == 1 || item.datasetIndex == 2) {
            isProduction = true;
          } else if (item.datasetIndex == 3 || item.datasetIndex == 4 || item.datasetIndex == 5) {
            isProduction = false;
          }
        })
        if (isNaN(totalValue) == false) {
          return isProduction == true
            ? productionLabelText + ' ' + formatNumber(totalValue, 'de', '1.0-2') + " kWh"
            : consumptionLabelText + ' ' + formatNumber(totalValue, 'de', '1.0-2') + " kWh";
        }
      } else {
        return null
      }
    }
    this.options = options;
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
    })
    options.layout = {
      padding: {
        left: 2,
        right: 2,
        top: 0,
        bottom: 0
      }
    }

    //x-axis
    options.scales.xAxes[0].time.unit = calculateResolution(this.service, this.period.from, this.period.to).timeFormat;

    //y-axis
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
      if (label == translate.instant('General.soc').split(" ")[0]) {
        return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
      } else {
        return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
      }
    }
    this.options = options;
  }


  public getChartHeight(): number {
    return this.service.deviceHeight / 2;
  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: EnergyModalComponent,
    });
    return await modal.present();
  }
}
