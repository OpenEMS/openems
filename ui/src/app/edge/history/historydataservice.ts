// @ts-strict-ignore
import { Inject, Injectable } from "@angular/core";

import { DataService } from "../../shared/components/shared/dataservice";
import { QueryHistoricTimeseriesEnergyResponse } from "../../shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge } from "../../shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { QueryHistoricTimeseriesEnergyRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesEnergyRequest";
import { Websocket } from "src/app/shared/service/websocket";
import { Service } from "src/app/shared/service/service";
import { RefresherCustomEvent } from "@ionic/angular";

@Injectable()
export class HistoryDataService extends DataService {

  public queryChannelsTimeout: any | null = null;
  protected override timestamps: string[] = [];
  private channelAddresses: { [sourceId: string]: ChannelAddress } = {};

  constructor(
    @Inject(Websocket) protected websocket: Websocket,
    @Inject(Service) protected service: Service,
  ) {
    super();
  }

  public getValues(channelAddresses: ChannelAddress[], edge: Edge, componentId: string) {

    for (const channelAddress of channelAddresses) {
      this.channelAddresses[channelAddress.toString()] = channelAddress;
    }

    if (this.queryChannelsTimeout == null) {

      this.queryChannelsTimeout = setTimeout(() => {
        if (Object.entries(this.channelAddresses).length > 0) {

          this.service.historyPeriod.subscribe(date => {
            edge.sendRequest(this.websocket, new QueryHistoricTimeseriesEnergyRequest(DateUtils.maxDate(date.from, edge?.firstSetupProtocol), date.to, Object.values(this.channelAddresses)))
              .then((response) => {
                const allComponents = {};
                const result = (response as QueryHistoricTimeseriesEnergyResponse).result;
                for (const [key, value] of Object.entries(result.data)) {
                  allComponents[key] = value;
                }
                this.currentValue.next({ allComponents: allComponents });
                this.timestamps = response.result['timestamps'] ?? [];
              }).catch(err => console.warn(err))
              .finally(() => {
              });
          });
        }
      }, 100);
    }
  }

  public override unsubscribeFromChannels(channels: ChannelAddress[]) {
    return;
  }

  public override refresh(ev: RefresherCustomEvent) {
    this.getValues(Object.values(this.channelAddresses), this.edge, "");
    ev.target.complete();
  }
}
