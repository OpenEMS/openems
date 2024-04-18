import { FeedInSetting, FeedInType } from "../../shared/enums";
import { SafetyCountry } from "../../views/configuration-execute/safety-country";
import { AbstractHomeIbn } from "./abstract-home";

type Home10App = {
  SAFETY_COUNTRY: SafetyCountry,
  RIPPLE_CONTROL_RECEIVER_ACTIV: boolean,
  MAX_FEED_IN_POWER?: number,
  FEED_IN_SETTING: string,
  HAS_DC_PV1: boolean,
  DC_PV1_ALIAS?: string,
  HAS_DC_PV2: boolean,
  DC_PV2_ALIAS?: string,
  HAS_EMERGENCY_RESERVE: boolean,
  EMERGENCY_RESERVE_ENABLED?: boolean,
  EMERGENCY_RESERVE_SOC?: number,
  SHADOW_MANAGEMENT_DISABLED?: boolean
}

export abstract class AbstractHome10Ibn extends AbstractHomeIbn {

  public override readonly homeAppAlias: string = 'FENECON Home';
  public override readonly homeAppId: string = 'App.FENECON.Home';
  public override readonly maxNumberOfModulesPerTower: number = 10;
  public override readonly maxNumberOfPvStrings: number = 2;
  public override readonly maxNumberOfMppt: number = -1;
  public override readonly maxNumberOfTowers: number = 3;
  public override readonly minNumberOfModulesPerTower: number = 4;

  public override mppt: {
    mppt1: boolean;
    mppt2: boolean;
  } = {
      mppt1: true,
      mppt2: true,
    };

  public override getHomeAppProperties(safetyCountry: SafetyCountry, feedInSetting: FeedInSetting): {} {

    const dc1 = this.pv.dc[0];
    const dc2 = this.pv.dc[1];

    const home10AppProperties: Home10App = {
      SAFETY_COUNTRY: safetyCountry,
      ...(this.feedInLimitation.feedInType === FeedInType.EXTERNAL_LIMITATION && { RIPPLE_CONTROL_RECEIVER_ACTIV: true }),
      ...(this.feedInLimitation.feedInType !== FeedInType.EXTERNAL_LIMITATION && { FEED_IN_TYPE: this.feedInLimitation.feedInType }),
      ...(this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION && { MAX_FEED_IN_POWER: this.feedInLimitation.maximumFeedInPower }),
      FEED_IN_SETTING: feedInSetting,
      HAS_DC_PV1: dc1.isSelected,
      ...(dc1.isSelected && { DC_PV1_ALIAS: dc1.alias }),
      HAS_DC_PV2: dc2.isSelected,
      ...(dc2.isSelected && { DC_PV2_ALIAS: dc2.alias }),
      HAS_EMERGENCY_RESERVE: this.emergencyReserve.isEnabled,
      ...(this.emergencyReserve.isEnabled && { EMERGENCY_RESERVE_ENABLED: this.emergencyReserve.isReserveSocEnabled }),
      ...(this.emergencyReserve.isReserveSocEnabled && { EMERGENCY_RESERVE_SOC: this.emergencyReserve.value }),
      ...(this.batteryInverter?.shadowManagementDisabled && { SHADOW_MANAGEMENT_DISABLED: true }),
    };

    return home10AppProperties;
  }
}
