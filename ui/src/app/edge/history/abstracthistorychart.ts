import { ChannelAddress, Edge, EdgeConfig, Service } from "../../shared/shared";
import { ChartDataSets } from 'chart.js';
import { ChartOptions, EMPTY_DATASET } from './shared';
import { JsonrpcResponseError } from "../../shared/jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "../../shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesDataResponse } from "../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { queryHistoricTimeseriesEnergyPerPeriodRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesEnergyPerPeriodRequest';
import { queryHistoricTimeseriesEnergyPerPeriodResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse';
import { TranslateService } from '@ngx-translate/core';

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
export abstract class AbstractHistoryChart {

    public loading: boolean = true;
    public spinnerId: string = "";

    //observable is used to fetch new chart data every 10 minutes
    // private refreshChartData = interval(600000);

    //observable is used to refresh chart height dependend on the window size
    // private refreshChartHeight = fromEvent(window, 'resize', null, null);

    // private ngUnsubscribe: Subject<void> = new Subject<void>();

    protected labels: Date[] = [];
    protected datasets: ChartDataSets[] = EMPTY_DATASET;
    protected options: ChartOptions | null = null;
    protected colors = []
    // prevents subscribing more than once
    protected hasSubscribed: boolean = false;

    // Colors for Phase 1-3
    protected phase1Color = {
        backgroundColor: 'rgba(255,127,80,0.05)',
        borderColor: 'rgba(255,127,80,1)',
    }
    protected phase2Color = {
        backgroundColor: 'rgba(0,0,255,0.1)',
        borderColor: 'rgba(0,0,255,1)',
    }
    protected phase3Color = {
        backgroundColor: 'rgba(128,128,0,0.1)',
        borderColor: 'rgba(128,128,0,1)',
    }

    constructor(
        protected service: Service,
        protected translate: TranslateService
    ) {
    }

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
                    this.setLabel(config);
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
     * Sends the Historic Timeseries Energy per Period Query and makes sure the result is not empty.
     * 
     * @param fromDate the From-Date
     * @param toDate   the To-Date
     * @param resolution the resolution in seconds
     * @param edge     the current Edge
     * @param ws       the websocket
     */
    protected queryHistoricTimeseriesEnergyPerPeriod(fromDate: Date, toDate: Date, channelAddresses: ChannelAddress[], resolution: number): Promise<queryHistoricTimeseriesEnergyPerPeriodResponse> {
        return new Promise((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    let request = new queryHistoricTimeseriesEnergyPerPeriodRequest(fromDate, toDate, channelAddresses, resolution);
                    edge.sendRequest(this.service.websocket, request).then(response => {
                        let result = (response as QueryHistoricTimeseriesDataResponse).result;
                        if (Object.keys(result.data).length != 0 && Object.keys(result.timestamps).length != 0) {
                            resolve(response as queryHistoricTimeseriesEnergyPerPeriodResponse);
                        } else {
                            reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                        }
                    }).catch(reason => reject(reason));
                })
            });
        });
    }

    /**
     * checks if chart is allowed to be refreshed
     * 
     */
    // protected checkAllowanceChartRefresh(): boolean {
    //     let currentDate = new Date();
    //     let allowRefresh: boolean = false;
    //     if (isAfter(this.service.historyPeriod.to, currentDate) || currentDate.getDate() == this.service.historyPeriod.from.getDate()) {
    //         allowRefresh = true;
    //     } else {
    //         allowRefresh = false;
    //     }
    //     return allowRefresh;
    // }

    /**
     * Subscribe to Chart Refresh if allowed
     * Unsubscribe to Chart Refresh if necessary
     */
    protected autoSubscribeChartRefresh() {
        // XXX disabled to reduce server load

        // if (this.hasSubscribed == false && this.checkAllowanceChartRefresh() == true) {
        //     if (this.ngUnsubscribe.isStopped == true) {
        //         this.ngUnsubscribe.isStopped = false;
        //     }
        //     this.refreshChartData.pipe(takeUntil(this.ngUnsubscribe)).subscribe(() => {
        //         this.updateChart();
        //     })
        //     this.refreshChartHeight.pipe(takeUntil(this.ngUnsubscribe), debounceTime(200), delay(100)).subscribe(() => {
        //         this.getChartHeight();
        //     });
        //     this.hasSubscribed = true;
        // } else if (this.hasSubscribed == true && this.checkAllowanceChartRefresh() == false) {
        //     this.unsubscribeChartRefresh();
        // }
    }

    /**
     * Unsubscribes to 10 minute Interval Observable and Window Resize Observable
     */
    protected unsubscribeChartRefresh() {
        // XXX disabled to reduce server load

        // this.hasSubscribed = false;
        // this.ngUnsubscribe.next();
        // this.ngUnsubscribe.complete();
    }

    /**
     * Sets the Label of Chart
     */
    protected abstract setLabel(config: EdgeConfig)

    /**
     * Updates and Fills the Chart
     */
    protected abstract updateChart()

    /**
     * Initializes empty chart on error
     * @param spinnerSelector to stop spinner
     */
    protected initializeChart() {
        EMPTY_DATASET[0].label = this.translate.instant('Edge.History.noData')
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
        this.service.stopSpinner(this.spinnerId);
    }

    /**
     * Sets Chart Height
     */
    protected abstract getChartHeight()
}
