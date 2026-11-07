import { subSeconds } from "date-fns";
import { ChronoUnit } from "src/app/edge/history/shared";
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
        const inValidDateTime: string | null = "Invalid Date";
        expect(() => DateTimeUtils.formatToISOZonedDateTime(inValidDateTime, timeZone)).toThrow(
            new Error(DateTimeUtils.INVALID_DATE_TIME_STRING),
        );
    });
    it("+toISO8601WithOffsetFormat - Datetime is null", () => {
        const inValidDateTime: string | null = null;
        expect(DateTimeUtils.formatToISOZonedDateTime(inValidDateTime, timeZone)).toEqual(null);
    });
    it("+toISO8601WithOffsetFormat - valid Datetime string", () => {
        const validDateTime: string | null = "2023-11-16T08:07:00";
        expect(DateTimeUtils.formatToISOZonedDateTime(validDateTime, timeZone)).toMatch(DATE_TIME_REGEX);
    });
    it("+isOfValidDateTimeFormat - test all valid ionic date-time formats", () => {
        const validDateTime: string[] = [
            "2025",
            "2023-11-16T08:07:00",
            "2023-11-16T08:07",
            "2023-11-16T08:07:00Z",
            "08:07",
        ];
        expect(validDateTime.map((datetime) => DateTimeUtils.isOfValidDateTimeFormat(datetime))).toEqual(
            validDateTime.map((_el) => true),
        );
    });

    const response = (timestamps: string[]) =>
        new QueryHistoricTimeseriesDataResponse("UUID", {
            timestamps: [...timestamps],
            data: { "_sum/ActivePower": timestamps.map(() => 1) },
        });

    it("+normalizeTimestamps - empty timestamps returns response unchanged", () => {
        const empty = response([]);
        expect(DateTimeUtils.normalizeTimestamps(ChronoUnit.Type.MONTHS, empty).result.timestamps).toEqual([]);
        expect(DateTimeUtils.normalizeTimestamps(ChronoUnit.Type.YEARS, empty).result.timestamps).toEqual([]);
    });

    it("+normalizeTimestamps MONTHS - invalid first or last timestamp returns response unchanged", () => {
        const invalid = response(["not-a-date", "2024-11-20T10:00:00"]);
        expect(DateTimeUtils.normalizeTimestamps(ChronoUnit.Type.MONTHS, invalid).result.timestamps).toEqual([
            "not-a-date",
            "2024-11-20T10:00:00",
        ]);
    });

    it("+normalizeTimestamps MONTHS - pads months up to January and sets first timestamp to start of month", () => {
        const result = DateTimeUtils.normalizeTimestamps(
            ChronoUnit.Type.MONTHS,
            response(["2024-03-15T10:00:00", "2024-11-20T10:00:00"]),
        ).result;
        // January and February prepended, original first entry (normalized to start of month) at index 2
        expect(result.timestamps.length).toEqual(4);
        expect(result.timestamps[2]).toEqual("2024-03-01 00:00:00");
        // data padded with nulls for the prepended months
        expect(result.data["_sum/ActivePower"].slice(0, 2)).toEqual([null, null]);
        expect(result.data["_sum/ActivePower"].length).toEqual(4);
    });

    it("+normalizeTimestamps YEARS - normalizes all timestamps to start of year", () => {
        const result = DateTimeUtils.normalizeTimestamps(
            ChronoUnit.Type.YEARS,
            response(["2023-05-10T10:00:00", "2024-11-20T10:00:00"]),
        ).result;
        expect(result.timestamps).toEqual(["2023-01-01 00:00:00", "2024-01-01 00:00:00"]);
    });

    it("+normalizeTimestamps YEARS - invalid entry returns response unchanged without partial update", () => {
        const invalid = response(["2023-05-10T10:00:00", "garbage"]);
        expect(DateTimeUtils.normalizeTimestamps(ChronoUnit.Type.YEARS, invalid).result.timestamps).toEqual([
            "2023-05-10T10:00:00",
            "garbage",
        ]);
    });
});
