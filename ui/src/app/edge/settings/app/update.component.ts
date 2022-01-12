import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { GetAppAssistant } from './jsonrpc/getAppAssistant';
import { GetAppInstances } from './jsonrpc/getAppInstances';
import { UpdateAppInstance } from './jsonrpc/updateAppInstance';

interface MyInstance {
  instanceId: string, // uuid
  form: FormGroup,
  fields: FormlyFieldConfig[]
  properties: {},
}

@Component({
  selector: UpdateAppComponent.SELECTOR,
  templateUrl: './update.component.html'
})
export class UpdateAppComponent implements OnInit {

  private static readonly SELECTOR = "appUpdate";
  public readonly spinnerId: string = UpdateAppComponent.SELECTOR;

  public instances: MyInstance[] = [];

  private edge: Edge = null;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.startSpinner(this.spinnerId);
    let appId = this.route.snapshot.params["appId"];
    this.service.setCurrentComponent("App " + appId, this.route).then(edge => {
      this.edge = edge;
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GetAppInstances.Request({ appId: appId })
        })).then(response => {
          let instances = (response as GetAppInstances.Response).result.instances;

          edge.sendRequest(this.websocket,
            new ComponentJsonApiRequest({
              componentId: "_appManager",
              payload: new GetAppAssistant.Request({ appId: appId })
            })).then(response2 => {
              let appAssistant = GetAppAssistant.postprocess((response2 as GetAppAssistant.Response).result);

              // Create 'MyInstance' definition for each App-Instance
              this.instances = [];
              for (let instance of instances) {
                this.instances.push({
                  instanceId: instance.instanceId,
                  form: new FormGroup({}),
                  fields: appAssistant.fields,
                  properties: instance.properties,
                })
              }

              console.log(this.instances)

              this.service.stopSpinner(this.spinnerId);

            }).catch(reason => {
              console.error(reason.error);
              this.service.toast("Error while receiving App Assistant for [" + appId + "]: " + reason.error.message, 'danger');
            });

        }).catch(reason => {
          console.error(reason.error);
          this.service.toast("Error while receiving App-Instances for [" + appId + "]: " + reason.error.message, 'danger');
        });
    });
  }

  public submit(instance: MyInstance) {
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: new UpdateAppInstance.Request({
          instanceId: instance.instanceId,
          properties: instance.form.value
        })
      })).then(response => {
        instance.form.markAsPristine();
        this.service.toast("Successfully updated App", 'success');
      }).catch(reason => {
        this.service.toast("Error updating App:" + reason.error.message, 'danger');
      });
  }
}