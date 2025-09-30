// @ts-strict-ignore
import { format } from "date-fns";
import { JsonrpcRequest } from "../base";

/**
 * Queries historic timeseries data; exports to Xlsx (Excel) file.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "queryHistoricTimeseriesExportXlxs",
 *   "params": {
 *     "timezone": Number,
 *     "fromDate": YYYY-MM-DD,
 *     "toDate": YYYY-MM-DD
 *   }
 * }
 * </pre>
 */
export class QueryHistoricTimeseriesExportXlxsRequest extends JsonrpcRequest {

    private static METHOD: string = "queryHistoricTimeseriesExportXlxs";

    public constructor(
        private fromDate: Date,
        private toDate: Date,
    ) {
        super(QUERY_HISTORIC_TIMESERIES_EXPORT_XLXS_REQUEST.METHOD, {
            timezone: INTL.DATE_TIME_FORMAT().resolvedOptions().timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete THIS.FROM_DATE;
        delete THIS.TO_DATE;
    }

}
