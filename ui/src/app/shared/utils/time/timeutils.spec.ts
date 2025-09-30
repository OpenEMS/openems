// @ts-strict-ignore
import { TimeUtils } from "./timeutils";

describe("TimeUtils", () => {
  it("#formatSecondsToDuration", () => {
    expect(TIME_UTILS.FORMAT_SECONDS_TO_DURATION(12000, "de")).toEqual("3h 20m");
    expect(TIME_UTILS.FORMAT_SECONDS_TO_DURATION(null, "de")).toEqual(null);
    expect(TIME_UTILS.FORMAT_SECONDS_TO_DURATION(undefined, "de")).toEqual(null);
    expect(TIME_UTILS.FORMAT_SECONDS_TO_DURATION(12000, null)).toEqual("3h 20m");
  });
});
