import { subSeconds } from "date-fns";
import { QueryHistoricTimeseriesDataResponse } from "../../jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { DATE_TIME_REGEX, DateTimeUtils } from "./datetime-utils";

describe("DateTimeUtils", () => {

    const timeZone = "Europe/Berlin";

    it("+isDifferenceInSecondsGreaterThan - expected true", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date = subSeconds(new Date(), 11);
        expect(DateTimeUtils.isDifferenceInSecondsGreaterThan(10, currDate, lastUpdate)).toEqual(true);
    });
    it("+isDifferenceInSecondsGreaterThan - expected false", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date = subSeconds(new Date(), 9);
        expect(DateTimeUtils.isDifferenceInSecondsGreaterThan(10, currDate, lastUpdate)).toEqual(false);
    });
    it("+isDifferenceInSecondsGreaterThan - invalid Dates", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date | null = null;
        expect(DateTimeUtils.isDifferenceInSecondsGreaterThan(10, currDate, lastUpdate)).toEqual(false);
    });
    it("+toISO8601WithOffsetFormat - invalid Datetime string", () => {
        const inValidDateTime: string | null = null;
        expect(() => DateTimeUtils.formatToISOZonedDateTime(inValidDateTime, timeZone)).toThrow(new Error(DateTimeUtils.INVALID_DATE_TIME_STRING));
    });
    it("+toISO8601WithOffsetFormat - valid Datetime string", () => {
        const validDateTime: string | null = "2023-11-16T08:07:00";
        expect(DateTimeUtils.formatToISOZonedDateTime(validDateTime, timeZone)).toMatch(DATE_TIME_REGEX);
    });
    it("+isOfValidDateTimeFormat - test all valid ionic date-time formats", () => {
        const validDateTime: string[] = ["2025", "2023-11-16T08:07:00", "2023-11-16T08:07", "2023-11-16T08:07:00Z", "08:07"];
        expect(validDateTime.map(datetime => DateTimeUtils.isOfValidDateTimeFormat(datetime))).toEqual(validDateTime.map(_el => true));
    });
    it("+toWallClockDate - keeps the target timezone wall clock instead of the device timezone", () => {
        const wallClockDate = DateTimeUtils.toWallClockDate("2024-06-30T22:00:00Z", timeZone);
        expect(DateTimeUtils.toLocalDateTimeString(wallClockDate)).toEqual("2024-07-01T00:00:00");
    });
    it("+normalizeTimestamps - converts ISO instants to EMS wall clock timestamps for chart rendering", () => {
        const response = new QueryHistoricTimeseriesDataResponse("1", {
            timestamps: ["2024-06-30T22:00:00Z", "2024-06-30T23:00:00Z"],
            data: { "_sum/ConsumptionActivePower": [1, 2] },
        });

        const normalized = DateTimeUtils.normalizeTimestamps("Hours" as any, response, timeZone);

        expect(normalized.result.timestamps).toEqual([
            "2024-07-01T00:00:00",
            "2024-07-01T01:00:00",
        ]);
    });
});
