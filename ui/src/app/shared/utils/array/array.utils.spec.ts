import { ArrayUtils } from "./array.utils";

describe("Array-Utils", () => {
    it("+findSmallestNumber", () => {
        expect(ArrayUtils.findSmallestNumber([])).toEqual(null);
        expect(ArrayUtils.findSmallestNumber([null, null])).toEqual(null);
        expect(ArrayUtils.findSmallestNumber([0, -1])).toEqual(-1);
        expect(ArrayUtils.findSmallestNumber([null, undefined])).toEqual(null);
    });
    it("+findBiggestNumber", () => {
        expect(ArrayUtils.findBiggestNumber([])).toEqual(null);
        expect(ArrayUtils.findBiggestNumber([null, null])).toEqual(null);
        expect(ArrayUtils.findBiggestNumber([0, -1])).toEqual(0);
        expect(ArrayUtils.findBiggestNumber([null, undefined])).toEqual(null);
    });

    it("+sortAlphabetically", () => {
        const inputArr = ["A", null, "C", undefined, "B", "a", "1"];
        const sortedArr = ["1", "A", "a", "B", "C", null, undefined];

        expect(ArrayUtils.sortAlphabetically(inputArr, (a) => a)).toEqual(sortedArr);
        expect(ArrayUtils.sortAlphabetically(inputArr, (_) => null)).toEqual(inputArr);
    });

    it("+sortByTrailingNumber", () => {
        const inputArr = [undefined, "component4", "component1", null, "component3", "component2"];
        const sortedArr = ["component1", "component2", "component3", "component4", null, undefined];
        const inputArr2 = ["2", "1", "3"];

        expect(ArrayUtils.sortByTrailingNumber(inputArr, (a) => a)).toEqual(sortedArr);
        expect(ArrayUtils.sortByTrailingNumber(inputArr2, (_) => null)).toEqual(inputArr2);
    });

    describe("ReducerFunctions", () => {
        it("+sum", () => {
            // Standard summation with positive integers
            expect([1, 2, 3, 4].reduce(ArrayUtils.ReducerFunctions.sum, 0)).toEqual(10);

            // Summation with negative numbers
            expect([10, -2, -5, 3].reduce(ArrayUtils.ReducerFunctions.sum, 0)).toEqual(6);

            // An empty array should return the initial value (0)
            expect([].reduce(ArrayUtils.ReducerFunctions.sum, 0)).toEqual(0);

            // Summation with floating-point numbers
            expect([1.5, 2.5, 1.0].reduce(ArrayUtils.ReducerFunctions.sum, 0)).toEqual(5);
        });
    });

    describe("+removeMatching", () => {
        it("only one array provided", () => {
            expect(ArrayUtils.removeMatching([])).toEqual(null);
        });
        it("empty arrays", () => {
            expect(ArrayUtils.removeMatching([], [])).toEqual([]);
        });
        it("input array & null array", () => {
            expect(ArrayUtils.removeMatching(["test1"], null)).toEqual(["test1"]);
        });
        it("input array null & array", () => {
            expect(ArrayUtils.removeMatching(null, ["test1"])).toEqual(null);
        });
        it("non overlapping arrays", () => {
            expect(ArrayUtils.removeMatching<string[]>(["test1"], ["test2"])).toEqual(["test1"]);
        });
        it("overlapping arrays", () => {
            expect(ArrayUtils.removeMatching<string[]>(["test1", "test2"], ["test1"])).toEqual(["test2"]);
        });
    });
});
