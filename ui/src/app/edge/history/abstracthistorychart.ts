import { ChannelAddress, Edge, EdgeConfig, Service } from "../../shared/shared";
import { ChartOptions, Dataset, EMPTY_DATASET } from './shared';
import { JsonrpcResponseError } from "../../shared/jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "../../shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesDataResponse } from "../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { TranslateService } from '@ngx-translate/core';
import { interval, Subject, fromEvent } from 'rxjs';
import { takeUntil, debounceTime, delay } from 'rxjs/operators';

export abstract class AbstractHistoryChart {


    public loading: boolean = true;

    //observable is used to fetch new chart data every 5 minutes
    // XXX disabled to reduce server load
    // private refreshChartData = interval(300000);
    //observable is used to refresh chart height dependend on the window size
    private refreshChartHeight = fromEvent(window, 'resize', null, null);

    private ngUnsubscribe: Subject<void> = new Subject<void>();

    protected labels: Date[] = [];
    protected datasets: Dataset[] = EMPTY_DATASET;
    protected options: ChartOptions;
    protected colors = []

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
     * Subscribes to 5 minute Interval Observable and Window Resize Observable to fetch new data and resize chart if needed
     */
    protected subscribeChartRefresh() {
        // XXX disabled to reduce server load
        //     this.refreshChartData.pipe(takeUntil(this.ngUnsubscribe)).subscribe(() => {
        //         this.updateChart()
        //     })
        //     this.refreshChartHeight.pipe(takeUntil(this.ngUnsubscribe), debounceTime(200), delay(100)).subscribe(() => {
        //         this.getChartHeight();
        //     });
    }

    /**
     * Unsubscribes to 5 minute Interval Observable and Window Resize Observable
     */
    protected unsubscribeChartRefresh() {
        // XXX disabled to reduce server load
        //     this.ngUnsubscribe.next();
        //     this.ngUnsubscribe.complete();
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
     */
    protected initializeChart(reason) {
        if (reason.error.code == 1) {
            EMPTY_DATASET[0].label = this.translate.instant('Edge.History.tryAgain')
        } else {
            EMPTY_DATASET[0].label = this.translate.instant('Edge.History.noData')
        }
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
    }

    /**
     * Sets Chart Height
     */
    protected abstract getChartHeight()
}