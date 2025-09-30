import { Component, effect, OnInit, signal, WritableSignal } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { PlatFormService } from "src/app/PLATFORM.SERVICE";
import { CategorizedComponents } from "src/app/shared/components/edge/edgeconfig";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetLatestSetupProtocolCoreInfoRequest } from "src/app/shared/jsonrpc/request/getLatestSetupProtocolCoreInfoRequest";
import { GetSetupProtocolRequest } from "src/app/shared/jsonrpc/request/getSetupProtocolRequest";
import { Base64PayloadResponse } from "src/app/shared/jsonrpc/response/base64PayloadResponse";
import { getFileName, GetLatestSetupProtocolCoreInfoResponse } from "src/app/shared/jsonrpc/response/getLatestSetupProtocolCoreInfoResponse";
import { ObjectUtils } from "src/app/shared/utils/object/OBJECT.UTILS";
import { environment } from "../../../../environments";
import { ChannelAddress, Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket } from "../../../shared/shared";
import { ChannelExportXlsxRequest } from "./channelexport/channelExportXlsxRequest";
import { GetModbusProtocolExportXlsxRequest } from "./modbusapi/getModbusProtocolExportXlsxRequest";

@Component({
  selector: PROFILE_COMPONENT.SELECTOR,
  templateUrl: "./PROFILE.COMPONENT.HTML",
  standalone: false,
})
export class ProfileComponent implements OnInit {

  private static readonly SELECTOR = "profile";

  public environment = environment;

  public edge: Edge | null = null;
  public config: EdgeConfig | null = null;
  public subscribedChannels: ChannelAddress[] = [];

  public components: CategorizedComponents[] | null = null;

  protected latestSetupProtocolData: GetLatestSetupProtocolCoreInfoResponse["result"] | null = null;
  protected spinnerId: string = PROFILE_COMPONENT.SELECTOR;
  protected isLoading: WritableSignal<boolean> = signal(true);
  protected isAtLeastOwner: boolean = false;

  constructor(
    private service: Service,
    private route: ActivatedRoute,
    public popoverController: PopoverController,
    private translate: TranslateService,
    private websocket: Websocket,
    private platFormService: PlatFormService,
  ) {
    effect(() => {
      const isLoading = THIS.IS_LOADING();

      if (isLoading === true) {
        THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(THIS.SPINNER_ID);
      } else {
        THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
      }
    });
  }

  public ngOnInit() {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.EDGE = edge;
      THIS.SERVICE.GET_CONFIG().then(async config => {
        THIS.IS_AT_LEAST_OWNER = EDGE_PERMISSION.IS_USER_ALLOWED_TO_SETUP_PROTOCOL_DOWNLOAD(edge);
        THIS.CONFIG = config;
        const categorizedComponentIds: string[] = ["_appManager", "_componentManager", "_cycle", "_meta", "_power", "_sum", "_predictorManager", "_host", "_evcsSlowPowerIncreaseFilter", "_serialNumber"];
        THIS.COMPONENTS = CONFIG.LIST_ACTIVE_COMPONENTS(categorizedComponentIds, THIS.TRANSLATE);
        await THIS.SET_LATEST_SETUP_PROTOCOL_DATA();
      });
    });
  }

  public getModbusProtocol(componentId: string, type: string) {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      const request = new ComponentJsonApiRequest({ componentId: componentId, payload: new GetModbusProtocolExportXlsxRequest() });
      EDGE.SEND_REQUEST(THIS.SERVICE.WEBSOCKET, request).then(response => {
        UTILS.DOWNLOAD_XLSX(response as Base64PayloadResponse, "Modbus-" + type + "-" + EDGE.ID);
      }).catch(reason => {
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.PROFILE.ERROR_DOWNLOADING_MODBUS_PROTOCOL") + ": " + (reason as JsonrpcResponseError).ERROR.MESSAGE, "danger");
      });
    });
  }

  public getChannelExport(componentId: string) {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      const request = new ComponentJsonApiRequest({ componentId: "_componentManager", payload: new ChannelExportXlsxRequest({ componentId: componentId }) });
      EDGE.SEND_REQUEST(THIS.SERVICE.WEBSOCKET, request).then(response => {
        UTILS.DOWNLOAD_XLSX(response as Base64PayloadResponse, "ChannelExport-" + EDGE.ID + "-" + componentId);
      }).catch(reason => {
        CONSOLE.WARN(reason);
      });
    });
  }

  /**
   * Downloads the lates setup protocol
   */
  protected async downloadLatestSetupProtocol(): Promise<void> {

    if (!(THIS.LATEST_SETUP_PROTOCOL_DATA?.setupProtocolId)) {
      throw Error("Download not possible: setupProtocolId is missing");
    }

    const canExecuteDownload = THIS.PLAT_FORM_SERVICE.DEVICE_HAS_FILE_PERMISSIONS();
    if (!canExecuteDownload) {
      return;
    }

    THIS.IS_LOADING.SET(true);
    const setupProtocol: Base64PayloadResponse | null = await THIS.PLAT_FORM_SERVICE.SEND_REQUEST(new GetSetupProtocolRequest({ setupProtocolId: THIS.LATEST_SETUP_PROTOCOL_DATA.SETUP_PROTOCOL_ID.TO_STRING() }), THIS.WEBSOCKET);
    if (!setupProtocol) {
      THIS.IS_LOADING.SET(false);
      return;
    }

    const blob: Blob | null = THIS.PLAT_FORM_SERVICE.CONVERT_BASE64_TO_BLOB(setupProtocol);

    if (!blob) {
      THIS.IS_LOADING.SET(false);
      return;
    }

    const fileName = getFileName(THIS.LATEST_SETUP_PROTOCOL_DATA.SETUP_PROTOCOL_TYPE, THIS.LATEST_SETUP_PROTOCOL_DATA.CREATE_DATE, THIS.EDGE);
    THIS.PLAT_FORM_SERVICE.DOWNLOAD_AS_PDF(blob, fileName);
    THIS.IS_LOADING.SET(false);
  }

  private async setLatestSetupProtocolData() {
    THIS.IS_LOADING.SET(true);

    const edge = await THIS.SERVICE.GET_CURRENT_EDGE();
    const request = new GetLatestSetupProtocolCoreInfoRequest({ edgeId: EDGE.ID });
    const setupProtocolData: GetLatestSetupProtocolCoreInfoResponse = await THIS.WEBSOCKET.SEND_REQUEST(request) as GetLatestSetupProtocolCoreInfoResponse;

    if (!(OBJECT_UTILS.HAS_KEYS(SETUP_PROTOCOL_DATA.RESULT, ["setupProtocolId", "createDate"]))) {
      THIS.IS_LOADING.SET(false);
      return;
    }

    const result = SETUP_PROTOCOL_DATA.RESULT;
    THIS.LATEST_SETUP_PROTOCOL_DATA = { setupProtocolType: RESULT.SETUP_PROTOCOL_TYPE, setupProtocolId: RESULT.SETUP_PROTOCOL_ID, createDate: RESULT.CREATE_DATE };
    THIS.IS_LOADING.SET(false);
  }
}
