import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Edge, Service, Websocket } from '../../../shared/shared';
import { GetNetworkConfigRequest } from '../network/getNetworkConfigRequest';
import { GetNetworkConfigResponse } from './getNetworkConfigResponse';
import { SetNetworkConfigRequest } from './setNetworkConfigRequest';
import { NetworkInterface } from './shared';

export interface InterfaceForm {
  name: string,
  form: FormGroup,
  model: NetworkInterface,
  fields: FormlyFieldConfig[]
};

@Component({
  selector: NetworkOldComponent.SELECTOR,
  templateUrl: './network.old.component.html',
})
export class NetworkOldComponent implements OnInit {

  private static readonly SELECTOR = "networkOld";

  public edge: Edge = null;
  public interfaces: InterfaceForm[] = [];

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent("Netzwerk Konfiguration" /* TODO translate */, this.route).then(edge => {
      this.edge = edge;

      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({ componentId: "_host", payload: new GetNetworkConfigRequest() })).then(response => {
          let result = (response as GetNetworkConfigResponse).result;
          for (let name of Object.keys(result.interfaces)) {
            let iface = result.interfaces[name];
            this.interfaces.push(this.generateInterface(name, iface));
          }
        }).catch(reason => {
          this.service.toast("Error reading current network configuration:" + reason.error.message, 'danger');
        });
    });
  }

  public submit(iface: InterfaceForm) {
    // Unset Gateway and DNS if DHCP is activated
    if (iface.model.dhcp) {
      iface.model.gateway = null;
      iface.model.dns = null;
    }

    let request = {
      interfaces: {},
    };
    request.interfaces[iface.name] = iface.model;

    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_host", payload: new SetNetworkConfigRequest(request),
      })).then(response => {
        this.service.toast("Successfully updated network configuration for [" + iface.name + "].", 'success');
      }).catch(reason => {
        this.service.toast("Error updating [" + iface.name + "]:" + reason.error.message, 'danger');
      });
  }

  private generateInterface(name: string, source: NetworkInterface) {
    return {
      name: name,
      form: new FormGroup({}),
      model: source,
      options: {},
      fields: [
        {
          key: 'dhcp',
          type: 'checkbox',
          templateOptions: {
            label: 'DHCP',
          },
        },
        {
          hideExpression: 'model.dhcp',
          key: 'gateway',
          type: 'input',
          templateOptions: {
            label: 'Gateway',
            placeholder: 'z.B. "192.168.0.1"',
          },
        },
        {
          hideExpression: 'model.dhcp',
          key: 'dns',
          type: 'input',
          templateOptions: {
            label: 'DNS',
            input: 'z.B. "192.168.0.1"',
          },
        },
        {
          key: 'linkLocalAddressing',
          type: 'checkbox',
          templateOptions: {
            label: 'Link-Local Address (z. B. "169.254.XXX.XXX")',
          },
        },
        {
          key: 'addresses',
          type: 'repeat',
          templateOptions: {
            label: 'Statische IP-Adresse hinzufügen',
          },
          fieldArray: {
            type: 'input',
          },
        },
      ],
    };
  }
}
