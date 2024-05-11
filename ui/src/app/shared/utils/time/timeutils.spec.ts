// @ts-strict-ignore
import { TimeUtils } from "./timeutils";

describe('TimeUtils', () => {
  it('#formatSecondsToDuration', () => {
    expect(TimeUtils.formatSecondsToDuration(12000, 'de')).toEqual("3h 20m");
    expect(TimeUtils.formatSecondsToDuration(null, 'de')).toEqual(null);
    expect(TimeUtils.formatSecondsToDuration(undefined, 'de')).toEqual(null);
    expect(TimeUtils.formatSecondsToDuration(12000, null)).toEqual("3h 20m");
  });
});
