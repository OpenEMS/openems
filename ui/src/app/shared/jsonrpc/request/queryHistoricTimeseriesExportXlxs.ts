import { format } from "date-fns";
import { JsonrpcRequest } from "../base";

/**
 * Queries historic timeseries data; exports to Xlsx (Excel) file.
 *
 * ```json
 * {
 *     "id": "UUID",
 *     "jsonrpc": "2.0",
 *     "method": "queryHistoricTimeseriesExportXlxs",
 *     "params": {
 *         "timezone": "Europe/Berlin",
 *         "fromDate": "YYYY-MM-DD",
 *         "toDate": "YYYY-MM-DD"
 *     }
 * }
 * ```
 */
export class QueryHistoricTimeseriesExportXlxsRequest extends JsonrpcRequest {
    private static METHOD: string = "queryHistoricTimeseriesExportXlxs";

    public constructor(fromDate: Date, toDate: Date) {
        super(QueryHistoricTimeseriesExportXlxsRequest.METHOD, {
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            fromDate: format(fromDate, "yyyy-MM-dd"),
            toDate: format(toDate, "yyyy-MM-dd"),
        });
    }
}
