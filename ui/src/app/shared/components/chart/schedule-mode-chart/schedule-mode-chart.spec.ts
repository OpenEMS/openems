import { reflectComponentType } from "@angular/core";
import { Theme } from "src/app/edge/history/shared";
import { ModeChartComponent } from "src/app/edge/live/common/storage/new-navigation/chart/mode-chart";
import { createForecastPattern } from "./schedule-mode-chart";

describe("Storage mode chart", () => {
    it("exposes a standalone component definition", () => {
        const component = reflectComponentType(ModeChartComponent);

        expect(component).not.toBeNull();
        expect(component?.isStandalone).toBeTrue();
        expect(component?.selector).toBe("oe-common-storage-mode-chart");
    });
});

describe("Schedule mode forecast hatching", () => {
    let previousTheme: string | null;

    beforeEach(() => {
        previousTheme = document.documentElement.getAttribute("data-theme");
        document.documentElement.setAttribute("data-theme", Theme.DARK);
    });

    afterEach(() => {
        if (previousTheme == null) {
            document.documentElement.removeAttribute("data-theme");
        } else {
            document.documentElement.setAttribute("data-theme", previousTheme);
        }
    });

    it("shouldUseFivePixelModeBandsAndTwoPixelGapsHorizontally", () => {
        const canvas = document.createElement("canvas");
        canvas.width = 28;
        canvas.height = 8;
        const context = canvas.getContext("2d");
        if (context == null) {
            throw new Error("Could not create forecast pattern test canvas");
        }

        context.fillStyle = createForecastPattern("rgb(100, 150, 200)", "rgb(0, 0, 0)");
        context.fillRect(0, 0, canvas.width, canvas.height);

        for (let x = 0; x < canvas.width; x++) {
            const pixel = context.getImageData(x, 2, 1, 1).data;
            expect(Array.from(pixel))
                .withContext(`Unexpected stripe color at x=${x}`)
                .toEqual(x % 7 < 5 ? [100, 150, 200, 255] : [16, 24, 32, 255]);
        }
    });

    it("shouldContinueDiagonalStripesAcrossTileEdges", () => {
        const canvas = document.createElement("canvas");
        canvas.width = 64;
        canvas.height = 64;
        const context = canvas.getContext("2d");
        if (context == null) {
            throw new Error("Could not create forecast pattern test canvas");
        }

        context.fillStyle = createForecastPattern("rgb(100, 150, 200)", "rgb(0, 0, 0)");
        context.fillRect(0, 0, canvas.width, canvas.height);

        for (let x = 1; x < 56; x++) {
            for (const offset of [-1, 0, 1]) {
                const y = x + 7 + offset;
                const pixel = context.getImageData(x, y, 1, 1).data;
                expect(pixel[0])
                    .withContext(`Stripe interrupted at (${x}, ${y})`)
                    .toBeGreaterThan(offset === 0 ? 95 : 75);
            }
        }

        const gap = context.getImageData(3, 0, 1, 1).data;
        expect(Array.from(gap)).toEqual([16, 24, 32, 255]);

        const pixels = context.getImageData(0, 0, canvas.width, canvas.height).data;
        const modeColorPixels = pixels.filter((value, index) => index % 4 === 0 && value > 60).length;
        expect(modeColorPixels / (canvas.width * canvas.height)).toBeGreaterThan(0.5);
        expect(pixels.filter((_value, index) => index % 4 === 3).every((alpha) => alpha === 255)).toBeTrue();
    });

    it("shouldTintLightBackgroundWithModeColorWithoutTransparency", () => {
        document.documentElement.setAttribute("data-theme", Theme.LIGHT);
        const canvas = document.createElement("canvas");
        canvas.width = 16;
        canvas.height = 16;
        const context = canvas.getContext("2d");
        if (context == null) {
            throw new Error("Could not create forecast pattern test canvas");
        }

        context.fillStyle = createForecastPattern("rgb(100, 150, 200)", "rgb(255, 255, 255)");
        context.fillRect(0, 0, canvas.width, canvas.height);

        expect(Array.from(context.getImageData(3, 0, 1, 1).data)).toEqual([224, 234, 244, 255]);
    });

    it("shouldUseConfiguredDarkThemeInsteadOfInferringThemeFromBackgroundColor", () => {
        const canvas = document.createElement("canvas");
        canvas.width = 16;
        canvas.height = 16;
        const context = canvas.getContext("2d");
        if (context == null) {
            throw new Error("Could not create forecast pattern test canvas");
        }

        context.fillStyle = createForecastPattern("rgb(100, 150, 200)", "rgb(255, 255, 255)");
        context.fillRect(0, 0, canvas.width, canvas.height);

        expect(Array.from(context.getImageData(3, 0, 1, 1).data)).toEqual([179, 187, 195, 255]);
    });
});
