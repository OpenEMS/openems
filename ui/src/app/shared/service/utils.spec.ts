// @ts-strict-ignore
import { DummyConfig } from "../components/edge/edgeconfig.spec";
import { EdgeConfig } from "../shared";
import { HistoryUtils, Utils } from "./utils";

describe("Utils", () => {

  it("#subtractSafely", () => {
    expect(Utils.subtractSafely(null, null)).toEqual(null);
    expect(Utils.subtractSafely(null, undefined)).toEqual(null);
    expect(Utils.subtractSafely(0, null)).toEqual(0);
    expect(Utils.subtractSafely(1, 1)).toEqual(0);
    expect(Utils.subtractSafely(1, 2)).toEqual(-1);
    expect(Utils.subtractSafely(1)).toEqual(1);
  });

  const dummyConfig = DummyConfig.from(
    DummyConfig.Component.SOCOMEC_CONSUMPTION_METER("meter0", "Wallbox"),
    DummyConfig.Component.SOCOMEC_CONSUMPTION_METER("meter1", "Wallbox 2"),
    DummyConfig.Component.EVCS_HARDY_BARTH("evcs0", "Charging Station"),
    DummyConfig.Component.EVCS_HARDY_BARTH("evcs1", "Charging Station 2"),
  );

  const channelData: HistoryUtils.ChannelData = {
    "ConsumptionActivePower": [null, null, null, 565, 560, 561, 573],
    "evcs0/ChargePower": [null, null, null, 0, 0, 0, 100],
    "evcs1/ChargePower": [null, null, null, 0, 0, 0, 0],
    "meter0/ActivePower": [124, 0, null, 0, 173, 0, 100],
    "meter1/ActivePower": [124, 0, null, 0, 173, 0, 0],
  };

  const evcsComponents: EdgeConfig.Component[] = dummyConfig.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
    .filter(component => !(
      component.factoryId == "Evcs.Cluster" ||
      component.factoryId == "Evcs.Cluster.PeakShaving" ||
      component.factoryId == "Evcs.Cluster.SelfConsumption"));

  const consumptionMeterComponents: EdgeConfig.Component[] = dummyConfig.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
    .filter(component => component.isEnabled && dummyConfig.isTypeConsumptionMetered(component));

  it("+calculateOtherConsumption - evcs + consumptionMeters", () => {
    const expectedResult = [null, null, null, 565, 214, 561, 373];
    expect(Utils.calculateOtherConsumption(channelData, evcsComponents, consumptionMeterComponents)).toEqual(expectedResult);
  });
  it("+calculateOtherConsumption - only consumptionMeters", () => {
    const expectedResult2 = [null, null, null, 565, 214, 561, 473];
    expect(Utils.calculateOtherConsumption(channelData, [], consumptionMeterComponents)).toEqual(expectedResult2);
  });
  it("+calculateOtherConsumption - only evcs", () => {
    const expectedResult3 = [null, null, null, 565, 560, 561, 473];
    expect(Utils.calculateOtherConsumption(channelData, evcsComponents, [])).toEqual(expectedResult3);
  });
  it("+calculateOtherConsumption - no evcs + no consumptionMeters", () => {
    const expectedResult4 = [null, null, null, 565, 560, 561, 573];
    expect(Utils.calculateOtherConsumption(channelData, [], [])).toEqual(expectedResult4);
  });
});
