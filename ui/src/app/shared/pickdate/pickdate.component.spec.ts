import { endOfMonth, endOfWeek, endOfYear, startOfDay, startOfMonth, startOfWeek, startOfYear, subDays, subMonths, subWeeks, subYears } from "date-fns";
import { DefaultTypes } from "../service/defaulttypes";
import { TestContext, sharedSetup } from "../test/utils.spec";
import { PickDateComponent } from "./pickdate.component";

export function expectPreviousPeriod(testContext: TestContext, firstSetupProtocol: Date, expectToBe: boolean): void {
  expect(PickDateComponent.isPreviousPeriodAllowed(testContext.service, firstSetupProtocol)).toBe(expectToBe);
};

export function expectNextPeriod(testContext: TestContext, expectToBe: boolean): void {
  expect(PickDateComponent.isNextPeriodAllowed(testContext.service)).toBe(expectToBe);
};

describe('Pickdate', () => {

  let TEST_CONTEXT: TestContext;
  beforeEach(() =>
    TEST_CONTEXT = sharedSetup()
  );

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Day-View: firstSetupProtocol = today', () => {
    const firstSetupProtocol = new Date();
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Day-View: firstSetupProtocol = yesterday', () => {
    const firstSetupProtocol = startOfDay(subDays(new Date(), 1));
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = current week', () => {
    const firstSetupProtocol = new Date();
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = Start of previous week, current period = current week', () => {
    const firstSetupProtocol = startOfWeek(subWeeks(new Date(), 1), { weekStartsOn: 1 });
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = Today, current period = previous week', () => {
    const firstSetupProtocol = new Date();
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  const previousWeekPeriod = new DefaultTypes.HistoryPeriod(startOfWeek(subWeeks(new Date(), 1), { weekStartsOn: 1 }), endOfWeek(subWeeks(new Date(), 1), { weekStartsOn: 1 }));
  const currentWeekPeriod = new DefaultTypes.HistoryPeriod(startOfWeek(new Date(), { weekStartsOn: 1 }), endOfWeek(new Date(), { weekStartsOn: 1 }));

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = previous week, current period = previous week', () => {
    TEST_CONTEXT.service.historyPeriod.next(previousWeekPeriod);
    const firstSetupProtocol = startOfWeek(subWeeks(new Date(), 1), { weekStartsOn: 1 });
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = 2 weeks ago, current period = previous week', () => {
    TEST_CONTEXT.service.historyPeriod.next(previousWeekPeriod);
    const firstSetupProtocol = startOfWeek(subWeeks(new Date(), 2), { weekStartsOn: 1 });

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Week-View: firstSetupProtocol = 2 weeks ago, current period = current week', () => {
    TEST_CONTEXT.service.historyPeriod.next(currentWeekPeriod);
    const firstSetupProtocol = startOfWeek(subWeeks(new Date(), 2), { weekStartsOn: 1 });

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  const previousMonthPeriod = new DefaultTypes.HistoryPeriod(startOfMonth(subMonths(new Date(), 1)), endOfMonth(subMonths(new Date(), 1)));
  const currentMonthPeriod = new DefaultTypes.HistoryPeriod(startOfMonth(new Date()), endOfMonth(new Date()));

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Month-View: firstSetupProtocol = today, current period = current month', () => {
    const firstSetupProtocol = new Date();
    TEST_CONTEXT.service.historyPeriod.next(currentMonthPeriod);
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });
  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Month-View: firstSetupProtocol = start of current month, current period = previous month', () => {
    TEST_CONTEXT.service.historyPeriod.next(previousMonthPeriod);
    const firstSetupProtocol = startOfMonth(subMonths(new Date(), 1));
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Month-View: firstSetupProtocol = 2 months ago, current period = previous month', () => {
    TEST_CONTEXT.service.historyPeriod.next(previousMonthPeriod);
    const firstSetupProtocol = startOfMonth(subMonths(new Date(), 2));
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  const previousYearPeriod = new DefaultTypes.HistoryPeriod(startOfYear(subYears(new Date(), 1)), endOfYear(subYears(new Date(), 1)));
  const currentYearPeriod = new DefaultTypes.HistoryPeriod(startOfYear(new Date()), endOfYear(new Date()));

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Year-View: firstSetupProtocol = today, current period = current year', () => {
    const firstSetupProtocol = new Date();
    TEST_CONTEXT.service.historyPeriod.next(currentYearPeriod);
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, false);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Year-View: firstSetupProtocol = previous year, current period = previous year', () => {
    TEST_CONTEXT.service.historyPeriod.next(previousYearPeriod);
    const firstSetupProtocol = startOfYear(subYears(new Date(), 1));
    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, false);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Year-View: firstSetupProtocol = 2 years ago, current period = previous year', () => {
    TEST_CONTEXT.service.historyPeriod.next(previousYearPeriod);
    const firstSetupProtocol = startOfYear(subYears(new Date(), 2));

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, true);
  });

  it('#isPreviousPeriodAllowed && #isNextPeriodAllowed - Year-View: firstSetupProtocol = 2 years ago, current period = this year', () => {
    TEST_CONTEXT.service.historyPeriod.next(currentYearPeriod);
    const firstSetupProtocol = startOfYear(subYears(new Date(), 2));

    expectPreviousPeriod(TEST_CONTEXT, firstSetupProtocol, true);
    expectNextPeriod(TEST_CONTEXT, false);
  });
});
