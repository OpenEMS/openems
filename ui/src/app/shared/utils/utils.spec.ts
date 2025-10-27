// @ts-strict-ignore
import { EvcsComponent } from "../components/edge/components/evcsComponent";
import { DummyConfig } from "../components/edge/edgeconfig.spec";
import { TestingUtils } from "../components/shared/testing/utils.spec";
import { Currency, EdgeConfig } from "../shared";
import { HistoryUtils, Utils } from "./utils";

describe("Utils", () => {

  beforeEach(async () => {
    // Used to load translations globally for CONVERT_PRICE_TO_CENT_PER_KWH, not recommended to implement locale statically
    await TestingUtils.sharedSetup();
  });

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
    DummyConfig.Component.Heat_MYPV_ACTHOR("heat0", "Heatingelement")
  );

  const channelData: HistoryUtils.ChannelData = {
    "ConsumptionActivePower": [null, null, null, 565, 560, 561, 573],
    "evcs0/ChargePower": [null, null, null, 0, 0, 0, 100],
    "evcs1/ChargePower": [null, null, null, 0, 0, 0, 0],
    "meter0/ActivePower": [124, 0, null, 0, 173, 0, 100],
    "meter1/ActivePower": [124, 0, null, 0, 173, 0, 0],
    "heat1/ActivePower": [null, null, null, 0, 0, 0, 100],
  };

  const evcsComponents: EvcsComponent[] = EvcsComponent.getComponents(dummyConfig, null);

  const heatComponents: EdgeConfig.Component[] = dummyConfig.getComponentsImplementingNature("io.openems.edge.heat.api.Heat")
    .filter(component =>
      !(component.factoryId === "Controller.Heat.Heatingelement") &&
      !component.isEnabled === false);

  const consumptionMeterComponents: EdgeConfig.Component[] = dummyConfig.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
    .filter(component => component.isEnabled && dummyConfig.isTypeConsumptionMetered(component));

  it("+calculateOtherConsumption - evcs + consumptionMeters", () => {
    const expectedResult = [null, null, null, 565, 214, 561, 373];
    expect(Utils.calculateOtherConsumption(channelData, evcsComponents, heatComponents, consumptionMeterComponents)).toEqual(expectedResult);
  });
  it("+calculateOtherConsumption - only consumptionMeters", () => {
    const expectedResult2 = [null, null, null, 565, 214, 561, 473];
    expect(Utils.calculateOtherConsumption(channelData, [], [], consumptionMeterComponents)).toEqual(expectedResult2);
  });
  it("+calculateOtherConsumption - only evcs", () => {
    const expectedResult3 = [null, null, null, 565, 560, 561, 473];
    expect(Utils.calculateOtherConsumption(channelData, evcsComponents, heatComponents, [])).toEqual(expectedResult3);
  });
  it("+calculateOtherConsumption - no evcs + no consumptionMeters", () => {
    const expectedResult4 = [null, null, null, 565, 560, 561, 573];
    expect(Utils.calculateOtherConsumption(channelData, [], [], [])).toEqual(expectedResult4);
  });
  it("+calculateOtherConsumption - heat + consumptionMeters", () => {
    const expectedResult = [null, null, null, 565, 214, 561, 473];
    expect(Utils.calculateOtherConsumption(channelData, [], heatComponents, consumptionMeterComponents)).toEqual(expectedResult);
  });
  it("+calculateOtherConsumption - only heat", () => {
    const expectedResult4 = [null, null, null, 565, 560, 561, 573];
    expect(Utils.calculateOtherConsumption(channelData, [], heatComponents, [])).toEqual(expectedResult4);
  });

  it("+CONVERT_PRICE_TO_CENT_PER_KWH", () => {
    let currencyLabel: string = Currency.getCurrencyLabelByCurrency("EUR");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(0)).toEqual("0 Cent/kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(null)).toEqual("- Cent/kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(undefined)).toEqual("- Cent/kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(1)).toEqual("0,1 Cent/kWh");

    currencyLabel = Currency.getCurrencyLabelByCurrency("CHF");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(0)).toEqual("0 Rp./kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(null)).toEqual("- Rp./kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(undefined)).toEqual("- Rp./kWh");
    expect(Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(1)).toEqual("0,1 Rp./kWh");
  });
});
