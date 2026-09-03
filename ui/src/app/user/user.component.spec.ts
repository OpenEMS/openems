// @ts-strict-ignore
import { Theme } from "src/environments";

describe("UserComponent - isThemeSelectionAvailable", () => {

    function isThemeSelectionAvailable(theme: Theme): boolean {
        return (["OpenEMS", "FENECON", "FENECONBeta"] as string[]).includes(theme);
    }

    it("should return true for OpenEMS theme", () => {
        expect(isThemeSelectionAvailable("OpenEMS")).toBe(true);
    });

    it("should return true for FENECON theme", () => {
        expect(isThemeSelectionAvailable("FENECON")).toBe(true);
    });

    it("should return true for FENECONBeta theme", () => {
        expect(isThemeSelectionAvailable("FENECONBeta")).toBe(true);
    });

    it("should return false for Heckert theme", () => {
        expect(isThemeSelectionAvailable("Heckert")).toBe(false);
    });
});
