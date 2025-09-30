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

        // THIS.REFRESH_WIDGET_DATA.PIPE(takeUntil(THIS.NG_UNSUBSCRIBE)).subscribe(() => {
        // THIS.UPDATE_VALUES()
        // })
    }

    /**
     * Unsubscribes to 10 minute Interval Observable
     */
    protected unsubscribeWidgetRefresh() {
        // XXX disabled to reduce server load

        // if (THIS.NG_UNSUBSCRIBE.IS_STOPPED == false) {
        //     THIS.NG_UNSUBSCRIBE.NEXT();
        // THIS.NG_UNSUBSCRIBE.COMPLETE();
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

        const resolution = calculateResolution(THIS.SERVICE, fromDate, toDate).resolution;
        const result: Promise<QueryHistoricTimeseriesDataResponse> = new Promise<QueryHistoricTimeseriesDataResponse>((resolve, reject) => {
            THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
                THIS.SERVICE.GET_CONFIG().then(config => {
                    THIS.GET_CHANNEL_ADDRESSES(edge, config).then(channelAddresses => {
                        const request = new QueryHistoricTimeseriesDataRequest(DATE_UTILS.MAX_DATE(fromDate, edge?.firstSetupProtocol), toDate, channelAddresses, resolution);
                        EDGE.SEND_REQUEST(THIS.SERVICE.WEBSOCKET, request).then(response => {
                            const result = (response as QueryHistoricTimeseriesDataResponse).result;
                            THIS.ACTIVE_QUERY_DATA = RESPONSE.ID;
                            if (OBJECT.KEYS(RESULT.DATA).length != 0 && OBJECT.KEYS(RESULT.TIMESTAMPS).length != 0) {
                                resolve(response as QueryHistoricTimeseriesDataResponse);
                            } else {
                                reject(new JsonrpcResponseError(RESPONSE.ID, { code: 0, message: "Result was empty" }));
                            }
                        }).catch(reason => reject(reason));
                    }).catch(reason => reject(reason));
                });
            });
        }).then((response) => {
            if (THIS.ACTIVE_QUERY_DATA !== RESPONSE.ID) {
                return;
            }
            return DATE_TIME_UTILS.NORMALIZE_TIMESTAMPS(RESOLUTION.UNIT, response);
        });
        return result;
    }

    /**
     * checks if widget is allowed to be refreshed
     */
    // protected checkAllowanceWidgetRefresh(): boolean {
    //     let currentDate = new Date();
    //     let allowRefresh: boolean = false;
    //     if (isAfter(THIS.SERVICE.HISTORY_PERIOD.FROM.GET_DATE(), CURRENT_DATE.GET_DATE()) || CURRENT_DATE.GET_DATE() == THIS.SERVICE.HISTORY_PERIOD.FROM.GET_DATE()) {
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
