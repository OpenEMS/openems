import { Component, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig, FormlyForm } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { JsonRpcUtils } from "src/app/shared/jsonrpc/jsonrpcutils";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Role } from "src/app/shared/type/role";
import { Edge, Service, Websocket } from "../../../shared/shared";
import { GetNetworkConfigRequest } from "./getNetworkConfigRequest";
import { GetNetworkConfigResponse } from "./getNetworkConfigResponse";
import { GetNetworkInfoRequest } from "./getNetworkInfoRequest";
import { GetNetworkInfoResponse } from "./getNetworkInfoResponse";
import { SetNetworkConfigRequest } from "./setNetworkConfigRequest";
import { InterfaceForm, InterfaceModel, IpAddress, NetworkConfig, NetworkInfo, NetworkInterface, NetworkUtils } from "./shared";

@Component({
  selector: NETWORK_COMPONENT.SELECTOR,
  templateUrl: "./NETWORK.COMPONENT.HTML",
  standalone: false,
})
export class NetworkComponent implements OnInit {

  private static readonly SELECTOR: string = "network";
  private static readonly ETH_0: string = "eth0";
  private static readonly STATIC_LABEL: string = "static";
  private static readonly NO_LABEL: string = "";

  public edge: Edge | null = null;
  protected forms: InterfaceForm[] = [];
  protected ipRegex: RegExp = /^(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}\/(?:3[0-2]|[0-2]?[0-9])$/;

  constructor(
    private translate: TranslateService,
    private service: Service,
    private websocket: Websocket,
  ) { }

  /**
 * Gets the dynamic ip with subnetmaks from a given network interface
 *
 * @param networkInfo the network info
 * @param networkInterface the network interface
 * @returns a dynamic ip address with subnetmask, if determined by "dynamic", else null
 */
  public static getDynamicIpWithSubnetMask(networkInfo: NetworkInfo, networkInterface: string): string | null {
    const networkInfoInterface = NETWORK_INFO.NETWORK_INTERFACES.FIND(el => EL.HARDWARE_INTERFACE === networkInterface);
    if (networkInfoInterface) {
      const dynamicIp = NETWORK_INFO_INTERFACE.IPS.FIND(el => "dynamic" in el);
      if (dynamicIp) {
        return DYNAMIC_IP.ADDRESS + "/" + DYNAMIC_IP.SUBNETMASK;
      }
    }
    return null;
  }

  public ngOnInit() {
    THIS.INITIALIZE_COMPONENT();
  }

  public submit(iface: InterfaceForm): void {
    if (!IFACE.FORM_GROUP.VALID) {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.NETWORK.MANDATORY_FIELDS"), "danger");
      return;
    }

    // Adds the static addresses entered in form field "Statische IP-Adressen hinzufügen" to addressJson in json format.
    const addressJson: IpAddress[] = THIS.BUILD_ADDRESS_JSON(iface);
    const request: NetworkConfig = THIS.BUILD_REQUEST(iface, addressJson);
    const interfaceName: string = IFACE.NAME === NetworkComponent.ETH_0 ? NetworkComponent.ETH_0 : IFACE.NAME;

    THIS.SEND_REQUEST(interfaceName, request);
  }

  /**
   * Hide expression dosent work with custom type 'repeat'.
   * So this is the workaround for that functionality.
   *
   * @param form the form fields that need to be changed.
   */
  protected hideOrShowFields(form: FormlyForm): void {

    const addressField: FormlyFieldConfig | undefined = FORM.FIELDS.FIND(element => ELEMENT.KEY == "addressesList");
    const linkLocalAddressField: FormlyFieldConfig | undefined = FORM.FIELDS.FIND(element => ELEMENT.KEY == "linkLocalAddressing");
    const metric: FormlyFieldConfig | undefined = FORM.FIELDS.FIND(element => ELEMENT.KEY == "metric");
    const advancedMode: boolean = FORM.MODEL.ADVANCED_MODE;

    if (addressField) { ADDRESS_FIELD.HIDE = !advancedMode; }
    if (linkLocalAddressField) { LINK_LOCAL_ADDRESS_FIELD.HIDE = !advancedMode; }
    if (metric) { METRIC.HIDE = !advancedMode; }
  }

  private async initializeComponent() {
    try {
      THIS.EDGE = await THIS.SERVICE.GET_CURRENT_EDGE();
      if (THIS.EDGE) {
        const response: GetNetworkConfigResponse = await THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, new ComponentJsonApiRequest({ componentId: "_host", payload: new GetNetworkConfigRequest() })) as GetNetworkConfigResponse;
        const getNetworkInfoReq = new GetNetworkInfoRequest();
        const [_err, networkInfoResponse] = await JSON_RPC_UTILS.HANDLE_OR_ELSE<GetNetworkInfoResponse>(
          THIS.EDGE.SEND_REQUEST<GetNetworkInfoResponse>(THIS.WEBSOCKET, new ComponentJsonApiRequest({ componentId: "_host", payload: getNetworkInfoReq })), GET_NETWORK_INFO_RESPONSE.EMPTY(GET_NETWORK_INFO_REQ.ID));
        THIS.HANDLE_NETWORK_RESPONSES(response, networkInfoResponse);
      }
    } catch (reason: any) {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.NETWORK.ERROR_READING") + reason?.error?.message, "danger");
    }
  }

  private handleNetworkResponses(networkConfigRes: GetNetworkConfigResponse, networkInfoRes: GetNetworkInfoResponse) {
    const result: NetworkConfig = NETWORK_CONFIG_RES.RESULT;

    if (THIS.EDGE) {
      const isAdmin: boolean = THIS.EDGE.ROLE_IS_AT_LEAST(ROLE.ADMIN);
      for (const name of OBJECT.KEYS(RESULT.INTERFACES)) {
        if (isAdmin || name === NetworkComponent.ETH_0) {
          const dynamicIpWithSubnetmask = NETWORK_COMPONENT.GET_DYNAMIC_IP_WITH_SUBNET_MASK(NETWORK_INFO_RES.RESULT, name);
          // Display all interfaces available for user with role Admin.
          // Display only eth0 (LAN) interface for user with role less than Admin.
          THIS.GENERATE_INTERFACE(name, RESULT.INTERFACES[name], dynamicIpWithSubnetmask);
        }
      }
    }
  }

  /**
   * Builds an array of IP address objects from the provided list of addresses extracted from the interface form data.
   * Any IP address entered in the array will be labeled with an empty string.
   *
   * Example:
   * ```typescript
   *
   * Input:
   * const addressList = ['192.168.1.50/24', '10.0.0.1/16'];
   *
   * Result:
   * [
   *  { label: '', ip: '192.168.1.50', subnetmask: '255.255.255.0' },
   *  { label: '', ip: '10.0.0.1', subnetmask: '255.255.0.0' }
   * ]
   * ```
   *
   * @param iface The {@link InterfaceForm} data containing the list of IP addresses.
   * @returns An array of {@link IpAddress} objects with labels, IP addresses, and subnet masks.
   */
  private buildAddressJson(iface: InterfaceForm): IpAddress[] {
    const addressJson: IpAddress[] = [];

    if (IFACE.MODEL.ADDRESSES_LIST) {
      for (const addr of IFACE.MODEL.ADDRESSES_LIST) {
        if (!THIS.IP_REGEX.TEST(addr)) {
          THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.NETWORK.VALID_ADDRESS_WARNING"), "danger");
          return [];
        }
        const [address, subnet] = ADDR.SPLIT("/");
        const subnetmask = NETWORK_UTILS.GET_SUBNETMASK_AS_STRING(NUMBER.PARSE_INT(subnet));

        ADDRESS_JSON.PUSH({
          label: NetworkComponent.NO_LABEL,
          address: address,
          subnetmask: subnetmask,
        });
      }
    }

    return addressJson;
  }

  /**
   * Builds the request payload for setting network configuration based on the provided interface form data and address information.
   *
   * @param iface The {@link InterfaceForm} data containing the network configuration details.
   * @param addressJson An array of {@link IpAddress} extracted from the form data.
   * @returns The request payload object containing the network configuration for the specified interface.
   */
  private buildRequest(iface: InterfaceForm, addressJson: IpAddress[]): NetworkConfig {
    const request: NetworkConfig = { interfaces: {} };

    // Unset Gateway and DNS if DHCP is activated
    if (IFACE.MODEL.DHCP) {
      IFACE.MODEL.GATEWAY = null;
      IFACE.MODEL.DNS = null;
      IFACE.MODEL.IP = null;
      IFACE.MODEL.SUBNETMASK = null;
    } else {
      // Ip address and subnetmask entered from regular form will be labelled as 'static'.
      const ip = IFACE.MODEL.IP;
      const subnetmask = IFACE.MODEL.SUBNETMASK;
      if (ip && subnetmask) {
        ADDRESS_JSON.PUSH({
          label: NetworkComponent.STATIC_LABEL,
          address: ip,
          subnetmask: subnetmask,
        });
      }
    }

    REQUEST.INTERFACES[IFACE.NAME] = {
      ...IFACE.MODEL,
      addresses: addressJson,
    };

    return request;
  }

  /**
   * Sends the request to edge with the configuration.
   *
   * @param interfaceName The name of the interface. for eg, 'enx', 'eth0'..
   * @param request {@link SetNetworkConfigRequest} payload object containing the network configuration for the specified interface.
   */
  private async sendRequest(interfaceName: string, request: NetworkConfig): Promise<void> {
    try {
      await THIS.EDGE?.sendRequest(THIS.WEBSOCKET, new ComponentJsonApiRequest({
        componentId: "_host",
        payload: new SetNetworkConfigRequest(request),
      }));
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.NETWORK.SUCCESS_UPDATE") + `[${interfaceName}].`, "success");
    } catch (reason: any) {
      THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.NETWORK.ERROR_UPDATING") + `[${interfaceName}].` + reason?.error?.message, "danger");
    }
  }


  /**
   * Generates the interface configuration based on the provided name and source data.
   *
   * @param name The name of the interface to be displayed.
   * @param source The data containing values for the individual {@link NetworkInterface}.
   */
  private generateInterface(name: string, source: NetworkInterface, dynamicIp: string | null): void {
    const addressArray: string[] = [];
    const interfaceModel: InterfaceModel & { dynamicIp: string | null } = { ...source, dynamicIp: null };

    // extracts the addresses json values to form values.
    if (SOURCE.ADDRESSES) {
      for (const address of SOURCE.ADDRESSES) {
        if (ADDRESS.LABEL == NetworkComponent.STATIC_LABEL) {
          INTERFACE_MODEL.IP = ADDRESS.ADDRESS;
          INTERFACE_MODEL.SUBNETMASK = ADDRESS.SUBNETMASK;
        } else {
          // Converts ip:"192.168.1.50" and subnetmask:"255.255.255.0" -> ["192.168.1.50/24"]
          const cidr: number = NETWORK_UTILS.GET_CIDR_FROM_SUBNETMASK(ADDRESS.SUBNETMASK);
          const ip: string = ADDRESS.ADDRESS.CONCAT("/" + CIDR.TO_STRING());
          ADDRESS_ARRAY.PUSH(ip);
        }
      }
    }

    INTERFACE_MODEL.ADDRESSES_LIST = addressArray;

    // Only found if edge version at least TODO
    INTERFACE_MODEL.DYNAMIC_IP = dynamicIp;
    // Generates the form.
    THIS.FORMS.PUSH({
      name: name,
      fields: THIS.FILL_FIELDS(addressArray),
      formGroup: new FormGroup({}),
      model: interfaceModel,
    });
  }

  /**
   * Fills the form fields based on the provided list of IP addresses.
   *
   * @param addressArray - The array of of IP addresses extracted from the form data.
   * @returns An array of {@link FormlyFieldConfig} representing the filled form fields.
   *
   */
  private fillFields(addressArray: string[]): FormlyFieldConfig[] {
    const fields: FormlyFieldConfig[] = [
      {
        key: "dhcp",
        type: "help-popover-label-with-description-and-checkbox",
        defaultValue: true,
        templateOptions: {
          label: THIS.TRANSLATE.INSTANT("EDGE.NETWORK.DHCP.ADDRESS"),
        },
        expressions: {
          "PROPS.DESCRIPTION": (field) => FIELD.MODEL.DYNAMIC_IP,
          "PROPS.HELP_MSG": (field) => FIELD.MODEL.DYNAMIC_IP ? THIS.TRANSLATE.INSTANT("EDGE.NETWORK.DHCP.INFO") : null,
        },
      },
      {
        hideExpression: "MODEL.DHCP",
        key: "ip",
        type: "input",
        resetOnHide: false,
        templateOptions: {
          label: THIS.TRANSLATE.INSTANT("EDGE.NETWORK.IP_ADDRESS"),
          placeholder: "Z.B. 192.168.0.50",
          required: true,
        },
        validators: {
          validation: ["ip"],
        },
      },
      {
        hideExpression: "MODEL.DHCP",
        key: "subnetmask",
        type: "input",
        resetOnHide: false,
        templateOptions: {
          label: THIS.TRANSLATE.INSTANT("EDGE.NETWORK.SUBNETMASK"),
          placeholder: "Z.B. 255.255.255.0",
          required: true,
        },
        validators: {
          validation: ["subnetmask"],
        },
      },
      {
        hideExpression: "MODEL.DHCP",
        key: "gateway",
        type: "input",
        resetOnHide: false,
        templateOptions: {
          label: "Gateway",
          placeholder: "Z.B. 192.168.0.1",
          required: true,
        },
        validators: {
          validation: ["ip"],
        },
      },
      {
        hideExpression: "MODEL.DHCP",
        key: "dns",
        type: "input",
        resetOnHide: false,
        templateOptions: {
          label: "DNS-Server",
          placeholder: "Z.B. 192.168.0.1",
          required: true,
        },
        validators: {
          validation: ["ip"],
        },
      },
      {
        key: "linkLocalAddressing",
        type: "checkbox",
        resetOnHide: false,
        templateOptions: {
          label: "Link-Local Address (z. B. 169.254.XXX.XXX)",
        },
        hide: true,
      },
      {
        hide: true,
        key: "addressesList",
        type: "repeat",
        resetOnHide: false,
        defaultValue: addressArray,
        templateOptions: {
          label: THIS.TRANSLATE.INSTANT("EDGE.NETWORK.ADD_IP"),
        },
        fieldArray: {
          type: "input",
          resetOnHide: false,
        },
      },
      {
        hide: true,
        key: "metric",
        type: "input",
        resetOnHide: false,
        templateOptions: {
          label: "Metric",
          placeholder: "Z.B. 512, 1024 ...",
        },
        defaultValue: 1024,
        parsers: [Number],
      },
    ];

    return fields;
  }
}

