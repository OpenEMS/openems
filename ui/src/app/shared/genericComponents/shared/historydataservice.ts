import { Inject, Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";

import { QueryHistoricTimeseriesEnergyRequest } from "../../jsonrpc/request/queryHistoricTimeseriesEnergyRequest";
import { QueryHistoricTimeseriesEnergyResponse } from "../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge, Service, Websocket } from "../../shared";
import { DataService } from "./dataservice";

@Injectable()
export class HistoryDataService extends DataService {

  private channelAddresses: { [sourceId: string]: ChannelAddress } = {}
  private subscribeChannelsTimeout: any | null = null;
  private date: BehaviorSubject<{ from: Date, to: Date }> = new BehaviorSubject(null);

  constructor(
    @Inject(Websocket) protected websocket: Websocket,
    @Inject(Service) protected service: Service
  ) {
    super()
  }

  public getValues(channelAddresses: ChannelAddress[], edge: Edge, componentId: string) {

    for (let channelAddress of channelAddresses) {
      this.channelAddresses[channelAddress.toString()] = channelAddress;
    }

    if (this.subscribeChannelsTimeout == null) {
      this.subscribeChannelsTimeout = setTimeout(() => {
        setInterval(() => {
          if (Object.entries(this.channelAddresses).length > 0
            && (this.date.value?.from != this.service.historyPeriod.from
              || this.date.value?.to != this.service.historyPeriod.to)) {
            edge.sendRequest(this.websocket, new QueryHistoricTimeseriesEnergyRequest(this.service.historyPeriod.from, this.service.historyPeriod.to, Object.values(this.channelAddresses)))
              .then((response) => {
                let allComponents = {};
                let result = (response as QueryHistoricTimeseriesEnergyResponse).result
                for (let [key, value] of Object.entries(result.data)) {
                  allComponents[key] = value;
                }
                this.currentValue.next({ allComponents: allComponents });
              }).catch(err => console.warn(err))
            this.date.next({ from: this.service.historyPeriod.from, to: this.service.historyPeriod.to });
          }
        }, 500)
      }, 300)
    }
  }
}