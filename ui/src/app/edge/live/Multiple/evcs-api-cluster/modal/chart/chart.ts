import { ChangeDetectorRef, Component, Inject, Input, ViewChild, effect } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { ChartOptions } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { ChartBaseModule } from "src/app/shared/chart-base.module";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants, XAxisType } from "src/app/shared/components/chart/chart.constants";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { LiveDataServiceProvider } from "src/app/shared/provider/live-data-service-provider";
import { ChannelAddress, EdgeConfig, Logger, Service } from "src/app/shared/shared";
import { TSignalValue } from "src/app/shared/type/utility";
import { ObjectUtils } from "src/app/shared/utils/object/object.utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";


@Component({
  selector: "oe-multiple-evcs-api-cluster-chart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
  imports: [
    LiveDataServiceProvider,
    ChartBaseModule,
    CommonUiModule,
  ],
  providers: [
    {
      provide: DataService,
      useClass: LiveDataService,
    },
  ],
})
export class ChartComponent extends AbstractHistoryChart {

  @Input() public evcss: { [evcsId: string]: EdgeConfig.Component } = {};

  @ViewChild(BaseChartDirective) public chart!: BaseChartDirective;

  private data: number[] = [];
  constructor(
    public override service: Service,
    public override cdRef: ChangeDetectorRef,
    protected override translate: TranslateService,
    protected override route: ActivatedRoute,
    protected override logger: Logger,
    @Inject(DataService) private dataService: DataService,
  ) {
    super(service, cdRef, translate, route, logger);

    let previousMax: number | null = null;

    effect(() => {
      const currentValue = dataService.currentValue();
      const _datasets: typeof this.datasets = this.datasets.map(dataset => ({
        ...dataset,
        data: this.getData(currentValue, this.evcss), // Example transformation
      }));

      previousMax = this.updateYAxisScaling(_datasets, previousMax, translate);
    });
  }

  public static getChartData(component: EdgeConfig.Component, data: number[]): HistoryUtils.ChartData {
    return {
      input: [],
      output: (_data: HistoryUtils.ChannelData) => {
        return [<HistoryUtils.DisplayValue<HistoryUtils.DataLabelsCustomOptions>>{
          name: Name.METER_ALIAS_OR_ID(component),
          converter: () => data,
          color: ChartConstants.Colors.GREEN,
          custom: {
            pluginType: "datalabels",
            datalabels: {
              displayUnit: "W",
            },
          },
        }];
      },
      tooltip: {
        formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
        enabled: false,
      },
      yAxes: [{
        unit: YAxisType.POWER,
        position: "left",
        yAxisId: ChartAxis.LEFT,
        customTitle: "W",
      }],
    };
  }

  protected override getChartData(): HistoryUtils.ChartData | null {
    this.chartType = "bar";
    this.xAxisScalingType = XAxisType.NUMBER;

    if (this.component == null || this.dataService == null || this.edge == null) {
      return this.chartObject;
    }

    this.dataService?.getValues(Object.entries(this.evcss).map(([k, v]) => new ChannelAddress(k, "ChargePower")), this.edge);
    return ChartComponent.getChartData(this.component, this.data);
  }

  protected getChannelData(currentData: CurrentValue, evcss: typeof this.evcss): typeof this.channelData {
    return {
      data: Object.entries(evcss).reduce((obj, [k, v]) => {
        obj[k + "/ChargePower"] = this.getData(currentData, evcss);
        return obj;
      }, {} as { [index: string]: number[] }),
    };
  }

  protected getData(currentData: CurrentValue, evcss: typeof this.evcss): number[] {
    return Object.entries(evcss).map(([k, v]) => currentData.allComponents[k + "/ChargePower"]);
  }

  protected getLabels() {
    if (this.evcss == null || this.component == null) {
      return [];
    }

    return Object.entries(this.evcss)
      .map(([_key, component]) => {
        console.log("component", component, this.evcss);
        return Name.METER_ALIAS_OR_ID(component);
      });
  }

  protected override loadChart(): Promise<void> {
    return new Promise((res) => {
      this.chartType = "bar";
      this.chartObject = this.getChartData();
      this.labels = this.getLabels();

      if (this.chartObject == null || this.dataService == null) {
        res();
        return;
      }
      this.channelData = this.getChannelData(this.dataService.currentValue(), this.evcss);

      const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, new QueryHistoricTimeseriesEnergyPerPeriodResponse("", {
        data: this.channelData.data,
        timestamps: this.labels.map(el => el.toString()),
      }));

      this.datasets = displayValues.datasets;
      this.legendOptions = displayValues.legendOptions;

      this.setChartLabel();
      res();
    });
  }

  protected override getChartHeight(): number {
    return window.innerHeight / 2;
  }

  private updateYAxisScaling(_datasets: typeof this.datasets, previousMax: number | null, translate: TranslateService) {

    if (this == null || this.chartObject == null || this.options == null || this.options.scales == null) {
      return null;
    }

    const optionScales = this.options.scales;
    const chartObject = this.chartObject;

    if (this.chart?.chart) {
      this.chart.chart.data.datasets = _datasets;
      chartObject.yAxes.filter(el => el satisfies HistoryUtils.yAxes).forEach((element) => {
        const scaleOptions: ReturnType<typeof ChartConstants.getScaleOptions> = ChartConstants.getScaleOptions(_datasets, element, this.chartType);
        if (scaleOptions == null || optionScales == null) {
          return null;
        }

        if (previousMax === null || scaleOptions.max > previousMax) {
          optionScales[ChartAxis.LEFT] = { ...optionScales[ChartAxis.LEFT], ...scaleOptions };
          this.options = AbstractHistoryChart.getYAxisOptions(this.options as ChartOptions, element, translate, this.chartType, _datasets, true, chartObject.tooltip.formatNumber);
        }
        previousMax = ObjectUtils.getKeySafely(optionScales, ChartAxis.LEFT)?.max as number;
      });
      this.chart.chart.update("none"); // Update without animation
    }
    return previousMax;
  }
}

type CurrentValue = TSignalValue<DataService["currentValue"]>;
