// @ts-strict-ignore
import { Component } from "@angular/core";
import { ProfileComponent } from "src/app/edge/settings/profile/profile.component";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, ChannelRegister, CurrentData } from "src/app/shared/shared";
import { OverrideStatus } from "src/app/shared/type/general";

@Component({
  templateUrl: "./modal.html",
})
export class ModalComponent extends AbstractModal {

  protected readonly CONVERT_TO_WATT = Converter.POWER_IN_WATT;

  protected writeChannelValues: number[] | null = [];
  protected writeChannels: ChannelAddress[] | null = [];
  protected overrideStatus: string | null = null;
  protected formattedWriteChannels: string[] | null = null;

  protected activePowerEqualsChannel: ChannelAddress | null = null;
  protected activePowerEqualsValue: number | null = null;
  protected channelRegisters = ChannelRegister;

  private profile = new ProfileComponent(this.service, this.route, null, this.translate);

  protected override getChannelAddresses(): ChannelAddress[] {
    this.activePowerEqualsChannel = new ChannelAddress(this.component.id, "Ess0SetActivePowerEquals");
    const writeChannelIds = this.config.components[this.component.id]?.properties.writeChannels || [];
    this.writeChannels = writeChannelIds.map(channelId => new ChannelAddress(this.component.id, channelId));
    return [
      ...this.writeChannels,
      this.activePowerEqualsChannel,
      new ChannelAddress(this.component.id, "OverrideStatus"),
    ];
  }

  protected override onIsInitialized(): void {
    this.edge.getConfig(this.websocket).subscribe((config) => {
      const newChannels = (config.components[this.component.id]?.properties?.writeChannels || [])
        .map(channelId => new ChannelAddress(this.component.id, channelId));

      this.writeChannels = newChannels.filter(channel => !channel.channelId.includes("Ess0SetActivePowerEquals"));
      this.getFormatChannelNames();
      this.edge.subscribeChannels(this.websocket, this.component.id, this.writeChannels);
    });
  }

  protected getModbusProtocol(componentId: string) {
    return this.profile.getModbusProtocol(componentId);
  }

  protected override onCurrentData(currentData: CurrentData) {
    this.activePowerEqualsValue = this.edge.currentData.value.channel[this.activePowerEqualsChannel!.toString()];
    this.writeChannelValues = this.writeChannels?.map(channel =>
      this.edge.currentData.value.channel[channel.toString()],
    ) || [];
    this.overrideStatus = this.getTranslatedState(currentData.allComponents[this.component.id + "/OverrideStatus"]);
  }

  protected getTranslatedChannel(channel: ChannelAddress): string {
    if (channel.channelId.includes("Ess0SetActive")) {
      const channelName = channel.channelId.replace("Ess0", "");
      switch (channelName) {
        case "SetActivePowerEquals":
          return this.translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_EQUALS");
        case "SetActivePowerGreaterOrEquals":
          return this.translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_GREATER_OR_EQUALS");
        case "SetActivePowerLessOrEquals":
          return this.translate.instant("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_LESS_OR_EQUALS");
      }
    }
  }

  private getTranslatedState(state: OverrideStatus) {
    switch (state) {
      case OverrideStatus.ACTIVE:
        return this.translate.instant("MODBUS_TCP_API_READ_WRITE.OVERRIDING");
      case OverrideStatus.ERROR:
        return this.translate.instant("EVCS.error");
      default:
        return this.translate.instant("MODBUS_TCP_API_READ_WRITE.NOT_OVERRIDING");
    }
  }

  /**
   * This method adds the name and register number of the corresponding channel to
   * the modal view. It has to be done dynamically since channels can be overwritten in any order.
   */
  private getFormatChannelNames(): void {
    this.formattedWriteChannels = [];
    this.writeChannels.forEach(channel => {
      for (const registerName in ChannelRegister) {
        if (channel.channelId.includes(registerName)) {
          // If channelId is included in ChannelRegister, get key/value e.g. SetActivePowerEquals/706
          const formattedString = `(${registerName}/${ChannelRegister[registerName]})`;
          this.formattedWriteChannels.push(formattedString);
        }
      }
    });
  }
}
