import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { CategorizedComponents } from "src/app/shared/components/edge/edgeconfig";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Base64PayloadResponse } from "src/app/shared/jsonrpc/response/base64PayloadResponse";
import { environment } from "../../../../environments";
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "../../../shared/shared";
import { ChannelExportXlsxRequest } from "./channelexport/channelExportXlsxRequest";
import { GetModbusProtocolExportXlsxRequest } from "./modbusapi/getModbusProtocolExportXlsxRequest";

@Component({
  selector: ProfileComponent.SELECTOR,
  templateUrl: "./profile.component.html",
  standalone: false,
})
export class ProfileComponent implements OnInit {

  private static readonly SELECTOR = "profile";

  public environment = environment;

  public edge: Edge | null = null;
  public config: EdgeConfig | null = null;
  public subscribedChannels: ChannelAddress[] = [];

  public components: CategorizedComponents[] | null = null;

  constructor(
    private service: Service,
    private route: ActivatedRoute,
    public popoverController: PopoverController,
    private translate: TranslateService,
  ) { }

  public ngOnInit() {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.config = config;
        const categorizedComponentIds: string[] = ["_appManager", "_componentManager", "_cycle", "_meta", "_power", "_sum", "_predictorManager", "_host", "_evcsSlowPowerIncreaseFilter"];
        this.components = config.listActiveComponents(categorizedComponentIds);
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

}
