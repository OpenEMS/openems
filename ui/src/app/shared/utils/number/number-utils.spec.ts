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

});
