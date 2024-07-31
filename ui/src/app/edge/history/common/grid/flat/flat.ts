import { Component } from '@angular/core';
import { GridSectionComponent } from 'src/app/edge/live/energymonitor/chart/section/grid.component';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';
import { TimeUtils } from 'src/app/shared/utils/time/timeutils';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';

@Component({
  selector: 'gridWidget',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

  protected restrictionTime: number | null = null;
  protected offGridTime: number | null = null;
  protected TIME_CONVERTER = TimeUtils.formatSecondsToDuration;

  private static readonly RESTRICTION_MODE: ChannelAddress = new ChannelAddress('ctrlEssLimiter14a0', 'RestrictionMode');
  private static readonly RESTRICTION_TIME: ChannelAddress = new ChannelAddress('ctrlEssLimiter14a0', 'CumulatedRestrictionTime');
  private static readonly OFF_GRID_TIME: ChannelAddress = new ChannelAddress('_sum', 'GridModeOffGridTime');

  protected override getChannelAddresses(): ChannelAddress[] {
    const channelAddresses = [];
    if (GridSectionComponent.isControllerEnabled(this.config, "Controller.Ess.Limiter14a")) {
      channelAddresses.push(
        FlatComponent.RESTRICTION_MODE,
        FlatComponent.RESTRICTION_TIME,
        FlatComponent.OFF_GRID_TIME,
      );
    }
    return channelAddresses;

  }

  protected override onCurrentData(currentData: CurrentData): void {
    this.restrictionTime = currentData.allComponents["ctrlEssLimiter14a0/CumulatedRestrictionTime"];
    this.offGridTime = currentData.allComponents["_sum/GridModeOffGridTime"];
  }
}
