// @ts-strict-ignore
import { RGBColor } from "./defaulttypes";

describe("Defaulttypes", () => {

    it("#RgbColor.toString()", () => {
        const black = new RGBColor(0, 0, 0);
        expect(black.toString()).toEqual("rgb(0,0,0)");
    });

    it("#RgbColor.toString() - invalid values", () => {
        const allInvalid = new RGBColor(null, null, null);
        expect(() => allInvalid.toString()).toThrow(Error("All values need to be valid"));
        const oneInvalid = new RGBColor(0, 0, null);
        expect(() => oneInvalid.toString()).toThrow(Error("All values need to be valid"));
    });
});
