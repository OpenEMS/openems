// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, CurrentData, GridMode, Utils } from "src/app/shared/shared";
import { Icon } from "src/app/shared/type/widget";
import { GridSectionComponent } from "../../../energymonitor/chart/section/GRID.COMPONENT";
import { ModalComponent } from "../modal/modal";

@Component({
  selector: "grid",
  templateUrl: "./FLAT.HTML",
  standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

  private static readonly RESTRICTION_MODE: ChannelAddress = new ChannelAddress("ctrlEssLimiter14a0", "RestrictionMode");
  private static readonly GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress("_sum", "GridActivePower");
  private static readonly GRID_MODE: ChannelAddress = new ChannelAddress("_sum", "GridMode");

  public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
  public readonly GridMode = GridMode;

  public gridBuyPower: number;
  public gridSellPower: number;

  protected gridMode: number;
  protected gridState: string;
  protected icon: Icon | null = null;
  protected isActivated: boolean = false;

  async presentModal() {
    const modal = await THIS.MODAL_CONTROLLER.CREATE({
      component: ModalComponent,
      componentProps: {
        edge: THIS.EDGE,
      },
    });
    return await MODAL.PRESENT();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    const channelAddresses: ChannelAddress[] = [
      FlatComponent.GRID_ACTIVE_POWER, FlatComponent.GRID_MODE,

      // TODO should be moved to Modal
      new ChannelAddress("_sum", "GridActivePowerL1"),
      new ChannelAddress("_sum", "GridActivePowerL2"),
      new ChannelAddress("_sum", "GridActivePowerL3"),
    ];

    if (GRID_SECTION_COMPONENT.IS_CONTROLLER_ENABLED(THIS.CONFIG, "CONTROLLER.ESS.LIMITER14A")) {
      CHANNEL_ADDRESSES.PUSH(FlatComponent.RESTRICTION_MODE);
    }
    return channelAddresses;
  }
  protected override onCurrentData(currentData: CurrentData) {
    THIS.IS_ACTIVATED = GRID_SECTION_COMPONENT.IS_CONTROLLER_ENABLED(THIS.CONFIG, "CONTROLLER.ESS.LIMITER14A");
    THIS.GRID_MODE = CURRENT_DATA.ALL_COMPONENTS[FlatComponent.GRID_MODE.toString()];
    THIS.GRID_STATE = Converter.GRID_STATE_TO_MESSAGE(THIS.TRANSLATE, currentData);
    const gridActivePower = CURRENT_DATA.ALL_COMPONENTS[FlatComponent.GRID_ACTIVE_POWER.toString()];
    THIS.GRID_BUY_POWER = gridActivePower;
    THIS.GRID_SELL_POWER = UTILS.MULTIPLY_SAFELY(gridActivePower, -1);
    THIS.ICON = GRID_SECTION_COMPONENT.GET_CURRENT_GRID_ICON(currentData);
  }

}
