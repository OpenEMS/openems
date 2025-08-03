// @ts-strict-ignore
import { ChangeDetectorRef, Component, inject } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { ProfileComponent } from "src/app/edge/settings/profile/profile.component";
import { PlatFormService } from "src/app/platform.service";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, ChannelRegister, CurrentData, Service, Websocket } from "src/app/shared/shared";
import { OverrideStatus } from "src/app/shared/type/general";

@Component({
  templateUrl: "./modal.html",
  standalone: false,
})
export class ModalComponent extends AbstractModal {
  protected override websocket: Websocket;
  protected override route: ActivatedRoute;
  protected override service: Service;
  override modalController: ModalController;
  protected override translate: TranslateService;
  override formBuilder: FormBuilder;
  override ref: ChangeDetectorRef;
  private platFormService = inject(PlatFormService);


  protected readonly CONVERT_TO_WATT = Converter.POWER_IN_WATT;

  protected writeChannelValues: number[] | null = [];
  protected writeChannels: ChannelAddress[] | null = [];
  protected overrideStatus: string | null = null;
  protected formattedWriteChannels: string[] | null = null;

  protected activePowerEqualsChannel: ChannelAddress | null = null;
  protected activePowerEqualsValue: number | null = null;
  protected channelRegisters = ChannelRegister;
  private profile = new ProfileComponent(this.service, this.route, null, this.translate, this.websocket, this.platFormService);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    const websocket = inject(Websocket);
    const route = inject(ActivatedRoute);
    const service = inject(Service);
    const modalController = inject(ModalController);
    const translate = inject(TranslateService);
    const formBuilder = inject(FormBuilder);
    const ref = inject(ChangeDetectorRef);

    super(websocket, route, service, modalController, translate, formBuilder, ref);
  
    this.websocket = websocket;
    this.route = route;
    this.service = service;
    this.modalController = modalController;
    this.translate = translate;
    this.formBuilder = formBuilder;
    this.ref = ref;
  }


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

  protected getModbusProtocol(componentId: string, type: string) {
    return this.profile.getModbusProtocol(componentId, type);
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
      let formattedString = `(${channel.channelId})`;
      for (const registerName in ChannelRegister) {
        if (channel.channelId.includes(registerName) && channel.channelId.startsWith("Ess0")) {
          formattedString = `(${registerName}/${ChannelRegister[registerName]})`;
          break;
        }
      }
      this.formattedWriteChannels.push(formattedString);
    });
  }
}
