// @ts-strict-ignore
import { ArrayUtils } from "./array.utils";

describe('Array-Utils', () => {
  it('#findSmallestNumber', () => {
    expect(ArrayUtils.findSmallestNumber([])).toEqual(null);
    expect(ArrayUtils.findSmallestNumber([null, null])).toEqual(null);
    expect(ArrayUtils.findSmallestNumber([0, -1])).toEqual(-1);
    expect(ArrayUtils.findSmallestNumber([null, undefined])).toEqual(null);
  });
  it('#findBiggestNumber', () => {
    expect(ArrayUtils.findBiggestNumber([])).toEqual(null);
    expect(ArrayUtils.findBiggestNumber([null, null])).toEqual(null);
    expect(ArrayUtils.findBiggestNumber([0, -1])).toEqual(0);
    expect(ArrayUtils.findBiggestNumber([null, undefined])).toEqual(null);
  });

  it('#sortAlphabeticaly', () => {
    const inputArr = ['A', null, 'C', undefined, 'B', 'a', '1'];
    const sortedArr = ['1', 'A', 'a', 'B', 'C', null, undefined];

    expect(ArrayUtils.sortedAlphabetically(inputArr, a => a)).toEqual(sortedArr);
    expect(ArrayUtils.sortedAlphabetically(inputArr, _a => null)).toEqual(inputArr);

    expect(() => ArrayUtils.sortedAlphabetically(inputArr, null)).toThrow();
  });
});
