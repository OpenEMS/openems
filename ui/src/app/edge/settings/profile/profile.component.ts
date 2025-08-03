import { Component, effect, OnInit, signal, WritableSignal, inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { PlatFormService } from "src/app/platform.service";
import { CategorizedComponents } from "src/app/shared/components/edge/edgeconfig";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetLatestSetupProtocolCoreInfoRequest } from "src/app/shared/jsonrpc/request/getLatestSetupProtocolCoreInfoRequest";
import { GetSetupProtocolRequest } from "src/app/shared/jsonrpc/request/getSetupProtocolRequest";
import { Base64PayloadResponse } from "src/app/shared/jsonrpc/response/base64PayloadResponse";
import { getFileName, GetLatestSetupProtocolCoreInfoResponse } from "src/app/shared/jsonrpc/response/getLatestSetupProtocolCoreInfoResponse";
import { ObjectUtils } from "src/app/shared/utils/object/object.utils";
import { environment } from "../../../../environments";
import { ChannelAddress, Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket } from "../../../shared/shared";
import { ChannelExportXlsxRequest } from "./channelexport/channelExportXlsxRequest";
import { GetModbusProtocolExportXlsxRequest } from "./modbusapi/getModbusProtocolExportXlsxRequest";

@Component({
  selector: ProfileComponent.SELECTOR,
  templateUrl: "./profile.component.html",
  standalone: false,
})
export class ProfileComponent implements OnInit {
  private service = inject(Service);
  private route = inject(ActivatedRoute);
  popoverController = inject(PopoverController);
  private translate = inject(TranslateService);
  private websocket = inject(Websocket);
  private platFormService = inject(PlatFormService);


  private static readonly SELECTOR = "profile";

  public environment = environment;

  public edge: Edge | null = null;
  public config: EdgeConfig | null = null;
  public subscribedChannels: ChannelAddress[] = [];

  public components: CategorizedComponents[] | null = null;

  protected latestSetupProtocolData: GetLatestSetupProtocolCoreInfoResponse["result"] | null = null;
  protected spinnerId: string = ProfileComponent.SELECTOR;
  protected isLoading: WritableSignal<boolean> = signal(true);
  protected isAtLeastOwner: boolean = false;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    effect(() => {
      const isLoading = this.isLoading();

      if (isLoading === true) {
        this.service.startSpinnerTransparentBackground(this.spinnerId);
      } else {
        this.service.stopSpinner(this.spinnerId);
      }
    });
  }

  public ngOnInit() {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
      this.service.getConfig().then(async config => {
        this.isAtLeastOwner = EdgePermission.isUserAllowedToSetupProtocolDownload(edge);
        this.config = config;
        const categorizedComponentIds: string[] = ["_appManager", "_componentManager", "_cycle", "_meta", "_power", "_sum", "_predictorManager", "_host", "_evcsSlowPowerIncreaseFilter", "_serialNumber"];
        this.components = config.listActiveComponents(categorizedComponentIds, this.translate);
        await this.setLatestSetupProtocolData();
      });
    });
  }

  public getModbusProtocol(componentId: string, type: string) {
    this.service.getCurrentEdge().then(edge => {
      const request = new ComponentJsonApiRequest({ componentId: componentId, payload: new GetModbusProtocolExportXlsxRequest() });
      edge.sendRequest(this.service.websocket, request).then(response => {
        Utils.downloadXlsx(response as Base64PayloadResponse, "Modbus-" + type + "-" + edge.id);
      }).catch(reason => {
        this.service.toast(this.translate.instant("Edge.Config.PROFILE.ERROR_DOWNLOADING_MODBUS_PROTOCOL") + ": " + (reason as JsonrpcResponseError).error.message, "danger");
      });
    });
  }

  public getChannelExport(componentId: string) {
    this.service.getCurrentEdge().then(edge => {
      const request = new ComponentJsonApiRequest({ componentId: "_componentManager", payload: new ChannelExportXlsxRequest({ componentId: componentId }) });
      edge.sendRequest(this.service.websocket, request).then(response => {
        Utils.downloadXlsx(response as Base64PayloadResponse, "ChannelExport-" + edge.id + "-" + componentId);
      }).catch(reason => {
        console.warn(reason);
      });
    });
  }

  /**
   * Downloads the lates setup protocol
   */
  protected async downloadLatestSetupProtocol(): Promise<void> {

    if (!(this.latestSetupProtocolData?.setupProtocolId)) {
      throw Error("Download not possible: setupProtocolId is missing");
    }

    const canExecuteDownload = this.platFormService.deviceHasFilePermissions();
    if (!canExecuteDownload) {
      return;
    }

    this.isLoading.set(true);
    const setupProtocol: Base64PayloadResponse | null = await this.platFormService.sendRequest(new GetSetupProtocolRequest({ setupProtocolId: this.latestSetupProtocolData.setupProtocolId.toString() }), this.websocket);
    if (!setupProtocol) {
      this.isLoading.set(false);
      return;
    }

    const blob: Blob | null = this.platFormService.convertBase64ToBlob(setupProtocol);

    if (!blob) {
      this.isLoading.set(false);
      return;
    }

    const fileName = getFileName(this.latestSetupProtocolData.setupProtocolType, this.latestSetupProtocolData.createDate, this.edge);
    this.platFormService.downloadAsPdf(blob, fileName);
    this.isLoading.set(false);
  }

  private async setLatestSetupProtocolData() {
    this.isLoading.set(true);

    const edge = await this.service.getCurrentEdge();
    const request = new GetLatestSetupProtocolCoreInfoRequest({ edgeId: edge.id });
    const setupProtocolData: GetLatestSetupProtocolCoreInfoResponse = await this.websocket.sendRequest(request) as GetLatestSetupProtocolCoreInfoResponse;

    if (!(ObjectUtils.hasKeys(setupProtocolData.result, ["setupProtocolId", "createDate"]))) {
      this.isLoading.set(false);
      return;
    }

    const result = setupProtocolData.result;
    this.latestSetupProtocolData = { setupProtocolType: result.setupProtocolType, setupProtocolId: result.setupProtocolId, createDate: result.createDate };
    this.isLoading.set(false);
  }
}
