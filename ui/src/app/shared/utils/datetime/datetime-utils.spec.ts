import { subSeconds } from "date-fns";
import { DateTimeUtils } from "./datetime-utils";

describe("DateTimeUtils", () => {
    it("#isDifferenceInSecondsGreaterThan - expected true", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date = subSeconds(new Date(), 11);
        expect(DateTimeUtils.isDifferenceInSecondsGreaterThan(10, currDate, lastUpdate)).toEqual(true);
    });
    it("#isDifferenceInSecondsGreaterThan - expected false", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date = subSeconds(new Date(), 9);
        expect(DateTimeUtils.isDifferenceInSecondsGreaterThan(10, currDate, lastUpdate)).toEqual(false);
    });
    it("#isDifferenceInSecondsGreaterThan - invalid Dates", () => {
        const currDate: Date = new Date();
        const lastUpdate: Date | null = null;
        expect(DateTimeUtils.isDifferenceInSecondsGreaterThan(10, currDate, lastUpdate)).toEqual(false);
    });
});
