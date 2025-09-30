// @ts-strict-ignore
import { RGBColor } from "./defaulttypes";

describe("Defaulttypes", () => {

    it("#RGB_COLOR.TO_STRING()", () => {
        const black = new RGBColor(0, 0, 0);
        expect(BLACK.TO_STRING()).toEqual("rgb(0,0,0)");
    });

    it("#RGB_COLOR.TO_STRING() - invalid values", () => {
        const allInvalid = new RGBColor(null, null, null);
        expect(() => ALL_INVALID.TO_STRING()).toThrow(Error("All values need to be valid"));
        const oneInvalid = new RGBColor(0, 0, null);
        expect(() => ONE_INVALID.TO_STRING()).toThrow(Error("All values need to be valid"));
    });
});
