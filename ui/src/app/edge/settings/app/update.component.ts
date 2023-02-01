import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { DeleteAppInstance } from './jsonrpc/deleteAppInstance';
import { GetAppAssistant } from './jsonrpc/getAppAssistant';
import { GetAppInstances } from './jsonrpc/getAppInstances';
import { UpdateAppInstance } from './jsonrpc/updateAppInstance';

interface MyInstance {
  instanceId: string, // uuid
  form: FormGroup,
  isDeleting: boolean,
  isUpdateting: boolean,
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

  protected instances: MyInstance[] = [];
  protected forms: FormGroup[] = [];

  private edge: Edge | null = null;

  protected appName: string | null = null;

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  public ngOnInit() {
    this.service.startSpinner(this.spinnerId);
    let appId = this.route.snapshot.params["appId"];
    let appName = this.route.snapshot.queryParams['name'];
    this.service.setCurrentComponent(appName, this.route).then(edge => {
      this.edge = edge;
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GetAppInstances.Request({ appId: appId })
        })).then(response => {
          let recInstances = (response as GetAppInstances.Response).result.instances;

          edge.sendRequest(this.websocket,
            new ComponentJsonApiRequest({
              componentId: "_appManager",
              payload: new GetAppAssistant.Request({ appId: appId })
            })).then(response2 => {
              let appAssistant = GetAppAssistant.postprocess((response2 as GetAppAssistant.Response).result);
              this.appName = appAssistant.name;
              this.instances = [];
              for (let instance of recInstances) {
                let form = new FormGroup({})
                this.forms.push(form)
                const clonedFields = [];
                appAssistant.fields.forEach(val => clonedFields.push(Object.assign({}, val)));
                // insert alias field into fields
                let aliasField = { key: "ALIAS", type: "input", templateOptions: { label: "Alias" }, defaultValue: instance.alias };
                clonedFields.splice(0, 0, aliasField)
                this.instances.push({
                  instanceId: instance.instanceId,
                  form: form,
                  isDeleting: false,
                  isUpdateting: false,
                  fields: clonedFields,
                  properties: instance.properties,
                })
              }

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

  protected submit(instance: MyInstance) {
    instance.isUpdateting = true
    // remove alias field from properties
    let alias = instance.form.value["ALIAS"]
    const clonedFields = {};
    for (let item in instance.form.value) {
      if (item != "ALIAS") {
        clonedFields[item] = instance.form.value[item]
      }
    }
    instance.form.markAsPristine();
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: new UpdateAppInstance.Request({
          instanceId: instance.instanceId,
          alias: alias,
          properties: clonedFields
        })
      })).then(response => {
        var res = (response as UpdateAppInstance.Response);

        if (res.result.warnings && res.result.warnings.length > 0) {
          this.service.toast(res.result.warnings.join(";"), 'warning');
        } else {
          this.service.toast("Successfully updated App", 'success');
        }
        instance.properties = res.result.instance.properties
        instance.properties["ALIAS"] = res.result.instance.alias
        instance.isUpdateting = false
      }).catch(reason => {
        this.service.toast("Error updating App:" + reason.error.message, 'danger');
        instance.isUpdateting = false
      });
  }

  protected delete(instance: MyInstance) {
    instance.isDeleting = true
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: new DeleteAppInstance.Request({
          instanceId: instance.instanceId
        })
      })).then(response => {
        this.instances.splice(this.instances.indexOf(instance), 1)
        this.service.toast("Successfully deleted App", 'success');
      }).catch(reason => {
        this.service.toast("Error deleting App:" + reason.error.message, 'danger');
      }).finally(() => {
        instance.isDeleting = false
      })
  }
}