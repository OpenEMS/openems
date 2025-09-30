import { endOfMonth, endOfWeek, endOfYear, startOfDay, startOfMonth, startOfWeek, startOfYear, subDays, subMonths, subWeeks, subYears } from "date-fns";
import { DefaultTypes } from "../../type/defaulttypes";
import { TestContext, TestingUtils } from "../shared/testing/UTILS.SPEC";

import { PickDateComponent } from "./PICKDATE.COMPONENT";

export function expectPreviousPeriod(testContext: TestContext, firstSetupProtocol: Date, expectToBe: boolean): void {
  expect(PICK_DATE_COMPONENT.IS_PREVIOUS_PERIOD_ALLOWED(TEST_CONTEXT.SERVICE, firstSetupProtocol)).toBe(expectToBe);
}

export function expectNextPeriod(testContext: TestContext, expectToBe: boolean): void {
  expect(PICK_DATE_COMPONENT.IS_NEXT_PERIOD_ALLOWED(TEST_CONTEXT.SERVICE)).toBe(expectToBe);
}

describe("Pickdate", () => {

  let TEST_CONTEXT: TestContext;
  beforeEach(async () =>
    TEST_CONTEXT = await TESTING_UTILS.SHARED_SETUP(),
  );

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Day-View: firstSetupProtocol = today", () => {
    const firstSetupProtocol = new Date();
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Day-View: firstSetupProtocol = yesterday", () => {
    const firstSetupProtocol = startOfDay(subDays(new Date(), 1));
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = current week", () => {
    const firstSetupProtocol = new Date();
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = Start of previous week, current period = current week", () => {
    const firstSetupProtocol = startOfWeek(subWeeks(new Date(), 1), { weekStartsOn: 1 });
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = Today, current period = previous week", () => {
    const firstSetupProtocol = new Date();
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  const previousWeekPeriod = new DEFAULT_TYPES.HISTORY_PERIOD(startOfWeek(subWeeks(new Date(), 1), { weekStartsOn: 1 }), endOfWeek(subWeeks(new Date(), 1), { weekStartsOn: 1 }));
  const currentWeekPeriod = new DEFAULT_TYPES.HISTORY_PERIOD(startOfWeek(new Date(), { weekStartsOn: 1 }), endOfWeek(new Date(), { weekStartsOn: 1 }));

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = previous week, current period = previous week", () => {
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(previousWeekPeriod);
    const firstSetupProtocol = startOfWeek(subWeeks(new Date(), 1), { weekStartsOn: 1 });
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = 2 weeks ago, current period = previous week", () => {
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(previousWeekPeriod);
    const firstSetupProtocol = startOfWeek(subWeeks(new Date(), 2), { weekStartsOn: 1 });

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = 2 weeks ago, current period = current week", () => {
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(currentWeekPeriod);
    const firstSetupProtocol = startOfWeek(subWeeks(new Date(), 2), { weekStartsOn: 1 });

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  const previousMonthPeriod = new DEFAULT_TYPES.HISTORY_PERIOD(startOfMonth(subMonths(new Date(), 1)), endOfMonth(subMonths(new Date(), 1)));
  const currentMonthPeriod = new DEFAULT_TYPES.HISTORY_PERIOD(startOfMonth(new Date()), endOfMonth(new Date()));

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Month-View: firstSetupProtocol = today, current period = current month", () => {
    const firstSetupProtocol = new Date();
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(currentMonthPeriod);
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });
  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Month-View: firstSetupProtocol = start of current month, current period = previous month", () => {
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(previousMonthPeriod);
    const firstSetupProtocol = startOfMonth(subMonths(new Date(), 1));
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Month-View: firstSetupProtocol = 2 months ago, current period = previous month", () => {
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(previousMonthPeriod);
    const firstSetupProtocol = startOfMonth(subMonths(new Date(), 2));
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  const previousYearPeriod = new DEFAULT_TYPES.HISTORY_PERIOD(startOfYear(subYears(new Date(), 1)), endOfYear(subYears(new Date(), 1)));
  const currentYearPeriod = new DEFAULT_TYPES.HISTORY_PERIOD(startOfYear(new Date()), endOfYear(new Date()));

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Year-View: firstSetupProtocol = today, current period = current year", () => {
    const firstSetupProtocol = new Date();
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(currentYearPeriod);
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Year-View: firstSetupProtocol = previous year, current period = previous year", () => {
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(previousYearPeriod);
    const firstSetupProtocol = startOfYear(subYears(new Date(), 1));
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Year-View: firstSetupProtocol = 2 years ago, current period = previous year", () => {
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(previousYearPeriod);
    const firstSetupProtocol = startOfYear(subYears(new Date(), 2));

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Year-View: firstSetupProtocol = 2 years ago, current period = this year", () => {
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(currentYearPeriod);
    const firstSetupProtocol = startOfYear(subYears(new Date(), 2));

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it("#isPreviousPeriodAllowed && #isNextPeriodAllowed - Total-View", () => {
    const firstSetupProtocol = startOfYear(subYears(new Date(), 2));
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT(new DEFAULT_TYPES.HISTORY_PERIOD(firstSetupProtocol, new Date()));
    TEST_CONTEXT.SERVICE.PERIOD_STRING = DEFAULT_TYPES.PERIOD_STRING.TOTAL;

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });
});
