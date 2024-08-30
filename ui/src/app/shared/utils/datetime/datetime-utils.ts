// @ts-strict-ignore
import { format, startOfMonth, startOfYear } from "date-fns";
import { de } from "date-fns/locale";
import { ChronoUnit } from "src/app/edge/history/shared";

import { QueryHistoricTimeseriesDataResponse } from "../../jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "../../jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { DateUtils } from "../date/dateutils";

export class DateTimeUtils {

  /**
   * Normalizes timestamps depending on chosen period
   *
   * @param unit the Chronounit
   * @param energyPerPeriodResponse the timeseries data
   * @returns the adjusted timestamps
   */
  public static normalizeTimestamps(unit: ChronoUnit.Type, energyPerPeriodResponse: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse): QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse {

    switch (unit) {
      case ChronoUnit.Type.MONTHS: {

        // Change first timestamp to start of month
        const formattedDate = startOfMonth(DateUtils.stringToDate(energyPerPeriodResponse.result.timestamps[0]));
        energyPerPeriodResponse.result.timestamps[0] = format(formattedDate, "yyyy-MM-dd HH:mm:ss", { locale: de })?.toString() ?? energyPerPeriodResponse.result.timestamps[0];

        // show 12 stacks, even if no data and timestamps
        const newTimestamps: string[] = [];
        const firstTimestamp = DateUtils.stringToDate(energyPerPeriodResponse.result.timestamps[0]);

        if (firstTimestamp.getMonth() !== 0) {
          for (let i = 0; i <= (firstTimestamp.getMonth() - 1); i++) {
            newTimestamps.push(new Date(firstTimestamp.getFullYear(), i).toString());

            for (const channel of Object.keys(energyPerPeriodResponse.result.data)) {
              energyPerPeriodResponse.result.data[channel.toString()]?.unshift(null);
            }
          }
        }

        energyPerPeriodResponse.result.timestamps = newTimestamps.concat(energyPerPeriodResponse.result.timestamps);
        break;
      }

      case ChronoUnit.Type.YEARS: {

        // Change dates to be first day of year
        const formattedDates = energyPerPeriodResponse.result.timestamps.map((timestamp) =>
          startOfYear(DateUtils.stringToDate(timestamp)));
        energyPerPeriodResponse.result.timestamps = formattedDates.map(date => format(date, "yyyy-MM-dd HH:mm:ss", { locale: de })?.toString());
        break;
      }
    }

    return energyPerPeriodResponse;
  }
}
