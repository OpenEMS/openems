// @ts-strict-ignore
import { ColorUtils } from "./COLOR.UTILS";

describe("Color-Utils", () => {
  it("#rgbStringToRgba", () => {
    expect(COLOR_UTILS.RGB_STRING_TO_RGBA("rgb(0,0,0)", 1)).toBe("rgba(0,0,0,1)");
    expect(() => COLOR_UTILS.RGB_STRING_TO_RGBA("rgb(0,0,0)", null)).toThrow(new Error("All values need to be valid"));
    expect(() => COLOR_UTILS.RGB_STRING_TO_RGBA(null, 1)).toThrow(new Error("Passed value is not of type string"));
    expect(() => COLOR_UTILS.RGB_STRING_TO_RGBA(null, null)).toThrow(new Error("Passed value is not of type string"));
  });

  it("#changeOpacityFromRGBA", () => {
    expect(COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA("rgba(0,0,0,0.05)", 1)).toBe("rgba(0,0,0,1)");
    expect(() => COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA("rgba(0,0,0,0.05)", null)).toThrow(new Error("All values need to be valid"));
    expect(() => COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(null, 1)).toThrow(new Error("Passed value is not of type string"));
    expect(() => COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(null, null)).toThrow(new Error("Passed value is not of type string"));
  });
});
