// @ts-strict-ignore
import { Inject, Injectable } from "@angular/core";

import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { QueryHistoricTimeseriesEnergyRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesEnergyRequest";
import { Service } from "src/app/shared/service/service";
import { Websocket } from "src/app/shared/service/websocket";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { DataService } from "../../shared/components/shared/dataservice";
import { QueryHistoricTimeseriesEnergyResponse } from "../../shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge } from "../../shared/shared";

@Injectable()
export class HistoryDataService extends DataService {

  public queryChannelsTimeout: ReturnType<typeof setTimeout> | null = null;
  protected override timestamps: string[] = [];
  private activeQueryData: string;
  private channelAddresses: { [sourceId: string]: ChannelAddress } = {};

  constructor(
    @Inject(Websocket) protected websocket: Websocket,
    @Inject(Service) protected service: Service,
  ) {
    super(service);
  }

  public getValues(channelAddresses: ChannelAddress[], edge: Edge, componentId: string) {

    for (const channelAddress of channelAddresses) {
      THIS.CHANNEL_ADDRESSES[CHANNEL_ADDRESS.TO_STRING()] = channelAddress;
    }

    if (THIS.QUERY_CHANNELS_TIMEOUT == null) {

      THIS.QUERY_CHANNELS_TIMEOUT = setTimeout(() => {
        if (OBJECT.ENTRIES(THIS.CHANNEL_ADDRESSES).length > 0) {

          THIS.SERVICE.HISTORY_PERIOD.SUBSCRIBE(date => {

            const request = new QueryHistoricTimeseriesEnergyRequest(
              DATE_UTILS.MAX_DATE(DATE.FROM, edge?.firstSetupProtocol),
              DATE.TO,
              OBJECT.VALUES(THIS.CHANNEL_ADDRESSES),
            );

            THIS.ACTIVE_QUERY_DATA = REQUEST.ID;

            EDGE.SEND_REQUEST(THIS.WEBSOCKET, request)
              .then((response) => {
                if (THIS.ACTIVE_QUERY_DATA === RESPONSE.ID) {
                  const allComponents = {};
                  const result = (response as QueryHistoricTimeseriesEnergyResponse).result;

                  for (const [key, value] of OBJECT.ENTRIES(RESULT.DATA)) {
                    allComponents[key] = value;
                  }

                  THIS.CURRENT_VALUE.SET({ allComponents: allComponents });
                  THIS.TIMESTAMPS = RESPONSE.RESULT["timestamps"] ?? [];
                }
              })
              .catch(err => CONSOLE.WARN(err))
              .finally(() => {
                THIS.QUERY_CHANNELS_TIMEOUT = null;
              });
          });
        }
      }, ChartConstants.REQUEST_TIMEOUT);
    }
  }

  public override unsubscribeFromChannels(channels: ChannelAddress[]) {
    return;
  }

  public override refresh(ev: CustomEvent) {
    THIS.GET_VALUES(OBJECT.VALUES(THIS.CHANNEL_ADDRESSES), THIS.EDGE, "");
    setTimeout(() => {
      (EV.TARGET as HTMLIonRefresherElement).complete();
    }, 1000);
  }
}
