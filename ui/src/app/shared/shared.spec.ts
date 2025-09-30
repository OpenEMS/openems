// @ts-strict-ignore
import { SumState } from "../index/shared/sumState";
import { Edge, EdgePermission } from "./shared";
import { Role } from "./type/role";

describe("EdgePermission", () => {

  const edge = new Edge("", "", "", "2024.2.2", ROLE.ADMIN, true, new Date(), SUM_STATE.OK, null);

  it("#getAllowedHistoryPeriods - no first ibn date", () => {
    expect(EDGE_PERMISSION.GET_ALLOWED_HISTORY_PERIODS(edge, ["day", "week", "month", "year"])).toEqual(["day", "week", "month", "year"]);
  });

  const edgeWithFirstIbnDate = new Edge("", "", "", "", ROLE.ADMIN, true, new Date(), SUM_STATE.OK, new Date());
  it("#getAllowedHistoryPeriods - first ibn date", () => {
    expect(EDGE_PERMISSION.GET_ALLOWED_HISTORY_PERIODS(edgeWithFirstIbnDate, ["day", "week", "month", "year", "total"])).toEqual(["day", "week", "month", "year", "total"]);
  });

  it("#getAllowedHistoryPeriods - historyPeriods: []", () => {
    expect(EDGE_PERMISSION.GET_ALLOWED_HISTORY_PERIODS(edgeWithFirstIbnDate, [])).toEqual(["day", "week", "month", "year", "total", "custom"]);
  });

  it("#getAllowedHistoryPeriods - historyPeriod: null", () => {
    expect(EDGE_PERMISSION.GET_ALLOWED_HISTORY_PERIODS(edgeWithFirstIbnDate, null)).toEqual(["day", "week", "month", "year", "total", "custom"]);
  });

  it("#getAllowedHistoryPeriods - historyPeriod: undefined", () => {
    expect(EDGE_PERMISSION.GET_ALLOWED_HISTORY_PERIODS(edgeWithFirstIbnDate, undefined)).toEqual(["day", "week", "month", "year", "total", "custom"]);
  });

  const edgeWithoutFirstIbnDate = new Edge("", "", "", "", ROLE.ADMIN, true, new Date(), SUM_STATE.OK, null);
  it("#getAllowedHistoryPeriods - no first ibn date", () => {
    expect(EDGE_PERMISSION.GET_ALLOWED_HISTORY_PERIODS(edgeWithoutFirstIbnDate)).toEqual(["day", "week", "month", "year", "custom"]);
  });
});
