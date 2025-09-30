// @ts-strict-ignore
import { Component, Input, OnChanges, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "CHART.JS";
import { Data } from "src/app/edge/history/shared";
import { CurrentData } from "src/app/shared/components/edge/currentdata";
import { Edge, EdgeConfig } from "src/app/shared/shared";

@Component({
  selector: EVCS_CHART_COMPONENT.SELECTOR,
  templateUrl: "./EVCS.CHART.HTML",
  standalone: false,
})
export class EvcsChartComponent implements OnInit, OnChanges {

  private static readonly SELECTOR = "evcsChart";

  @Input({ required: true }) private evcsMap!: { [sourceId: string]: EDGE_CONFIG.COMPONENT };
  @Input({ required: true }) private edge!: Edge;
  @Input({ required: true }) private currentData!: CurrentData;
  @Input() private evcsConfigMap: { [evcsId: string]: EDGE_CONFIG.COMPONENT } = {};
  @Input({ required: true }) private componentId!: string;

  public loading: boolean = true;
  public options: BarChartOptions;
  public labels: string[];
  public datasets: CHART.CHART_DATASET[];
  public chart: CHART.CHART; // This will hold our chart info

  constructor(
    protected translate: TranslateService,
    public modalController: ModalController,
  ) { }

  getMaxPower() {
    const minPower = 22;
    let maxHW = THIS.CURRENT_DATA[THIS.COMPONENT_ID + "/MaximumHardwarePower"];
    let chargePower = THIS.CURRENT_DATA[THIS.COMPONENT_ID + "/ChargePower"];
    maxHW = maxHW == null ? minPower : maxHW / 1000;
    chargePower = chargePower == null ? 0 : chargePower / 1000;

    const maxPower: number = chargePower < minPower || maxHW;
    return MATH.ROUND(maxPower);
  }

  ngOnInit() {
    THIS.OPTIONS = DEFAULT_BAR_CHART_OPTIONS;

    THIS.OPTIONS.SCALES.Y_AXES[0].TICKS.MAX = THIS.GET_MAX_POWER();
    THIS.LABELS = ["Ladeleistung"];
    THIS.DATASETS = [
      { data: [], label: "" },
    ];

  }

  ngOnChanges(changes: import("@angular/core").SimpleChanges): void {

    THIS.UPDATE_CHART();
  }

  private updateChart() {
    if (THIS.DATASETS == null) {
      THIS.LOADING = true;
      return;
    }
    THIS.LOADING = true;
    let index = 0;
    for (const evcsId in THIS.EVCS_MAP) {
      const chargePower = THIS.EDGE.CURRENT_DATA.VALUE.CHANNEL[evcsId + "/ChargePower"];
      const chargePowerKW = chargePower / 1000.0;
      const alias = THIS.EVCS_CONFIG_MAP[evcsId].PROPERTIES.ALIAS;
      if (THIS.DATASETS[index] == null) {
        THIS.DATASETS.PUSH({
          label: alias,
          data: [chargePowerKW != null ? chargePowerKW : 0],
        });
      } else if (alias == "") { //THIS.DATASETS[index].label
        THIS.DATASETS[index].label = evcsId;
      } else {
        THIS.DATASETS[index].label = alias;
        THIS.DATASETS[index].data = [chargePowerKW != null ? chargePowerKW : 0];
      }
      index++;
    }
    THIS.LOADING = false;
  }

}

export const DEFAULT_BAR_CHART_OPTIONS: BarChartOptions = {
  maintainAspectRatio: false,
  legend: {
    position: "bottom",
  },
  elements: {
    point: {
      radius: 0,
      hitRadius: 0,
      hoverRadius: 0,
    },
    line: {
      borderWidth: 2,
      tension: 0.1,
    },
  },
  hover: {
    mode: "point",
    intersect: true,
  },
  scales: {
    xAxes: [{
      stacked: true,
    }],
    yAxes: [{
      scaleLabel: {
        display: true,
        labelString: "",
      },
      ticks: {
        beginAtZero: true,
        max: 50,
      },
      stacked: true,
    }],
  },
  tooltips: {
    mode: "index",
    intersect: false,
    axis: "x",
    title: "Ladeleistung",
    callbacks: {
      label(tooltipItems: BarChartTooltipItem, data: Data): string {
        let value: number = TOOLTIP_ITEMS.Y_LABEL; //.toFixed(2);
        value = parseFloat(VALUE.TO_FIXED(2));
        const label = DATA.DATASETS[TOOLTIP_ITEMS.DATASET_INDEX].label;
        return label + ": " + VALUE.TO_LOCALE_STRING("de-DE") + " kW";
      },
    },
  },
  annotation: {
    annotations: [{
      type: "line",
      mode: "horizontal",
      yScaleID: "y-axis-0",
      value: 33,
      borderColor: "green",
      borderWidth: 4,
      label: {
        enabled: true,
        content: "Test label",
      },
    }],
  },
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
};

export type BarChartTooltipItem = {
  datasetIndex: number,
  index: number,
  y: number,
  yLabel: number
};
