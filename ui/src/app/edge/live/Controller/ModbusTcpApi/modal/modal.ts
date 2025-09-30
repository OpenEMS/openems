// @ts-strict-ignore
import { ChangeDetectorRef, Component } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { ProfileComponent } from "src/app/edge/settings/profile/PROFILE.COMPONENT";
import { PlatFormService } from "src/app/PLATFORM.SERVICE";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, ChannelRegister, CurrentData, Service, Websocket } from "src/app/shared/shared";
import { OverrideStatus } from "src/app/shared/type/general";

@Component({
  templateUrl: "./MODAL.HTML",
  standalone: false,
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
  private profile = new ProfileComponent(THIS.SERVICE, THIS.ROUTE, null, THIS.TRANSLATE, THIS.WEBSOCKET, THIS.PLAT_FORM_SERVICE);

  constructor(
    protected override websocket: Websocket,
    protected override route: ActivatedRoute,
    protected override service: Service,
    public override modalController: ModalController,
    protected override translate: TranslateService,
    public override formBuilder: FormBuilder,
    public override ref: ChangeDetectorRef,
    private platFormService: PlatFormService,
  ) {
    super(websocket, route, service, modalController, translate, formBuilder, ref);
  }


  protected override getChannelAddresses(): ChannelAddress[] {
    THIS.ACTIVE_POWER_EQUALS_CHANNEL = new ChannelAddress(THIS.COMPONENT.ID, "Ess0SetActivePowerEquals");
    const writeChannelIds = THIS.CONFIG.COMPONENTS[THIS.COMPONENT.ID]?.PROPERTIES.WRITE_CHANNELS || [];
    THIS.WRITE_CHANNELS = WRITE_CHANNEL_IDS.MAP(channelId => new ChannelAddress(THIS.COMPONENT.ID, channelId));
    return [
      ...THIS.WRITE_CHANNELS,
      THIS.ACTIVE_POWER_EQUALS_CHANNEL,
      new ChannelAddress(THIS.COMPONENT.ID, "OverrideStatus"),
    ];
  }

  protected override onIsInitialized(): void {
    THIS.EDGE.GET_CONFIG(THIS.WEBSOCKET).subscribe((config) => {
      const newChannels = (CONFIG.COMPONENTS[THIS.COMPONENT.ID]?.properties?.writeChannels || [])
        .map(channelId => new ChannelAddress(THIS.COMPONENT.ID, channelId));

      THIS.WRITE_CHANNELS = NEW_CHANNELS.FILTER(channel => !CHANNEL.CHANNEL_ID.INCLUDES("Ess0SetActivePowerEquals"));
      THIS.GET_FORMAT_CHANNEL_NAMES();
      THIS.EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, THIS.COMPONENT.ID, THIS.WRITE_CHANNELS);
    });
  }

  protected getModbusProtocol(componentId: string, type: string) {
    return THIS.PROFILE.GET_MODBUS_PROTOCOL(componentId, type);
  }

  protected override onCurrentData(currentData: CurrentData) {
    THIS.ACTIVE_POWER_EQUALS_VALUE = THIS.EDGE.CURRENT_DATA.VALUE.CHANNEL[THIS.ACTIVE_POWER_EQUALS_CHANNEL!.toString()];
    THIS.WRITE_CHANNEL_VALUES = THIS.WRITE_CHANNELS?.map(channel =>
      THIS.EDGE.CURRENT_DATA.VALUE.CHANNEL[CHANNEL.TO_STRING()],
    ) || [];
    THIS.OVERRIDE_STATUS = THIS.GET_TRANSLATED_STATE(CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/OverrideStatus"]);
  }

  protected getTranslatedChannel(channel: ChannelAddress): string {
    if (CHANNEL.CHANNEL_ID.INCLUDES("Ess0SetActive")) {
      const channelName = CHANNEL.CHANNEL_ID.REPLACE("Ess0", "");
      switch (channelName) {
        case "SetActivePowerEquals":
          return THIS.TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_EQUALS");
        case "SetActivePowerGreaterOrEquals":
          return THIS.TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_GREATER_OR_EQUALS");
        case "SetActivePowerLessOrEquals":
          return THIS.TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.SET_ACTIVE_POWER_LESS_OR_EQUALS");
      }
    }
  }

  private getTranslatedState(state: OverrideStatus) {
    switch (state) {
      case OVERRIDE_STATUS.ACTIVE:
        return THIS.TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.OVERRIDING");
      case OVERRIDE_STATUS.ERROR:
        return THIS.TRANSLATE.INSTANT("EVCS.ERROR");
      default:
        return THIS.TRANSLATE.INSTANT("MODBUS_TCP_API_READ_WRITE.NOT_OVERRIDING");
    }
  }

  /**
   * This method adds the name and register number of the corresponding channel to
   * the modal view. It has to be done dynamically since channels can be overwritten in any order.
   */
  private getFormatChannelNames(): void {
    THIS.FORMATTED_WRITE_CHANNELS = [];
    THIS.WRITE_CHANNELS.FOR_EACH(channel => {
      let formattedString = `(${CHANNEL.CHANNEL_ID})`;
      for (const registerName in ChannelRegister) {
        if (CHANNEL.CHANNEL_ID.INCLUDES(registerName) && CHANNEL.CHANNEL_ID.STARTS_WITH("Ess0")) {
          formattedString = `(${registerName}/${ChannelRegister[registerName]})`;
          break;
        }
      }
      THIS.FORMATTED_WRITE_CHANNELS.PUSH(formattedString);
    });
  }
}
