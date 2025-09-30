import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "chargerChart",
  templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class ChargerChartDetailsComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, route: ActivatedRoute, translate: TranslateService): HISTORY_UTILS.CHART_DATA {
    const component = CONFIG.GET_COMPONENT(ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
    return {
      input: [{
        name: COMPONENT.ID,
        powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActualPower"),
        energyChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActualEnergy"),
      }],
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => [{
        name: COMPONENT.ALIAS,
        nameSuffix: (energyQueryResponse: QueryHistoricTimeseriesEnergyResponse) => {
          return ENERGY_QUERY_RESPONSE.RESULT.DATA[COMPONENT.ID + "/ActualEnergy"];
        },
        converter: () => {
          return data[COMPONENT.ID];
        },
        color: "rgb(0,152,204)",
        hiddenOnInit: false,
        stack: 2,
      }],
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
    return CHARGER_CHART_DETAILS_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.ROUTE, THIS.TRANSLATE);
  }
}
