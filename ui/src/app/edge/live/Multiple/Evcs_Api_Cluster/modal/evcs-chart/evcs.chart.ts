import { ChartDataSets } from 'chart.js';
import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { Data } from 'src/app/edge/history/shared';
import { EdgeConfig, Edge } from 'src/app/shared/shared';
import { Label } from 'ng2-charts';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import * as Chart from 'chart.js';

@Component({
  selector: EvcsChartComponent.SELECTOR,
  templateUrl: './evcs.chart.html'
})
export class EvcsChartComponent implements OnInit, OnChanges {

  @Input() evcsMap: { [sourceId: string]: EdgeConfig.Component };
  @Input() edge: Edge;
  @Input() currentData: CurrentData;
  @Input() evcsConfigMap: { [evcsId: string]: EdgeConfig.Component } = {};
  @Input() componentId: string;

  private static readonly SELECTOR = "evcsChart";
  public loading: boolean = true;
  public options: BarChartOptions;
  public labels: Label[];
  public datasets: ChartDataSets[];
  chart: Chart; // This will hold our chart info


  constructor(
    protected translate: TranslateService,
    public modalController: ModalController
  ) { }

  ngOnInit() {
    this.options = DEFAULT_BAR_CHART_OPTIONS;

    this.options.scales.yAxes[0].ticks.max = this.getMaxPower();
    this.labels = ['Ladeleistung'];
    this.datasets = [
      { data: [], label: '' }
    ];

  }

  ngOnChanges(changes: import("@angular/core").SimpleChanges): void {

    this.updateChart();
  }

  private updateChart() {
    if (this.datasets == null) {
      this.loading = true;
      return;
    }
    this.loading = true;
    let index = 0;
    for (let evcsId in this.evcsMap) {
      let chargePower = this.edge.currentData.value.channel[evcsId + '/ChargePower'];
      let chargePowerKW = chargePower / 1000.0
      let alias = this.evcsConfigMap[evcsId].properties.alias;
      if (this.datasets[index] == null) {
        this.datasets.push({
          label: alias,
          data: [chargePowerKW != null ? chargePowerKW : 0]
        });
      } else if (alias == "") { //this.datasets[index].label
        this.datasets[index].label = evcsId;
      } else {
        this.datasets[index].label = alias;
        this.datasets[index].data = [chargePowerKW != null ? chargePowerKW : 0];
      }
      index++;
    };
    this.loading = false;
  }

  getMaxPower() {
    let maxPower: number;
    let minPower = 22;
    let maxHW = this.currentData[this.componentId + '/MaximumHardwarePower'];
    let chargePower = this.currentData[this.componentId + '/ChargePower'];
    maxHW = maxHW == null ? minPower : maxHW / 1000;
    chargePower = chargePower == null ? 0 : chargePower / 1000;

    maxPower = chargePower < minPower || maxPower < minPower ? minPower : maxHW;
    return Math.round(maxPower);
  }
}

export const DEFAULT_BAR_CHART_OPTIONS: BarChartOptions = {
  maintainAspectRatio: false,
  legend: {
    position: 'bottom'
  },
  elements: {
    point: {
      radius: 0,
      hitRadius: 0,
      hoverRadius: 0
    },
    line: {
      borderWidth: 2,
      tension: 0.1
    },
  },
  hover: {
    mode: 'point',
    intersect: true
  },
  scales: {
    xAxes: [{
      stacked: true
    }],
    yAxes: [{
      scaleLabel: {
        display: true,
        labelString: ""
      },
      ticks: {
        beginAtZero: true,
        max: 50
      },
      stacked: true
    }],
  },
  tooltips: {
    mode: 'index',
    intersect: false,
    axis: 'x',
    title: "Ladeleistung",
    callbacks: {
      label(tooltipItems: BarChartTooltipItem, data: Data): string {
        let value: number = tooltipItems.yLabel; //.toFixed(2);
        value = parseFloat(value.toFixed(2));
        let label = data.datasets[tooltipItems.datasetIndex].label;
        return label + ": " + value.toLocaleString('de-DE') + " kW";
      }
    }
  },
  annotation: {
    annotations: [{
      type: 'line',
      mode: 'horizontal',
      yScaleID: 'y-axis-0',
      value: 33,
      borderColor: 'green',
      borderWidth: 4,
      label: {
        enabled: true,
        content: 'Test label'
      }
    }]
  }
};

export type BarChartOptions = {
  maintainAspectRatio: boolean,
  legend: {
    position: "bottom"
  },
  elements: {
    point: {
      radius: number,
      hitRadius: number,
      hoverRadius: number
    },
    line: {
      borderWidth: number,
      tension: number
    }
  },
  hover: {
    mode: string,
    intersect: boolean
  },
  scales: {
    yAxes: [{
      scaleLabel: {
        display: boolean,
        labelString: string
      },
      ticks: {
        beginAtZero: boolean,
        max?: number
      },
      stacked: boolean
    }],
    xAxes: [{
      stacked: boolean
    }]
  },
  tooltips: {
    mode: string,
    intersect: boolean,
    axis: string,
    title?: string
    callbacks: {
      label?(tooltipItem: BarChartTooltipItem, data: Data): string,
    }
  },
  annotation: {
    annotations: [{
      type: string,
      mode: string,
      yScaleID: string,
      value: number,
      borderColor: string,
      borderWidth: number,
      label: {
        enabled: boolean,
        content: string
      }
    }]
  }
}

export type BarChartTooltipItem = {
  datasetIndex: number,
  index: number,
  y: number,
  yLabel: number
}
