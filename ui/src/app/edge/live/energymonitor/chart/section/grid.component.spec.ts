import { registerLocaleData } from "@angular/common";
import localeDe from "@angular/common/locales/de";
import localeDeExtra from "@angular/common/locales/extra/de";
import { Subject } from "rxjs";
import { GridMode } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { SvgImagePosition, SvgSquare, SvgSquarePosition, SvgTextPosition } from "./abstractsection.component";
import { GridSectionComponent } from "./grid.component";

function makeMockSummary(gridBuyPrice: number | null, buyActivePower = 0): DefaultTypes.Summary {
    return {
        grid: {
            powerRatio: 0,
            activePowerL1: 0, activePowerL2: 0, activePowerL3: 0,
            buyActivePower,
            maxBuyActivePower: 0,
            sellActivePower: 0,
            sellActivePowerL1: 0, sellActivePowerL2: 0, sellActivePowerL3: 0,
            maxSellActivePower: 0,
            gridMode: GridMode.ON_GRID,
            restrictionMode: 0,
            gridBuyPrice,
        },
        system: { totalPower: 0 } as any,
        storage: {} as any,
        production: {} as any,
        consumption: {} as any,
    } as DefaultTypes.Summary;
}

function makeComponent(): GridSectionComponent {
    const mockConfig = {
        getComponent: () => ({}),
        getPropertyFromComponent: () => "EUR",
        widgets: { classes: [] },
    };
    const component = new GridSectionComponent(
        { instant: (k: string) => k } as any,
        { getConfig: () => Promise.resolve(mockConfig) } as any,
        { position: () => null } as any,
        {} as any,
        {} as any,
        { transform: (_v: number, _u: string) => "0 kW" } as any,
        { toggleAnimation$: new Subject<boolean>() } as any,
    );

    // Simulate the square state that updateOnWindowResize would set up.
    (component as any).square = new SvgSquare(
        84,
        new SvgTextPosition(42, 48, 30),
        new SvgImagePosition("", 16, 52, 48),
    );
    (component as any).squarePosition = new SvgSquarePosition(-115, -42);
    (component as any).innerRadius = 120;
    (component as any).isEnabled = true;

    return component;
}

describe("GridSectionComponent – grid buy price text offset", () => {
    beforeAll(() => registerLocaleData(localeDe, "de", localeDeExtra));

    it("shifts both texts up by 25 px when price first appears", async () => {
        const component = makeComponent();
        const originalY = (component as any).square.valueText.y as number;

        await component._updateCurrentData(makeMockSummary(0.05));

        expect((component as any).square.valueText.y).toBe(originalY - 25);
        expect((component as any).priceOffsetApplied).toBeTrue();
        expect((component as any).savedValueTextY).toBe(originalY);
    });

    it("recalculates gridBuyPrice.yPosition from the shifted y", async () => {
        const component = makeComponent();
        const iconY = (component as any).square.image.y as number;

        await component._updateCurrentData(makeMockSummary(0.05));

        const priceY = (component as any).gridBuyPrice?.yPosition as number;
        expect(priceY).toBeLessThan(iconY);
    });

    it("does not shift again on subsequent calls when price stays", async () => {
        const component = makeComponent();
        await component._updateCurrentData(makeMockSummary(0.05));
        const yAfterFirst = (component as any).square.valueText.y as number;

        await component._updateCurrentData(makeMockSummary(0.05));

        expect((component as any).square.valueText.y).toBe(yAfterFirst);
    });

    it("restores original y when price disappears", async () => {
        const component = makeComponent();
        const originalY = (component as any).square.valueText.y as number;

        await component._updateCurrentData(makeMockSummary(0.05));
        await component._updateCurrentData(makeMockSummary(null));

        expect((component as any).square.valueText.y).toBe(originalY);
        expect((component as any).priceOffsetApplied).toBeFalse();
        expect((component as any).savedValueTextY).toBeNull();
    });

    it("re-applies offset after setElementHeight resets state (window resize)", async () => {
        const component = makeComponent();
        await component._updateCurrentData(makeMockSummary(0.05));

        // Window resize triggers setElementHeight which resets the offset tracking.
        (component as any).setElementHeight();
        expect((component as any).priceOffsetApplied).toBeFalse();
        expect((component as any).savedValueTextY).toBeNull();

        const yAfterReset = (component as any).square.valueText.y as number;
        await component._updateCurrentData(makeMockSummary(0.05));

        expect((component as any).square.valueText.y).toBe(yAfterReset - 25);
    });

    it("does not shift texts when no price is available", async () => {
        const component = makeComponent();
        const originalY = (component as any).square.valueText.y as number;

        await component._updateCurrentData(makeMockSummary(null));

        expect((component as any).square.valueText.y).toBe(originalY);
        expect((component as any).priceOffsetApplied).toBeFalse();
    });
});
