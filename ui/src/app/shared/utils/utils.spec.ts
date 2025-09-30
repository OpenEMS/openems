// @ts-strict-ignore
import { DummyConfig } from "../components/edge/EDGECONFIG.SPEC";
import { TestingUtils } from "../components/shared/testing/UTILS.SPEC";
import { Currency, EdgeConfig } from "../shared";
import { HistoryUtils, Utils } from "./utils";

describe("Utils", () => {

  beforeEach(async () => {
    // Used to load translations globally for CONVERT_PRICE_TO_CENT_PER_KWH, not recommended to implement locale statically
    await TESTING_UTILS.SHARED_SETUP();
  });

  it("#subtractSafely", () => {
    expect(UTILS.SUBTRACT_SAFELY(null, null)).toEqual(null);
    expect(UTILS.SUBTRACT_SAFELY(null, undefined)).toEqual(null);
    expect(UTILS.SUBTRACT_SAFELY(0, null)).toEqual(0);
    expect(UTILS.SUBTRACT_SAFELY(1, 1)).toEqual(0);
    expect(UTILS.SUBTRACT_SAFELY(1, 2)).toEqual(-1);
    expect(UTILS.SUBTRACT_SAFELY(1)).toEqual(1);
  });

  const dummyConfig = DUMMY_CONFIG.FROM(
    DUMMY_CONFIG.COMPONENT.SOCOMEC_CONSUMPTION_METER("meter0", "Wallbox"),
    DUMMY_CONFIG.COMPONENT.SOCOMEC_CONSUMPTION_METER("meter1", "Wallbox 2"),
    DUMMY_CONFIG.COMPONENT.EVCS_HARDY_BARTH("evcs0", "Charging Station"),
    DUMMY_CONFIG.COMPONENT.EVCS_HARDY_BARTH("evcs1", "Charging Station 2"),
    DUMMY_CONFIG.COMPONENT.Heat_MYPV_ACTHOR("heat0", "Heatingelement")
  );

  const channelData: HISTORY_UTILS.CHANNEL_DATA = {
    "ConsumptionActivePower": [null, null, null, 565, 560, 561, 573],
    "evcs0/ChargePower": [null, null, null, 0, 0, 0, 100],
    "evcs1/ChargePower": [null, null, null, 0, 0, 0, 0],
    "meter0/ActivePower": [124, 0, null, 0, 173, 0, 100],
    "meter1/ActivePower": [124, 0, null, 0, 173, 0, 0],
    "heat1/ActivePower": [null, null, null, 0, 0, 0, 100],
  };

  const evcsComponents: EDGE_CONFIG.COMPONENT[] = DUMMY_CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.EVCS.API.EVCS")
    .filter(component => !(
      COMPONENT.FACTORY_ID == "EVCS.CLUSTER" ||
      COMPONENT.FACTORY_ID == "EVCS.CLUSTER.PEAK_SHAVING" ||
      COMPONENT.FACTORY_ID == "EVCS.CLUSTER.SELF_CONSUMPTION"));

  const heatComponents: EDGE_CONFIG.COMPONENT[] = DUMMY_CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.HEAT.API.HEAT")
    .filter(component =>
      !(COMPONENT.FACTORY_ID === "CONTROLLER.HEAT.HEATINGELEMENT") &&
      !COMPONENT.IS_ENABLED === false);

  const consumptionMeterComponents: EDGE_CONFIG.COMPONENT[] = DUMMY_CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
    .filter(component => COMPONENT.IS_ENABLED && DUMMY_CONFIG.IS_TYPE_CONSUMPTION_METERED(component));

  it("+calculateOtherConsumption - evcs + consumptionMeters", () => {
    const expectedResult = [null, null, null, 565, 214, 561, 373];
    expect(UTILS.CALCULATE_OTHER_CONSUMPTION(channelData, evcsComponents, heatComponents, consumptionMeterComponents)).toEqual(expectedResult);
  });
  it("+calculateOtherConsumption - only consumptionMeters", () => {
    const expectedResult2 = [null, null, null, 565, 214, 561, 473];
    expect(UTILS.CALCULATE_OTHER_CONSUMPTION(channelData, [], [], consumptionMeterComponents)).toEqual(expectedResult2);
  });
  it("+calculateOtherConsumption - only evcs", () => {
    const expectedResult3 = [null, null, null, 565, 560, 561, 473];
    expect(UTILS.CALCULATE_OTHER_CONSUMPTION(channelData, evcsComponents, heatComponents, [])).toEqual(expectedResult3);
  });
  it("+calculateOtherConsumption - no evcs + no consumptionMeters", () => {
    const expectedResult4 = [null, null, null, 565, 560, 561, 573];
    expect(UTILS.CALCULATE_OTHER_CONSUMPTION(channelData, [], [], [])).toEqual(expectedResult4);
  });
  it("+calculateOtherConsumption - heat + consumptionMeters", () => {
    const expectedResult = [null, null, null, 565, 214, 561, 473];
    expect(UTILS.CALCULATE_OTHER_CONSUMPTION(channelData, [], heatComponents, consumptionMeterComponents)).toEqual(expectedResult);
  });
  it("+calculateOtherConsumption - only heat", () => {
    const expectedResult4 = [null, null, null, 565, 560, 561, 573];
    expect(UTILS.CALCULATE_OTHER_CONSUMPTION(channelData, [], heatComponents, [])).toEqual(expectedResult4);
  });

  it("+CONVERT_PRICE_TO_CENT_PER_KWH", () => {
    let currencyLabel: string = CURRENCY.GET_CURRENCY_LABEL_BY_CURRENCY("EUR");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(0)).toEqual("0 Cent/kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(null)).toEqual("- Cent/kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(undefined)).toEqual("- Cent/kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(1)).toEqual("0,1 Cent/kWh");

    currencyLabel = CURRENCY.GET_CURRENCY_LABEL_BY_CURRENCY("CHF");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(0)).toEqual("0 Rp./kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(null)).toEqual("- Rp./kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(undefined)).toEqual("- Rp./kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(1)).toEqual("0,1 Rp./kWh");
  });
});
