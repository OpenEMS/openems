import { subSeconds } from "date-fns";
import { DATE_TIME_REGEX, DateTimeUtils } from "./datetime-utils";

describe("DateTimeUtils", () => {

    const timeZone = "Europe/Berlin";

    it("+isDifferenceInSecondsGreaterThan - expected true", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date = subSeconds(new Date(), 11);
        expect(DATE_TIME_UTILS.IS_DIFFERENCE_IN_SECONDS_GREATER_THAN(10, currDate, lastUpdate)).toEqual(true);
    });
    it("+isDifferenceInSecondsGreaterThan - expected false", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date = subSeconds(new Date(), 9);
        expect(DATE_TIME_UTILS.IS_DIFFERENCE_IN_SECONDS_GREATER_THAN(10, currDate, lastUpdate)).toEqual(false);
    });
    it("+isDifferenceInSecondsGreaterThan - invalid Dates", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date | null = null;
        expect(DATE_TIME_UTILS.IS_DIFFERENCE_IN_SECONDS_GREATER_THAN(10, currDate, lastUpdate)).toEqual(false);
    });
    it("+toISO8601WithOffsetFormat - invalid Datetime string", () => {
        const inValidDateTime: string | null = null;
        expect(() => DATE_TIME_UTILS.FORMAT_TO_ISOZONED_DATE_TIME(inValidDateTime, timeZone)).toThrow(new Error(DateTimeUtils.INVALID_DATE_TIME_STRING));
    });
    it("+toISO8601WithOffsetFormat - valid Datetime string", () => {
        const validDateTime: string | null = "2023-11-16T08:07:00";
        expect(DATE_TIME_UTILS.FORMAT_TO_ISOZONED_DATE_TIME(validDateTime, timeZone)).toMatch(DATE_TIME_REGEX);
    });
    it("+isOfValidDateTimeFormat - test all valid ionic date-time formats", () => {
        const validDateTime: string[] = ["2025", "2023-11-16T08:07:00", "2023-11-16T08:07", "2023-11-16T08:07:00Z", "08:07"];
        expect(VALID_DATE_TIME.MAP(datetime => DATE_TIME_UTILS.IS_OF_VALID_DATE_TIME_FORMAT(datetime))).toEqual(VALID_DATE_TIME.MAP(_el => true));
    });
});
