import { PickDateTimeRangeComponent } from "./pick-date-time-range";

describe("#isValidTime", () => {
    const timeRange = new PickDateTimeRangeComponent();
    const validTimes = ["00:00", "12:34", "23:59"];
    const invalidTimes = ["24:00", "12:60", "-1:23", "12345", null, undefined, ""];

    validTimes.forEach(time => {
        it(`should return true for valid time: ${time}`, () => {
            expect(timeRange.isValidTime(time)).toBeTrue();
        });
    });

    invalidTimes.forEach(time => {
        it(`should return false for invalid time: ${time}`, () => {
            expect(timeRange.isValidTime(time)).toBeFalse();
        });
    });
});
