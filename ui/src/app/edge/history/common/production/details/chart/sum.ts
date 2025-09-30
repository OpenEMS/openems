// @ts-strict-ignore
import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { Phase } from "src/app/shared/components/shared/phase";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "sumChart",
  templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class SumChartDetailsComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HISTORY_UTILS.CHART_DATA {

    const component = CONFIG.GET_COMPONENT(ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
    const hasCharger = CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER").length > 0;
    const hasAsymmetricMeters = CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ASYMMETRIC_METER").length > 0;

    const input: HISTORY_UTILS.INPUT_CHANNEL[] = [
      {
        name: COMPONENT.ID,
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ProductionActivePower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ProductionActiveEnergy"),
      },
    ];
    let converter: ((data: HISTORY_UTILS.CHANNEL_DATA, phase: string) => any) | null = null;

    if (hasCharger) {
      INPUT.PUSH({
        name: COMPONENT.ID + "ActualPower",
        powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/ProductionDcActualPower"),
      });

      converter = (data: HISTORY_UTILS.CHANNEL_DATA, phase: string) => data[COMPONENT.ID + "ActualPower"]?.reduce((arr, el, index) => {
        ARR.PUSH(UTILS.ADD_SAFELY(UTILS.DIVIDE_SAFELY(el, 3), data["ProductionAcActivePower" + phase][index]));
        return arr;
      }, []);
    }

    if (hasAsymmetricMeters) {
      converter = (data, phase) => data["ProductionAcActivePower" + phase];
    }

    if (hasAsymmetricMeters || hasCharger) {
      INPUT.PUSH(...Phase.THREE_PHASE.map(phase => ({
        name: "ProductionAcActivePower" + phase,
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ProductionAcActivePower" + phase),
      })));
    }

    const phaseOutput: (data: HISTORY_UTILS.CHANNEL_DATA) => HISTORY_UTILS.DISPLAY_VALUE[] =
      converter ? (data) => Phase.THREE_PHASE.map((phase, i) => ({
        name: "Phase " + phase,
        converter: () => converter(data, phase),
        color: "rgb(" + ABSTRACT_HISTORY_CHART.PHASE_COLORS[i] + ")",
        stack: 3,
      })) : () => [];

    const chartObject: HISTORY_UTILS.CHART_DATA = {
      input: input,
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => [
        {
          name: TRANSLATE.INSTANT("GENERAL.TOTAL"),
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => ENERGY_QUERY_RESPONSE.RESULT.DATA["_sum/ProductionActiveEnergy"],
          converter: () => data[COMPONENT.ID],
          color: CHART_CONSTANTS.COLORS.BLUE,
          hiddenOnInit: false,
          stack: 2,
        },
        ...phaseOutput(data),
      ],
      tooltip: {
        formatNumber: "1.1-2",
        afterTitle: TRANSLATE.INSTANT("GENERAL.TOTAL"),
      },
      yAxes: [{
        unit: YAXIS_TYPE.ENERGY,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      }],
    };

    return chartObject;
  }

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {
    return SUM_CHART_DETAILS_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.ROUTE, THIS.TRANSLATE);
  }
}
