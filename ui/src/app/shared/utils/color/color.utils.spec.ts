import { ColorUtils } from "./color.utils";

describe("Color-Utils", () => {
    it("#rgbStringToRgba", () => {
        expect(ColorUtils.rgbStringToRgba("rgb(0,0,0)", 1)).toBe("rgba(0,0,0,1)");
    });

    it("#changeOpacityFromRGBA", () => {
        expect(ColorUtils.changeOpacityFromRGBA("rgba(0,0,0,0.05)", 1)).toBe("rgba(0,0,0,1)");
    });
});
