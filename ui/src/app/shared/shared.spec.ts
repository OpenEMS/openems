// @ts-strict-ignore
import { SumState } from "../index/shared/sumState";
import { Edge, EdgePermission } from "./shared";
import { Role } from "./type/role";

describe("EdgePermission", () => {

  const edge = new Edge("", "", "", "2024.2.2", Role.ADMIN, true, new Date(), SumState.OK, null);

  it("#getAllowedHistoryPeriods - no first ibn date", () => {
    expect(EdgePermission.getAllowedHistoryPeriods(edge, ["day", "week", "month", "year"])).toEqual(["day", "week", "month", "year"]);
  });

  const edgeWithFirstIbnDate = new Edge("", "", "", "", Role.ADMIN, true, new Date(), SumState.OK, new Date());
  it("#getAllowedHistoryPeriods - first ibn date", () => {
    expect(EdgePermission.getAllowedHistoryPeriods(edgeWithFirstIbnDate, ["day", "week", "month", "year", "total"])).toEqual(["day", "week", "month", "year", "total"]);
  });

  it("#getAllowedHistoryPeriods - historyPeriods: []", () => {
    expect(EdgePermission.getAllowedHistoryPeriods(edgeWithFirstIbnDate, [])).toEqual(["day", "week", "month", "year", "total", "custom"]);
  });

  it("#getAllowedHistoryPeriods - historyPeriod: null", () => {
    expect(EdgePermission.getAllowedHistoryPeriods(edgeWithFirstIbnDate, null)).toEqual(["day", "week", "month", "year", "total", "custom"]);
  });

  it("#getAllowedHistoryPeriods - historyPeriod: undefined", () => {
    expect(EdgePermission.getAllowedHistoryPeriods(edgeWithFirstIbnDate, undefined)).toEqual(["day", "week", "month", "year", "total", "custom"]);
  });

  const edgeWithoutFirstIbnDate = new Edge("", "", "", "", Role.ADMIN, true, new Date(), SumState.OK, null);
  it("#getAllowedHistoryPeriods - no first ibn date", () => {
    expect(EdgePermission.getAllowedHistoryPeriods(edgeWithoutFirstIbnDate)).toEqual(["day", "week", "month", "year", "custom"]);
  });
});
