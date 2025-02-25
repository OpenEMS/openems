import { subSeconds } from "date-fns";
import { DateTimeUtils } from "./datetime-utils";

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
        expect(DateTimeUtils.formatToISOZonedDateTime(validDateTime, timeZone)).toEqual("2023-11-16T09:07:00.000+01:00");

    });
    it("+isOfValidDateTimeFormat - test all valid ionic date-time formats", () => {
        const validDateTime: string[] = ["2025", "2023-11-16T08:07:00", "2023-11-16T08:07", "2023-11-16T08:07:00Z", "08:07"];
        expect(validDateTime.map(datetime => DateTimeUtils.isOfValidDateTimeFormat(datetime))).toEqual(validDateTime.map(_el => true));
    });
});
