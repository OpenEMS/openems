import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Phase } from "src/app/shared/components/shared/phase";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, ChartConstants, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "consumptionMeterChart",
  templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class ConsumptionMeterChartDetailsComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HISTORY_UTILS.CHART_DATA {
    const component = config?.getComponent(ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
    return {
      input: [{
        name: COMPONENT.ID,
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActivePower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActiveProductionEnergy"),
      },
      ...Phase.THREE_PHASE.map(phase => ({
        name: "ConsumptionActivePower" + phase,
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActivePower" + phase),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActiveProductionEnergy" + phase),
      }))],
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => [
        {
          name: COMPONENT.ALIAS,
          nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => ENERGY_QUERY_RESPONSE.RESULT.DATA[COMPONENT.ID + "/ActiveProductionEnergy"],
          converter: () => data[COMPONENT.ID],
          color: CHART_CONSTANTS.COLORS.RED,
          hiddenOnInit: false,
          stack: 2,
        },

        ...Phase.THREE_PHASE.map((phase, i) => ({
          name: "Phase " + phase,
          converter: () =>
            data["ConsumptionActivePower" + phase],
          color: "rgb(" + ABSTRACT_HISTORY_CHART.PHASE_COLORS[i] + ")",
          stack: 3,
        })),
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
  }

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {
    return CONSUMPTION_METER_CHART_DETAILS_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.ROUTE, THIS.TRANSLATE);
  }

}
