import { SumState } from "../index/shared/sumState";
import { Edge, EdgePermission } from "./shared";
import { Role } from "./type/role";

describe('EdgePermission', () => {

  const edge = new Edge("", "", "", "", Role.ADMIN, true, new Date(), SumState.OK, null);

  it('#getAllowedHistoryPeriods - no first ibn date', () => {
    expect(EdgePermission.getAllowedHistoryPeriods(edge, ['day', 'week', 'month', 'year', 'custom'])).toEqual(['day', 'week', 'month', 'year', 'custom']);
  });

  const edgeWithFirstIbnDate = new Edge("", "", "", "", Role.ADMIN, true, new Date(), SumState.OK, new Date());
  it('#getAllowedHistoryPeriods - first ibn date', () => {
    expect(EdgePermission.getAllowedHistoryPeriods(edgeWithFirstIbnDate, ['day', 'week', 'month', 'year', 'total', 'custom'])).toEqual(['day', 'week', 'month', 'year', 'total', 'custom']);
  });
});
