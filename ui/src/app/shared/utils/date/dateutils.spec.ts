// @ts-strict-ignore
import { DateUtils } from "./dateutils";

describe("DateUtils", () => {

  const dates: Date[] = [
    new Date(DATE.PARSE("2023-01-01")),
    new Date(DATE.PARSE("2023-01-02")),
  ];

  it("#minDate - smallest date", () => {

    // valid params
    expect(DATE_UTILS.MIN_DATE(...dates)).toEqual(dates[0]);

    // no params
    expect(DATE_UTILS.MIN_DATE()).toEqual(null);

    // null as param
    expect(isNaN(DATE_UTILS.MIN_DATE(null, null)?.getTime())).toBe(true);
  });

  it("#maxDate - biggest date", () => {
    // valid params
    expect(DATE_UTILS.MAX_DATE(...dates)).toEqual(dates[1]);

    // no params
    expect(DATE_UTILS.MAX_DATE()).toEqual(null);

    // null as param
    expect(isNaN(DATE_UTILS.MAX_DATE(null, null)?.getTime())).toBe(true);
  });

  it("#stringToDate - converts string to date", () => {
    expect(DATE_UTILS.STRING_TO_DATE("2023-01-02")).toEqual(new Date(DATE.PARSE("2023-01-02")));
    expect(DATE_UTILS.STRING_TO_DATE("wrong format")).toEqual(null);
  });

  it("#isDateBefore - checks if given date is before date to be compared to", () => {
    const date: Date = DATE_UTILS.STRING_TO_DATE("2023-01-01") as Date;
    expect(DATE_UTILS.IS_DATE_BEFORE(date, DATE_UTILS.STRING_TO_DATE("2023-01-31"))).toEqual(true);
    expect(DATE_UTILS.IS_DATE_BEFORE(date, DATE_UTILS.STRING_TO_DATE("2022-12-31"))).toEqual(false);
    expect(DATE_UTILS.IS_DATE_BEFORE(date, DATE_UTILS.STRING_TO_DATE("2023-01-01"))).toEqual(false);
    expect(DATE_UTILS.IS_DATE_BEFORE(date, null)).toEqual(false);
    expect(DATE_UTILS.IS_DATE_BEFORE(null, DATE_UTILS.STRING_TO_DATE("2023-01-01"))).toEqual(false);
  });

  describe("#formatQuarterDateRange", () => {

    // Valid dates for Quarter
    it("should correctly format dates with standard format", () => {
      const startDate = new Date(2024, 0, 1); // January 1, 2024
      const endDate = new Date(2024, 2, 31); // March 31, 2024
      const dateFormat = "DD.MM.YYYY";
      const expected = "01.01.2024 - 31.03.2024";
      expect(DATE_UTILS.FORMAT_QUARTER_DATE_RANGE(startDate, endDate, dateFormat)).toBe(expected);
    });

    // Null end date
    it("should return null if Date is null", () => {
      const startDate = new Date(2024, 0, 1);
      const dateFormat = "DD.MM.YYYY";
      expect(DATE_UTILS.FORMAT_QUARTER_DATE_RANGE(startDate, null!, dateFormat)).toBeNull();
      expect(DATE_UTILS.FORMAT_QUARTER_DATE_RANGE(null!, null!, dateFormat)).toBeNull();
    });

  });
});
