import { Utils } from "./utils";

describe('Utils', () => {

  it('#subtractSafely', () => {
    expect(Utils.subtractSafely(null, null)).toEqual(null);
    expect(Utils.subtractSafely(null, undefined)).toEqual(null);
    expect(Utils.subtractSafely(0, null)).toEqual(0);
    expect(Utils.subtractSafely(1, 1)).toEqual(0);
    expect(Utils.subtractSafely(1, 2)).toEqual(-1);
    expect(Utils.subtractSafely(1)).toEqual(1);
  });
});
