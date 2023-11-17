import { DateUtils } from "./dateutils";

describe('DateUtils', () => {

  const dates: Date[] = [
    new Date(Date.parse("2023-01-01")),
    new Date(Date.parse("2023-01-02"))
  ];

  it('#minDate - smallest date', () => {

    // valid params
    expect(DateUtils.minDate(...dates)).toEqual(dates[0]);

    // no params
    expect(DateUtils.minDate()).toEqual(null);

    // null as param
    expect(isNaN(DateUtils.minDate(null, null)?.getTime())).toBe(true);
  });

  it('#maxDate - biggest date', () => {
    // valid params
    expect(DateUtils.maxDate(...dates)).toEqual(dates[1]);

    // no params
    expect(DateUtils.maxDate()).toEqual(null);

    // null as param
    expect(isNaN(DateUtils.maxDate(null, null)?.getTime())).toBe(true);
  });

  it('#stringToDate - converts string to date', () => {
    expect(DateUtils.stringToDate('2023-01-02')).toEqual(new Date(Date.parse('2023-01-02')));
    expect(DateUtils.stringToDate('wrong format')).toEqual(null);
  });
});
