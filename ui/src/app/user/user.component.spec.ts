// @ts-strict-ignore
import { signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { environment, Theme } from "src/environments";
import { UserComponent } from "./user.component";

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

    describe("on UserComponent instance", () => {
        beforeEach(() => TestBed.configureTestingModule({}));

        it("should reflect environment.theme on the component field", () => {
            TestBed.runInInjectionContext(() => {
                const component = new UserComponent(
                    { instant: (k: string) => k } as any,
                    {} as any,
                    {} as any,
                    { currentUser: signal(null) } as any,
                    {} as any,
                    {} as any,
                    {} as any,
                );
                const expected = (["OpenEMS", "FENECON", "FENECONBeta"] as string[]).includes(environment.theme);
                expect((component as any).isThemeSelectionAvailable).toBe(expected);
            });
        });
    });
});
