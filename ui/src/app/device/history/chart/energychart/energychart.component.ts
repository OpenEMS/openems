import { Component, Input, OnInit, OnChanges, ViewChild, AfterViewInit, SimpleChanges } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BaseChartDirective } from 'ng2-charts/ng2-charts';
import { TranslateService } from '@ngx-translate/core';

import { Device } from '../../../../shared/device/device';
import { ConfigImpl } from '../../../../shared/device/config';
import { DefaultTypes } from '../../../../shared/service/defaulttypes';
import { Dataset, EMPTY_DATASET } from './../../../../shared/shared';
import { DEFAULT_TIME_CHART_OPTIONS, ChartOptions, TooltipItem, Data } from './../shared';
import { Utils } from './../../../../shared/service/utils';
import { CurrentDataAndSummary } from '../../../../shared/device/currentdata';

// TODO grid should be shown as "Netzeinspeisung"/"Netzbezug" instead of positive/negative value
@Component({
  selector: 'energychart',
  templateUrl: './energychart.component.html'
})
export class EnergyChartComponent implements OnChanges {

  @Input() private device: Device;
  @Input() private config: ConfigImpl;
  @Input() private channels: DefaultTypes.ChannelAddresses;
  @Input() private fromDate: Date;
  @Input() private toDate: Date;

  @ViewChild('energyChart') private chart: BaseChartDirective;

  constructor(
    private utils: Utils,
    private translate: TranslateService
  ) {
    this.grid = this.translate.instant('General.Grid');
    this.gridBuy = this.translate.instant('General.GridBuy');
    this.gridSell = this.translate.instant('General.GridSell');
  }

  public labels: Date[] = [];
  public datasets: Dataset[] = EMPTY_DATASET;
  public loading: boolean = true;

  private ngUnsubscribe: Subject<void> = new Subject<void>();
  private grid: String = "";
  private gridBuy: String = "";
  private gridSell: String = "";

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
  private options: ChartOptions;

  ngOnInit() {
    let options = <ChartOptions>this.utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
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
      return label + ": " + value.toPrecision(2) + " kW";
    }
    this.options = options;
  }

  ngOnChanges() {
    if (Object.keys(this.channels).length === 0) {
      this.loading = true;
      return;
    }
    this.loading = true;
    this.device.historicDataQuery(this.fromDate, this.toDate, this.channels).then(historicData => {
      // prepare datas array and prefill with each device

      // prepare datasets and labels
      let activePowers = {
        production: [],
        gridBuy: [],
        gridSell: [],
        consumption: [],
        storageCharge: [],
        storageDischarge: []
      }
      let labels: Date[] = [];
      for (let record of historicData.data) {
        labels.push(new Date(record.time));
        let data = new CurrentDataAndSummary(record.channels, this.config);
        activePowers.gridBuy.push(Utils.divideSafely(data.summary.grid.buyActivePower, 1000)); // convert to kW
        activePowers.gridSell.push(Utils.divideSafely(data.summary.grid.sellActivePower, 1000)); // convert to kW
        activePowers.production.push(Utils.divideSafely(data.summary.production.activePower, 1000)); // convert to kW
        activePowers.consumption.push(Utils.divideSafely(data.summary.consumption.activePower, 1000)); // convert to kW
        activePowers.storageCharge.push(Utils.divideSafely(Utils.addSafely(data.summary.storage.chargeActivePower, data.summary.production.activePowerDC), 1000)); // convert to kW
        activePowers.storageDischarge.push(Utils.divideSafely(data.summary.storage.dischargeActivePowerAC, 1000)); // convert to kW
      }
      this.datasets = [{
        label: this.translate.instant('General.Production'),
        data: activePowers.production,
        hidden: false
      }, {
        label: this.translate.instant('General.GridBuy'),
        data: activePowers.gridBuy,
        hidden: false
      }, {
        label: this.translate.instant('General.GridSell'),
        data: activePowers.gridSell,
        hidden: false
      }, {
        label: this.translate.instant('General.Consumption'),
        data: activePowers.consumption,
        hidden: false
      }, {
        label: this.translate.instant('General.ChargePower'),
        data: activePowers.storageCharge,
        hidden: true
      }, {
        label: this.translate.instant('General.DischargePower'),
        data: activePowers.storageDischarge,
        hidden: true
      }];
      this.labels = labels;
      // stop loading spinner
      this.loading = false;
      setTimeout(() => {
        // Workaround, because otherwise chart data and labels are not refreshed...
        if (this.chart) {
          this.chart.ngOnChanges({} as SimpleChanges);
        }
      });
    }).catch(error => {
      this.datasets = EMPTY_DATASET;
      this.labels = [];
    });
  }
}