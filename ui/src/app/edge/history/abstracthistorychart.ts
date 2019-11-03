import { JsonrpcResponseError } from "../../shared/jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "../../shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesDataResponse } from "../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Edge, EdgeConfig, Service } from "../../shared/shared";
import { EMPTY_DATASET, Dataset, ChartOptions } from './shared';

export abstract class AbstractHistoryChart {

    protected labels: Date[] = [];
    protected datasets: Dataset[] = EMPTY_DATASET;
    protected options: ChartOptions;
    protected colors = []
    protected loading: boolean = true;

    // Colors for Phase 1-3 (additional color is for GridBuy/Sell etc.)
    protected phase1Color = {
        backgroundColor: 'rgba(139,69,19,0.05)',
        borderColor: 'rgba(139,69,19,1)',
    }
    protected phase1AdditionalColor = {
        backgroundColor: 'rgba(255,127,36,0.05)',
        borderColor: 'rgba(255,127,36,1)',
    }
    protected phase2Color = {
        backgroundColor: 'rgba(0,0,0,0.1)',
        borderColor: 'rgba(0,0,0,1)',
    }
    protected phase2AdditionalColor = {
        backgroundColor: 'rgba(255,222,173,0.1)',
        borderColor: 'rgba(255,222,173,1)',
    }
    protected phase3Color = {
        backgroundColor: 'rgba(112,128,144,0.1)',
        borderColor: 'rgba(112,128,144,1)',
    }
    protected phase3AdditionalColor = {
        backgroundColor: 'rgba(198,226,255,0.1)',
        borderColor: 'rgba(198,226,255,1)',
    }


    constructor(
        protected service: Service
    ) { }

    /**
     * Gets the ChannelAdresses that should be queried.
     * 
     * @param edge the current Edge
     * @param config the EdgeConfig
     */
    protected abstract getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]>;

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
                this.service.getConfig().then(config => {
                    this.getChannelAddresses(edge, config).then(channelAddresses => {
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
                })
            });
        });
    }

    /**
     * Sets the Label of Chart
     */
    protected abstract setLabel()

    /**
     * Updates and Fills the Chart
     */
    protected abstract updateChart()

    /**
     * Initializes Chart
     */
    protected initializeChart() {
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
    }

    /**
     * Sets Chart Height
     */
    protected abstract getChartHeight()
}