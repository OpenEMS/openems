// @ts-strict-ignore
import { ColorUtils } from "./color.utils";

describe("Color-Utils", () => {
    it("#rgbStringToRgba", () => {
        expect(ColorUtils.rgbStringToRgba("rgb(0,0,0)", 1)).toBe("rgba(0,0,0,1)");
        expect(() => ColorUtils.rgbStringToRgba("rgb(0,0,0)", null)).toThrow(new Error("All values need to be valid"));
        expect(() => ColorUtils.rgbStringToRgba(null, 1)).toThrow(new Error("Passed value is not of type string"));
        expect(() => ColorUtils.rgbStringToRgba(null, null)).toThrow(new Error("Passed value is not of type string"));
    });

    it("#changeOpacityFromRGBA", () => {
        expect(ColorUtils.changeOpacityFromRGBA("rgba(0,0,0,0.05)", 1)).toBe("rgba(0,0,0,1)");
        expect(() => ColorUtils.changeOpacityFromRGBA("rgba(0,0,0,0.05)", null)).toThrow(new Error("All values need to be valid"));
        expect(() => ColorUtils.changeOpacityFromRGBA(null, 1)).toThrow(new Error("Passed value is not of type string"));
        expect(() => ColorUtils.changeOpacityFromRGBA(null, null)).toThrow(new Error("Passed value is not of type string"));
    });
});
