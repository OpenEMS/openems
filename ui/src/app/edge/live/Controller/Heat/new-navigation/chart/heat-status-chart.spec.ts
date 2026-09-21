import { MIN_HEATING_POWER_IN_W, resolveIsHeating } from "./heat-status-chart";

describe("resolveIsHeating", () => {
    it("returns true when managedConsumption is greater than zero", () => {
        const result = resolveIsHeating(100);

        expect(result).toBeTrue();
    });

    it("returns false when managedConsumption is zero", () => {
        const result = resolveIsHeating(0);

        expect(result).toBeFalse();
    });

    it("returns false when managedConsumption is below the minimum heating threshold", () => {
        const result = resolveIsHeating(MIN_HEATING_POWER_IN_W - 1);

        expect(result).toBeFalse();
    });

    it("returns true when managedConsumption is exactly the minimum heating threshold", () => {
        const result = resolveIsHeating(MIN_HEATING_POWER_IN_W);

        expect(result).toBeTrue();
    });

    it("returns null when no ESH data is available", () => {
        const result = resolveIsHeating(null);

        expect(result).toBeNull();
    });
});
