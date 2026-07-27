import { NumberUtils } from "./number-utils";

describe("NumberUtils", () => {
    describe("convertNumberToBeAtMost", () => {
        it("should return null if value is null", () => {
            const result = NumberUtils.convertNumberToBeAtMost(null, 10);
            expect(result).toBeNull();
        });

        it("should return the value if it is less than atMost", () => {
            const result = NumberUtils.convertNumberToBeAtMost(5, 10);
            expect(result).toBe(5);
        });

        it("should return the value if it is equal to atMost", () => {
            const result = NumberUtils.convertNumberToBeAtMost(10, 10);
            expect(result).toBe(10);
        });

        it("should return atMost if value is greater than atMost", () => {
            const result = NumberUtils.convertNumberToBeAtMost(15, 10);
            expect(result).toBe(10);
        });

        it("should work with negative numbers", () => {
            const result = NumberUtils.convertNumberToBeAtMost(-5, -10);
            expect(result).toBe(-10);
        });

        it("should handle zero correctly", () => {
            const result = NumberUtils.convertNumberToBeAtMost(0, 10);
            expect(result).toBe(0);
        });
    });

    describe("isPresentNumber", () => {
        it("should return true for finite numbers", () => {
            expect(NumberUtils.isPresentNumber(0)).toBeTrue();
            expect(NumberUtils.isPresentNumber(-1)).toBeTrue();
            expect(NumberUtils.isPresentNumber(12.34)).toBeTrue();
        });

        it("should return false for NaN", () => {
            expect(NumberUtils.isPresentNumber(Number.NaN)).toBeFalse();
        });

        it("should return false for infinity values", () => {
            expect(NumberUtils.isPresentNumber(Number.POSITIVE_INFINITY)).toBeFalse();
            expect(NumberUtils.isPresentNumber(Number.NEGATIVE_INFINITY)).toBeFalse();
        });

        it("should return false for null and undefined", () => {
            expect(NumberUtils.isPresentNumber(null)).toBeFalse();
            expect(NumberUtils.isPresentNumber(undefined)).toBeFalse();
        });

        it("should return false for non-number values", () => {
            expect(NumberUtils.isPresentNumber("1")).toBeFalse();
            expect(NumberUtils.isPresentNumber({})).toBeFalse();
            expect(NumberUtils.isPresentNumber([])).toBeFalse();
            expect(NumberUtils.isPresentNumber(true)).toBeFalse();
        });
    });

    describe("NumberUtils.multiplySafely", () => {
        it("should multiply numbers correctly", () => {
            const result = NumberUtils.multiplySafely(2, 3, 4);
            expect(result).toBe(24);
        });

        it("should return null if one value is null", () => {
            const result = NumberUtils.multiplySafely(null, 3, 4);
            expect(result).toBeNull();
        });

        it("should return null if multiple values are null", () => {
            const result = NumberUtils.multiplySafely(null, null, 4);
            expect(result).toBeNull();
        });

        it("should return null if all values are null", () => {
            const result = NumberUtils.multiplySafely(null, null);
            expect(result).toBeNull();
        });

        it("should handle zero correctly", () => {
            const result = NumberUtils.multiplySafely(0, 5);
            expect(result).toBe(0);
        });

        it("should return null for null and zero combination", () => {
            const result = NumberUtils.multiplySafely(null, 0);
            expect(result).toBeNull();
        });
    });

    describe("numberToBooleanOrElse", () => {
        it("should return false for 0", () => {
            expect(NumberUtils.numberToBooleanOrElse(0, true)).toBeFalse();
        });

        it("should return true for 1", () => {
            expect(NumberUtils.numberToBooleanOrElse(1, false)).toBeTrue();
        });

        it("should return orElse for values other than 0 or 1", () => {
            expect(NumberUtils.numberToBooleanOrElse(2 as 0 | 1, false)).toBeFalse();
            expect(NumberUtils.numberToBooleanOrElse(2 as 0 | 1, true)).toBeTrue();
        });
    });
});
