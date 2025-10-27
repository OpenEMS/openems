import { Component } from "@angular/core";
import { GridSectionComponent } from "src/app/edge/live/energymonitor/chart/section/grid.component";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { TimeUtils } from "src/app/shared/utils/time/timeutils";

@Component({
  selector: "gridWidget",
  templateUrl: "./flat.html",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  private static readonly RESTRICTION_MODE_14A: ChannelAddress = new ChannelAddress("ctrlEssLimiter14a0", "RestrictionMode");
  private static readonly RESTRICTION_TIME_14A: ChannelAddress = new ChannelAddress("ctrlEssLimiter14a0", "CumulatedRestrictionTime");
  private static readonly OFF_GRID_TIME: ChannelAddress = new ChannelAddress("_sum", "GridModeOffGridTime");

  private static readonly RESTRICTON_MODE_RCR: ChannelAddress = new ChannelAddress("ctrlEssRippleControlReceiver0", "RestrictionMode");
  private static readonly RESTRICTION_TIME_RCR: ChannelAddress = new ChannelAddress("ctrlEssRippleControlReceiver0", "CumulatedRestrictionTime");

  protected restrictionTime14a: number | null = null;
  protected restrictionTimeRcr: number | null = null;
  protected offGridTime: number | null = null;
  protected TIME_CONVERTER = TimeUtils.formatSecondsToDuration;

  protected override getChannelAddresses(): ChannelAddress[] {
    const channelAddresses = [];
    if (GridSectionComponent.isControllerEnabled(this.config, "Controller.Ess.Limiter14a")) {
      channelAddresses.push(
        FlatComponent.RESTRICTION_MODE_14A,
        FlatComponent.RESTRICTION_TIME_14A,
      );
    }
    if (GridSectionComponent.isControllerEnabled(this.config, "Controller.Ess.RippleControlReceiver")) {
      channelAddresses.push(
        FlatComponent.RESTRICTON_MODE_RCR,
        FlatComponent.RESTRICTION_TIME_RCR,
      );
    }
    if (GridSectionComponent.isControllerEnabled(this.config, "Controller.Ess.EmergencyCapacityReserve")) {
      channelAddresses.push(
        FlatComponent.OFF_GRID_TIME,
      );
    }
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData): void {
    this.restrictionTime14a = currentData.allComponents["ctrlEssLimiter14a0/CumulatedRestrictionTime"];
    this.restrictionTimeRcr = currentData.allComponents["ctrlEssRippleControlReceiver0/CumulatedRestrictionTime"];
    this.offGridTime = currentData.allComponents["_sum/GridModeOffGridTime"];
  }
}
