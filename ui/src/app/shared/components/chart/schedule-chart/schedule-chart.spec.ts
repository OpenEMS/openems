import { ScheduleChartComponent } from "./schedule-chart";

describe("ScheduleChartComponent.normalizeLines", () => {
    it("fills gaps with zero when switching between charge and discharge lines", () => {
        const result = ScheduleChartComponent.normalizePositiveNegativeLines([
            -10, -11, -12, 20, 21, 22, -30, -31, -32,
        ]);

        expect(result.positive).toEqual([
            null,
            null,
            0,
            20,
            21,
            22,
            null,
            null,
            null,
        ]);
        expect(result.negative).toEqual([
            10,
            11,
            12,
            null,
            null,
            0,
            30,
            31,
            32,
        ]);

        // TODO Even better would be this, but it leads to overlapping chart lines
        // expect(result.positive).toEqual([10, 11, 12, 0, null, 0, 30, 31, 32]);
        // expect(result.negative).toEqual([null, null, 0, 20, 21, 22, 0, null, null]);
    });
});
