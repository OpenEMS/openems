import { TimedisplayPipe } from "./TIMEDISPLAY.PIPE";

describe("TimeDisplayPipe", () => {
    let pipe: TimedisplayPipe;

    beforeEach(() => {
        pipe = new TimedisplayPipe();
    });

    //  Valid ISO 8601 date-time string
    it("should extract HH:mm from a valid ISO 8601 string", () => {
        const isoString = "2025-07-14T14:30:00.000Z";
        expect(PIPE.TRANSFORM(isoString)).toBe("14:30");
    });

    //  Valid ISO 8601 date-time string with different time
    it("should extract correct HH:mm from another ISO 8601 string", () => {
        const isoString = "2023-01-01T08:05:10.123Z";
        expect(PIPE.TRANSFORM(isoString)).toBe("08:05");
    });

    //  Valid plain time string (HH:mm)
    it("should return a plain HH:mm string as is", () => {
        const plainTime = "10:45";
        expect(PIPE.TRANSFORM(plainTime)).toBe("10:45");
    });

    //  Another valid plain time string (HH:mm)
    it("should return another plain HH:mm string as is", () => {
        const plainTime = "23:59";
        expect(PIPE.TRANSFORM(plainTime)).toBe("23:59");
    });

    //  Null input
    it("should return '00:00' for null input", () => {
        expect(PIPE.TRANSFORM(null)).toBe("00:00");
    });

    //  ISO 8601 string with time at midnight
    it("should correctly handle midnight from an ISO 8601 string", () => {
        const isoString = "2025-07-14T00:00:00.000Z";
        expect(PIPE.TRANSFORM(isoString)).toBe("00:00");
    });

    //  Plain time string at midnight
    it("should correctly handle midnight from a plain time string", () => {
        const plainTime = "00:00";
        expect(PIPE.TRANSFORM(plainTime)).toBe("00:00");
    });

    //  ISO 8601 string with time at noon
    it("should correctly handle noon from an ISO 8601 string", () => {
        const isoString = "2025-07-14T12:00:00.000Z";
        expect(PIPE.TRANSFORM(isoString)).toBe("12:00");
    });

});
