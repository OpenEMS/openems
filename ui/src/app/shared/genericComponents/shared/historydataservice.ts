import { Inject, Injectable } from "@angular/core";

import { QueryHistoricTimeseriesEnergyRequest } from "../../jsonrpc/request/queryHistoricTimeseriesEnergyRequest";
import { QueryHistoricTimeseriesEnergyResponse } from "../../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, Edge, Service, Websocket } from "../../shared";
import { DataService } from "./dataservice";

@Injectable()
export class HistoryDataService extends DataService {

  private channelAddresses: { [sourceId: string]: ChannelAddress } = {};
  private queryChannelsTimeout: any | null = null;

  constructor(
    @Inject(Websocket) protected websocket: Websocket,
    @Inject(Service) protected service: Service
  ) {
    super();
  }

  public getValues(channelAddresses: ChannelAddress[], edge: Edge, componentId: string) {

    for (let channelAddress of channelAddresses) {
      this.channelAddresses[channelAddress.toString()] = channelAddress;
    }

    if (this.queryChannelsTimeout == null) {
      this.queryChannelsTimeout = setTimeout(() => {
        if (Object.entries(this.channelAddresses).length > 0) {

          this.service.historyPeriod.subscribe(date => {
            edge.sendRequest(this.websocket, new QueryHistoricTimeseriesEnergyRequest(date.from, date.to, Object.values(this.channelAddresses)))
              .then((response) => {
                let allComponents = {};
                let thisComponent = {};
                let result = (response as QueryHistoricTimeseriesEnergyResponse).result;
                for (let [key, value] of Object.entries(result.data)) {
                  allComponents[key] = value;
                }
                this.currentValue.next({ thisComponent: thisComponent, allComponents: allComponents });
              }).catch(err => console.warn(err));
          });
        }
      }, 100);
    }
  }
}