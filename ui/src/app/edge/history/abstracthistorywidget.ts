// @ts-strict-ignore
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { QueryHistoricTimeseriesDataRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesDataRequest";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { calculateResolution } from "./shared";

// NOTE: Auto-refresh of widgets is currently disabled to reduce server load
export abstract class AbstractHistoryWidget {

    private activeQueryData: string;

    //observable is used to fetch new widget data every 5 minutes
    // private refreshWidgetData = interval(600000);

    // private ngUnsubscribe: Subject<void> = new Subject<void>();

    constructor(
        protected service: Service,
    ) { }

    /**
     * Subscribes to 10 minute Interval Observable to update data in Flat Widget
     */
    protected subscribeWidgetRefresh() {
        // XXX disabled to reduce server load

        // this.refreshWidgetData.pipe(takeUntil(this.ngUnsubscribe)).subscribe(() => {
        // this.updateValues()
        // })
    }

    /**
     * Unsubscribes to 10 minute Interval Observable
     */
    protected unsubscribeWidgetRefresh() {
        // XXX disabled to reduce server load

        // if (this.ngUnsubscribe.isStopped == false) {
        //     this.ngUnsubscribe.next();
        // this.ngUnsubscribe.complete();
        // }
    }

    /**
     * Sends the Historic Timeseries Data Query and makes sure the result is not empty.
     *
     * @param fromDate the From-Date
     * @param toDate   the To-Date
     * @param edge     the current Edge
     * @param ws       the websocket
    */
    protected queryHistoricTimeseriesData(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesDataResponse> {

        const resolution = calculateResolution(this.service, fromDate, toDate).resolution;
        const result: Promise<QueryHistoricTimeseriesDataResponse> = new Promise<QueryHistoricTimeseriesDataResponse>((resolve, reject) => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        const request = new QueryHistoricTimeseriesDataRequest(DateUtils.maxDate(fromDate, edge?.firstSetupProtocol), toDate, channelAddresses, resolution);
                        edge.sendRequest(this.service.websocket, request).then(response => {
                            const result = (response as QueryHistoricTimeseriesDataResponse).result;
                            this.activeQueryData = response.id;
                            if (Object.keys(result.data).length != 0 && Object.keys(result.timestamps).length != 0) {
                                resolve(response as QueryHistoricTimeseriesDataResponse);
                            } else {
                                reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                            }
                        }).catch(reason => reject(reason));
                    }).catch(reason => reject(reason));
                });
            });
        }).then((response) => {
            if (this.activeQueryData !== response.id) {
                return;
            }
            return DateTimeUtils.normalizeTimestamps(resolution.unit, response);
        });
        return result;
    }

    /**
     * checks if widget is allowed to be refreshed
     */
    // protected checkAllowanceWidgetRefresh(): boolean {
    //     let currentDate = new Date();
    //     let allowRefresh: boolean = false;
    //     if (isAfter(this.service.historyPeriod.from.getDate(), currentDate.getDate()) || currentDate.getDate() == this.service.historyPeriod.from.getDate()) {
    //         allowRefresh = true;
    //     } else {
    //         allowRefresh = false;
    //     }
    //     return allowRefresh;
    // }

    /**
     * Gets the ChannelAddresses that should be queried.
     *
     * @param edge the current Edge
     * @param config the EdgeConfig
     */
    protected abstract getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]>;

    /**
     * Updates and Fills the Chart
     */
    protected abstract updateValues();
}
