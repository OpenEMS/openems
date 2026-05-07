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
        private timeZone: string = Intl.DateTimeFormat().resolvedOptions().timeZone,
    ) {
        super(QueryHistoricTimeseriesExportXlxsRequest.METHOD, {
            timezone: timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
        });
        // delete local fields, otherwise they are sent with the JSON-RPC Request
        delete this.fromDate;
        delete this.toDate;
        delete this.timeZone;
    }

}
