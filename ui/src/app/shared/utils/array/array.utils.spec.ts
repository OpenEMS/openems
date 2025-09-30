// @ts-strict-ignore
import { ArrayUtils } from "./ARRAY.UTILS";

describe("Array-Utils", () => {
  it("#findSmallestNumber", () => {
    expect(ARRAY_UTILS.FIND_SMALLEST_NUMBER([])).toEqual(null);
    expect(ARRAY_UTILS.FIND_SMALLEST_NUMBER([null, null])).toEqual(null);
    expect(ARRAY_UTILS.FIND_SMALLEST_NUMBER([0, -1])).toEqual(-1);
    expect(ARRAY_UTILS.FIND_SMALLEST_NUMBER([null, undefined])).toEqual(null);
  });
  it("#findBiggestNumber", () => {
    expect(ARRAY_UTILS.FIND_BIGGEST_NUMBER([])).toEqual(null);
    expect(ARRAY_UTILS.FIND_BIGGEST_NUMBER([null, null])).toEqual(null);
    expect(ARRAY_UTILS.FIND_BIGGEST_NUMBER([0, -1])).toEqual(0);
    expect(ARRAY_UTILS.FIND_BIGGEST_NUMBER([null, undefined])).toEqual(null);
  });

  it("#sortAlphabeticaly", () => {
    const inputArr = ["A", null, "C", undefined, "B", "a", "1"];
    const sortedArr = ["1", "A", "a", "B", "C", null, undefined];

    expect(ARRAY_UTILS.SORTED_ALPHABETICALLY(inputArr, a => a)).toEqual(sortedArr);
    expect(ARRAY_UTILS.SORTED_ALPHABETICALLY(inputArr, _a => null)).toEqual(inputArr);

    expect(() => ARRAY_UTILS.SORTED_ALPHABETICALLY(inputArr, null)).toThrow();
  });

  it("#sum", () => {
    // Standard summation with positive integers
    expect([1, 2, 3, 4].reduce(ARRAY_UTILS.REDUCER_FUNCTIONS.SUM, 0)).toEqual(10);

    // Summation with negative numbers
    expect([10, -2, -5, 3].reduce(ARRAY_UTILS.REDUCER_FUNCTIONS.SUM, 0)).toEqual(6);

    // An empty array should return the initial value (0)
    expect([].reduce(ARRAY_UTILS.REDUCER_FUNCTIONS.SUM, 0)).toEqual(0);

    // Summation with floating-point numbers
    expect([1.5, 2.5, 1.0].reduce(ARRAY_UTILS.REDUCER_FUNCTIONS.SUM, 0)).toEqual(5);
  });
});
