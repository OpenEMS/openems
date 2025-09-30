// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { calculateResolution, ChronoUnit } from "src/app/edge/history/shared";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/CHART.MODULE";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-ERROR.MODULE";
import { ChannelAddress, ChartConstants, EdgeConfig, Utils } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "oe-controller-peakshaving-timeslot-chart",
  templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: true,
  imports: [
    BaseChartDirective,
    ReactiveFormsModule,
    CommonModule,
    IonicModule,
    TranslateModule,
    ChartComponentsModule,
    HistoryDataErrorModule,
    NgxSpinnerModule,
  ],
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, component: EDGE_CONFIG.COMPONENT, translate: TranslateService): HISTORY_UTILS.CHART_DATA {

    ASSERTION_UTILS.ASSERT_IS_DEFINED(component);
    const meterId = CONFIG.GET_PROPERTY_FROM_COMPONENT<string>(component, "METER.ID");
    const input: HISTORY_UTILS.INPUT_CHANNEL[] = [{
      name: "ActivePower",
      powerChannel: new ChannelAddress(meterId, "ActivePower"),
      converter: HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO,
    },
    {
      name: "EssSoc",
      powerChannel: new ChannelAddress("_sum", "EssSoc"),
    },
    {
      name: "Charge",
      powerChannel: new ChannelAddress("_sum", "EssActivePower"),
      converter: HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE,
    },
    {
      name: "Discharge",
      powerChannel: new ChannelAddress("_sum", "EssActivePower"),
      converter: HISTORY_UTILS.VALUE_CONVERTER.NON_NULL_OR_NEGATIVE,

    },
    {
      name: "CHARGE_UNDER",
      powerChannel: new ChannelAddress(COMPONENT.ID, "_PropertyRechargePower"),
    },
    {
      name: "DISCHARGE_OVER",
      powerChannel: new ChannelAddress(COMPONENT.ID, "_PropertyPeakShavingPower"),
    },
    ];

    return {
      input: input,
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
        const output: HISTORY_UTILS.DISPLAY_VALUE[] = [
          {
            name: TRANSLATE.INSTANT("GENERAL.GRID_BUY_ADVANCED"),
            color: CHART_CONSTANTS.COLORS.BLUE_GREY,
            converter: () => data["ActivePower"],
          },
          {
            name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER"),
            color: CHART_CONSTANTS.COLORS.GREEN,
            converter: () => data["CHARGE_UNDER"],
            hideShadow: true,
            borderDash: [3, 3],
          },
          {
            name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER"),
            color: CHART_CONSTANTS.COLORS.RED,
            converter: () => data["DISCHARGE_OVER"],
            hideShadow: true,
            borderDash: [3, 3],
          },
          {
            name: TRANSLATE.INSTANT("GENERAL.CHARGE"),
            color: CHART_CONSTANTS.COLORS.GREEN,
            converter: () => data["Charge"],
          },
          {
            name: TRANSLATE.INSTANT("GENERAL.DISCHARGE"),
            color: CHART_CONSTANTS.COLORS.RED,
            converter: () => data["Discharge"],
          },
          {
            name: TRANSLATE.INSTANT("GENERAL.SOC"),
            color: CHART_CONSTANTS.COLORS.GREY,
            converter: () => data["EssSoc"]?.map(value => UTILS.MULTIPLY_SAFELY(value, 1000)),
            yAxisId: CHART_AXIS.RIGHT,
            borderDash: [10, 10],
          },
        ];
        return output;
      },
      tooltip: {
        formatNumber: CHART_CONSTANTS.NUMBER_FORMAT.ZERO_TO_TWO,
      },
      yAxes: [{
        unit: YAXIS_TYPE.ENERGY,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      },

      {
        unit: YAXIS_TYPE.PERCENTAGE,
        position: "right",
        yAxisId: CHART_AXIS.RIGHT,
      },
      ],
    };
  }

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {
    return CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.COMPONENT, THIS.TRANSLATE);
  }

  protected override loadChart(): Promise<void> {
    const unit: CHRONO_UNIT.TYPE = calculateResolution(THIS.SERVICE, THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO).RESOLUTION.UNIT;
    THIS.LOAD_LINE_CHART(unit);
    return;
  }
}
