import { ArrayUtils } from "./arrayutils";

describe('ArrayUtils', () => {

  it('#sortAlphabeticaly', () => {
    const inputArr = ['A', null, 'C', undefined, 'B', 'a', '1'];
    const sortedArr = ['1', 'A', 'a', 'B', 'C', null, undefined];

    expect(ArrayUtils.sortedAlphabetically(inputArr, a => a)).toEqual(sortedArr);
    expect(ArrayUtils.sortedAlphabetically(inputArr, _a => null)).toEqual(inputArr);

    expect(() => ArrayUtils.sortedAlphabetically(inputArr, null)).toThrow();
  });
});
