import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig, FormlyForm } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Role } from 'src/app/shared/type/role';
import { Edge, Service, Websocket } from '../../../shared/shared';
import { GetNetworkConfigRequest } from './getNetworkConfigRequest';
import { GetNetworkConfigResponse } from './getNetworkConfigResponse';
import { SetNetworkConfigRequest } from './setNetworkConfigRequest';
import { InterfaceForm, InterfaceModel, IpAddress, NetworkConfig, NetworkInterface, NetworkUtils } from './shared';

@Component({
  selector: NetworkComponent.SELECTOR,
  templateUrl: './network.component.html',
})
export class NetworkComponent implements OnInit {

  private static readonly SELECTOR: string = 'network';
  private static readonly ETH_0: string = 'eth0';
  private static readonly STATIC_LABEL: string = 'static';
  private static readonly NO_LABEL: string = '';

  public edge: Edge | null = null;
  protected forms: InterfaceForm[] = [];
  protected ipRegex: RegExp = /^(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}\/(?:3[0-2]|[0-2]?[0-9])$/;

  constructor(
    private translate: TranslateService,
    private service: Service,
    private websocket: Websocket,
  ) { }

  public ngOnInit() {
    this.initializeComponent();
  }

  private async initializeComponent() {
    try {
      this.edge = await this.service.getCurrentEdge();
      if (this.edge) {
        const response: GetNetworkConfigResponse = await this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({ componentId: '_host', payload: new GetNetworkConfigRequest() })) as GetNetworkConfigResponse;
        this.handleNetworkConfigResponse(response);
      }
    } catch (reason: any) {
      this.service.toast(this.translate.instant('Edge.Network.errorReading') + reason?.error?.message ?? 'Unknown error', 'danger');
    }
  }

  private handleNetworkConfigResponse(response: GetNetworkConfigResponse) {
    const result: NetworkConfig = response.result;
    if (this.edge) {
      const isAdmin: boolean = this.edge.roleIsAtLeast(Role.ADMIN);
      for (const name of Object.keys(result.interfaces)) {
        if (isAdmin || name === NetworkComponent.ETH_0) {
          // Display all interfaces available for user with role Admin.
          // Display only eth0 (LAN) interface for user with role less than Admin.
          this.generateInterface(name, result.interfaces[name]);
        }
      }
    }
  }

  public submit(iface: InterfaceForm): void {
    if (!iface.formGroup.valid) {
      this.service.toast(this.translate.instant('Edge.Network.mandatoryFields'), 'danger');
      return;
    }

    // Adds the static addresses entered in form field "Statische IP-Adressen hinzufügen" to addressJson in json format.
    const addressJson: IpAddress[] = this.buildAddressJson(iface);
    const request: NetworkConfig = this.buildRequest(iface, addressJson);
    const interfaceName: string = iface.name === NetworkComponent.ETH_0 ? NetworkComponent.ETH_0 : iface.name;

    this.sendRequest(interfaceName, request);
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

    if (iface.model.addressesList) {
      for (const addr of iface.model.addressesList) {
        if (!this.ipRegex.test(addr)) {
          this.service.toast(this.translate.instant('Edge.Network.validAddressWarning'), 'danger');
          return [];
        }
        const [address, subnet] = addr.split('/');
        const subnetmask = NetworkUtils.getSubnetmaskAsString(Number.parseInt(subnet));

        addressJson.push({
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
    if (iface.model.dhcp) {
      iface.model.gateway = null;
      iface.model.dns = null;
      iface.model.ip = null;
      iface.model.subnetmask = null;
    } else {
      // Ip address and subnetmask entered from regular form will be labelled as 'static'.
      const ip = iface.model.ip;
      const subnetmask = iface.model.subnetmask;
      if (ip && subnetmask) {
        addressJson.push({
          label: NetworkComponent.STATIC_LABEL,
          address: ip,
          subnetmask: subnetmask,
        });
      }
    }

    request.interfaces[iface.name] = {
      ...iface.model,
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
      await this.edge?.sendRequest(this.websocket, new ComponentJsonApiRequest({
        componentId: '_host',
        payload: new SetNetworkConfigRequest(request),
      }));
      this.service.toast(this.translate.instant('Edge.Network.successUpdate') + `[${interfaceName}].`, 'success');
    } catch (reason: any) {
      this.service.toast(this.translate.instant('Edge.Network.errorUpdating') + `[${interfaceName}].` + reason?.error?.message ?? 'Unknown error', 'danger');
    }
  }

  /**
   * Hide expression dosent work with custom type 'repeat'.
   * So this is the workaround for that functionality.
   *
   * @param form the form fields that need to be changed.
   */
  protected hideOrShowFields(form: FormlyForm): void {

    const addressField: FormlyFieldConfig | undefined = form.fields.find(element => element.key == 'addressesList');
    const linkLocalAddressField: FormlyFieldConfig | undefined = form.fields.find(element => element.key == 'linkLocalAddressing');
    const metric: FormlyFieldConfig | undefined = form.fields.find(element => element.key == 'metric');
    const advancedMode: boolean = form.model.advancedMode;

    if (addressField) { addressField.hide = !advancedMode; }
    if (linkLocalAddressField) { linkLocalAddressField.hide = !advancedMode; }
    if (metric) { metric.hide = !advancedMode; }
  }

  /**
   * Generates the interface configuration based on the provided name and source data.
   *
   * @param name The name of the interface to be displayed.
   * @param source The data containing values for the individual {@link NetworkInterface}.
   */
  private generateInterface(name: string, source: NetworkInterface): void {
    const addressArray: string[] = [];
    const interfaceModel: InterfaceModel = { ...source };

    // extracts the addresses json values to form values.
    if (source.addresses) {
      for (const address of source.addresses) {
        if (address.label == NetworkComponent.STATIC_LABEL) {
          interfaceModel.ip = address.address;
          interfaceModel.subnetmask = address.subnetmask;
        } else {
          // Converts ip:"192.168.1.50" and subnetmask:"255.255.255.0" -> ["192.168.1.50/24"]
          const cidr: number = NetworkUtils.getCidrFromSubnetmask(address.subnetmask);
          const ip: string = address.address.concat('/' + cidr.toString());
          addressArray.push(ip);
        }
      }
    }

    interfaceModel.addressesList = addressArray;

    // Generates the form.
    this.forms.push({
      name: name,
      fields: this.fillFields(addressArray),
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
        key: 'dhcp',
        type: 'checkbox',
        defaultValue: true,
        templateOptions: {
          label: 'DHCP',
        },
      },
      {
        hideExpression: 'model.dhcp',
        key: 'ip',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: this.translate.instant('Edge.Network.ipAddress'),
          placeholder: 'z.B. 192.168.0.50',
          required: true,
        },
        validators: {
          validation: ['ip'],
        },
      },
      {
        hideExpression: 'model.dhcp',
        key: 'subnetmask',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: this.translate.instant('Edge.Network.subnetmask'),
          placeholder: 'z.B. 255.255.255.0',
          required: true,
        },
        validators: {
          validation: ['subnetmask'],
        },
      },
      {
        hideExpression: 'model.dhcp',
        key: 'gateway',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: 'Gateway',
          placeholder: 'z.B. 192.168.0.1',
          required: true,
        },
        validators: {
          validation: ['ip'],
        },
      },
      {
        hideExpression: 'model.dhcp',
        key: 'dns',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: 'DNS-Server',
          placeholder: 'z.B. 192.168.0.1',
          required: true,
        },
        validators: {
          validation: ['ip'],
        },
      },
      {
        key: 'linkLocalAddressing',
        type: 'checkbox',
        resetOnHide: false,
        templateOptions: {
          label: 'Link-Local Address (z. B. 169.254.XXX.XXX)',
        },
        hide: true,
      },
      {
        hide: true,
        key: 'addressesList',
        type: 'repeat',
        resetOnHide: false,
        defaultValue: addressArray,
        templateOptions: {
          label: this.translate.instant('Edge.Network.addIP'),
        },
        fieldArray: {
          type: 'input',
          resetOnHide: false,
        },
      },
      {
        hide: true,
        key: 'metric',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: 'Metric',
          placeholder: 'z.B. 512, 1024 ...',
        },
        defaultValue: 1024,
        parsers: [Number],
      },
    ];

    return fields;
  }
}
