import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Role } from 'src/app/shared/type/role';
import { Edge, Service, Websocket } from '../../../shared/shared';
import { GetNetworkConfigRequest } from './getNetworkConfigRequest';
import { GetNetworkConfigResponse } from './getNetworkConfigResponse';
import { SetNetworkConfigRequest } from './setNetworkConfigRequest';
import { IpAddress } from './shared';

export type InterfaceForm = {
  name: string,
  formGroup: FormGroup,
  model: any,
  fields: FormlyFieldConfig[]
};

@Component({
  selector: NetworkComponent.SELECTOR,
  templateUrl: './network.component.html'
})
export class NetworkComponent implements OnInit {

  private static readonly SELECTOR = 'network';

  public edge: Edge | null = null;
  protected forms: InterfaceForm[] = [];
  protected ipRegex: RegExp = /^(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}\/(?:3[0-2]|[0-2]?[0-9])$/;

  constructor(
    private translate: TranslateService,
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  public ngOnInit() {

    this.service.setCurrentComponent({ languageKey: 'Edge.Config.Index.networkConfiguration' }, this.route).then(edge => {
      this.edge = edge;

      this.edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({ componentId: '_host', payload: new GetNetworkConfigRequest() })).then(response => {

          const result = (response as GetNetworkConfigResponse).result;
          for (let name of Object.keys(result.interfaces)) {
            const iface = result.interfaces[name];

            if (this.edge.roleIsAtLeast(Role.ADMIN)) {
              // Display all interfaces available for user with role Admin.
              this.generateInterface(name, iface);
            } else {
              // Display only eth0 (LAN) interface for user with role less than Admin.
              if (name === 'eth0') {
                this.generateInterface(name, iface);
              }
            }
          }
        }).catch(reason => {
          this.service.toast(this.translate.instant('Edge.Network.errorReading') + reason.error.message, 'danger');
        });
    });
  }

  public submit(iface: InterfaceForm): void {
    if (!iface.formGroup.valid) {
      this.service.toast(this.translate.instant('Edge.Network.mandatoryFields'), 'danger');
      return;
    }

    // Adds the static addresses entered in form field "Statische IP-Adressen hinzufügen" to addressJson in json format.
    const addressJson: IpAddress[] = [];

    // Converts ["192.168.1.50/24"] -> {label: " ''/'static' ", ip: "192.168.1.50", subnetmask: "255.255.255.0" }
    // Any ip address entered in the array("Statische IP-Adressen hinzufügen") will be labeled with emty string.
    for (const addr of iface.model.addressesList) {
      if (this.ipRegex.test(addr)) {
        var ip = addr.split('/');
        var subnetmask = this.getSubnetmaskAsString(ip[1]);
      } else {
        this.service.toast(this.translate.instant('Edge.Network.validAddressWarning'), 'danger');
        return;
      }

      addressJson.push({
        label: '', //TODO with specific labels with specific systems.
        address: ip[0],
        subnetmask: subnetmask
      });
    }

    // Unset Gateway and DNS if DHCP is activated
    if (iface.model.dhcp) {
      iface.model.gateway = null;
      iface.model.dns = null;
      iface.model.ip = null;
      iface.model.subnetmask = null;
    } else {
      // Ip address and subnetmask entered from regular form will be labelled as 'static'. 
      addressJson.push({
        label: 'static',
        address: iface.model.ip,
        subnetmask: iface.model.subnetmask
      });
    }

    // updates the addresses array with latest values.
    iface.model.addresses = addressJson;

    let request = {
      interfaces: {}
    };
    request.interfaces[iface.name] = iface.model;
    const interfaceName = iface.name === 'eth0' ? 'eth0' : iface.name;

    // Sends the request to edge with the configuration.
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: '_host', payload: new SetNetworkConfigRequest(request)
      })).then(response => {
        this.service.toast(this.translate.instant('Edge.Network.successUpdate') + '[' + interfaceName + '].', 'success');
      }).catch(reason => {
        this.service.toast(this.translate.instant('Edge.Network.errorUpdating') + '[' + interfaceName + ']:' + reason.error.message, 'danger');
      });
  }

  /**
   * Hide expression dosent work with custom type 'repeat'. 
   * So this is the workaround for that functionality.
   * 
   * @param index index of the form from form array.
   * @param value boolean value respresenting to show or hide.
   */
  protected hideOrShowFields(index: number, value: boolean): void {
    if (this.forms[index] != null) {
      const addressField = this.forms[index].fields.find(element => element.key == 'addressesList');
      const linkLocalAddressField = this.forms[index].fields.find(element => element.key == 'linkLocalAddressing');

      addressField.hide = !value;
      linkLocalAddressField.hide = !value;
    }
  }

  /**
   * Converts the subnetmask to a string address.
   * 
   * e. g. Converts "24" to "255.255.255.0"
   * 
   * @param cidr the CIDR
   * @returns the subnetmask as a string
   */
  protected getSubnetmaskAsString(subnetmask: number): string {
    var result = [];
    for (var i = 0; i < 4; i++) {
      var n = Math.min(subnetmask, 8);
      result.push(256 - Math.pow(2, 8 - n));
      subnetmask -= n;
    }
    return result.join('.');
  }

  /**
   * Generates the interface for the individual networks.
   * 
   * @param name string to display on the individual network interface window.
   * @param source contains values for individual network.
   */
  private generateInterface(name: string, source: any): void {
    let addressArray: string[] = [];

    // extracts the addresses json values to form values.
    if (source.addresses) {
      for (const address of source.addresses) {
        if (address.label == 'static') {
          source.ip = address.address;
          source.subnetmask = address.subnetmask;
        } else {
          // Converts ip:"192.168.1.50" and subnetmask:"255.255.255.0" -> ["192.168.1.50/24"]
          const cidr = address.subnetmask.split('.').map(Number).map(part => (part >>> 0).toString(2)).join('').split('1').length - 1;
          const ip: string = address.address.concat('/' + cidr.toString());
          addressArray.push(ip);
        }
      }
    }

    // Generates the form.
    this.forms.push({
      name: name,
      fields: this.fillFields(addressArray),
      formGroup: new FormGroup({}),
      model: source
    });
  }

  /**
   * fills the fields with source.
   * 
   * @returns FormlyFieldConfig[].
   */
  private fillFields(addressArray: String[]): FormlyFieldConfig[] {
    const fields: FormlyFieldConfig[] = [
      {
        key: 'dhcp',
        type: 'checkbox',
        defaultValue: true,
        templateOptions: {
          label: 'DHCP'
        }
      },
      {
        hideExpression: 'model.dhcp',
        key: 'ip',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: this.translate.instant('Edge.Network.ipAddress'),
          placeholder: 'z.B. 192.168.0.50',
          required: true
        },
        validators: {
          validation: ['ip']
        }
      },
      {
        hideExpression: 'model.dhcp',
        key: 'subnetmask',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: this.translate.instant('Edge.Network.subnetmask'),
          placeholder: 'z.B. 255.255.255.0',
          required: true
        },
        validators: {
          validation: ['subnetmask']
        }
      },
      {
        hideExpression: 'model.dhcp',
        key: 'gateway',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: 'Gateway',
          placeholder: 'z.B. 192.168.0.1',
          required: true
        },
        validators: {
          validation: ['ip']
        }
      },
      {
        hideExpression: 'model.dhcp',
        key: 'dns',
        type: 'input',
        resetOnHide: false,
        templateOptions: {
          label: 'DNS-Server',
          placeholder: 'z.B. 192.168.0.1',
          required: true
        },
        validators: {
          validation: ['ip']
        }
      },
      {
        key: 'linkLocalAddressing',
        type: 'checkbox',
        resetOnHide: false,
        templateOptions: {
          label: 'Link-Local Address (z. B. 169.254.XXX.XXX)'
        },
        hide: true
      },
      {
        hide: true,
        key: 'addressesList',
        type: 'repeat',
        resetOnHide: false,
        defaultValue: addressArray,
        templateOptions: {
          label: this.translate.instant('Edge.Network.addIP')
        },
        fieldArray: {
          type: 'input',
          resetOnHide: false
        }
      }
    ];

    return fields;
  }
}