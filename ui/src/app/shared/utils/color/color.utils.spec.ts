// @ts-strict-ignore
import { ColorUtils } from "./color.utils";

describe('Color-Utils', () => {
  it('#rgbStringToRgba', () => {
    expect(ColorUtils.rgbStringToRGBA('rgb(0,0,0)', 1)).toBe('rgba(0,0,0,1)');
    expect(ColorUtils.rgbStringToRGBA('rgb(0,0,0)', null)).toEqual('rgba(0,0,0,0)');
    expect(ColorUtils.rgbStringToRGBA(null, 1)).toEqual(null);
    expect(ColorUtils.rgbStringToRGBA(null, null)).toEqual(null);
  });

  it('#changeOpacityFromRGBA', () => {
    expect(ColorUtils.changeOpacityFromRGBA('rgba(0,0,0,0.05)', 1)).toBe('rgba(0,0,0,1)');
    expect(ColorUtils.changeOpacityFromRGBA('rgba(0,0,0,0.05)', null)).toBe('rgba(0,0,0,0)');
    expect(ColorUtils.changeOpacityFromRGBA(null, 1)).toBe(null);
    expect(ColorUtils.changeOpacityFromRGBA(null, null)).toBe(null);
  });
});
