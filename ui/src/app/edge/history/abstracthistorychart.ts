import { JsonrpcResponseError } from "../../shared/jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "../../shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesDataResponse } from "../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Edge, Service } from "../../shared/shared";
import { QuerykWhResponse } from 'src/app/shared/jsonrpc/response/querykWhResponse';
import { QuerykWhRequest } from 'src/app/shared/jsonrpc/request/querykWhRequest';

export abstract class AbstractHistoryChart {

    constructor(
        protected service: Service
    ) { }

    /**
     * Gets the ChannelAdresses that should be queried.
     * 
     * @param edge the current Edge
     */
    protected abstract getChannelAddresses(edge: Edge): Promise<ChannelAddress[]>;

    /**
     * Sends the Historic Timeseries Data Query and makes sure the result is not empty.
     * 
     * @param fromDate the From-Date
     * @param toDate   the To-Date
     * @param edge     the current Edge
     * @param ws       the websocket
     */
    protected queryHistoricTimeseriesData(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesDataResponse> {
        return new Promise((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                this.getChannelAddresses(edge).then(channelAddresses => {
                    let request = new QueryHistoricTimeseriesDataRequest(fromDate, toDate, channelAddresses);
                    edge.sendRequest(this.service.websocket, request).then(response => {
                        let result = (response as QueryHistoricTimeseriesDataResponse).result;
                        if (Object.keys(result.data).length != 0 && Object.keys(result.timestamps).length != 0) {
                            resolve(response as QueryHistoricTimeseriesDataResponse);
                        } else {
                            reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                        }
                    }).catch(reason => reject(reason));
                }).catch(reason => reject(reason));
            });
        });
    }

    // protected querykWh(fromDate: Date, toDate: Date): Promise<QuerykWhResponse> {
    //     return new Promise((resolve, reject) => {
    //         this.service.getCurrentEdge().then(edge => {
    //             this.getChannelAddresses(edge).then(channelAddresses => {
    //                 let request = new QuerykWhRequest(fromDate, toDate, channelAddresses);
    //                 edge.sendRequest(this.service.websocket, request).then(response => {
    //                     let result = (response as QuerykWhResponse).result;
    //                     if (Object.keys(result.data).length != 0) {
    //                         resolve(response as QuerykWhResponse);
    //                     } else {
    //                         reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
    //                     }
    //                 }).catch(reason => reject(reason));
    //             }).catch(reason => reject(reason));
    //         })
    //     })
    // }
}