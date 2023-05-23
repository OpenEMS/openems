import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
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
  isUpdating: boolean,
  fields: FormlyFieldConfig[]
  properties: {},
}

@Component({
  selector: UpdateAppComponent.SELECTOR,
  templateUrl: './update.component.html'
})
export class UpdateAppComponent implements OnInit {

  private static readonly SELECTOR = 'app-update';
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
    private router: Router,
    private translate: TranslateService,
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
          componentId: '_appManager',
          payload: new GetAppInstances.Request({ appId: appId })
        })).then(response => {
          let recInstances = (response as GetAppInstances.Response).result.instances;

          edge.sendRequest(this.websocket,
            new ComponentJsonApiRequest({
              componentId: '_appManager',
              payload: new GetAppAssistant.Request({ appId: appId })
            })).then(response2 => {
              let appAssistant = GetAppAssistant.postprocess((response2 as GetAppAssistant.Response).result);
              this.appName = appAssistant.name;
              this.instances = [];
              for (let instance of recInstances) {
                let form = new FormGroup({});
                this.forms.push(form);
                const clonedFields = [];
                appAssistant.fields.forEach(val => clonedFields.push(Object.assign({}, val)));
                this.instances.push({
                  instanceId: instance.instanceId,
                  form: form,
                  isDeleting: false,
                  isUpdating: false,
                  fields: clonedFields,
                  properties: {
                    'ALIAS': instance.alias,
                    ...instance.properties
                  },
                });
              }

              this.service.stopSpinner(this.spinnerId);

            }).catch(reason => {
              console.error(reason.error);
              this.service.toast('Error while receiving App Assistant for [' + appId + ']: ' + reason.error.message, 'danger');
            });

        }).catch(reason => {
          console.error(reason.error);
          this.service.toast('Error while receiving App-Instances for [' + appId + ']: ' + reason.error.message, 'danger');
        });
    });
  }

  protected submit(instance: MyInstance) {
    this.service.startSpinnerTransparentBackground(instance.instanceId);
    instance.isUpdating = true;
    // remove alias field from properties
    let alias = instance.form.value['ALIAS'];
    const clonedFields = {};
    for (let item in instance.form.value) {
      if (item != 'ALIAS') {
        clonedFields[item] = instance.form.value[item];
      }
    }
    instance.form.markAsPristine();
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: '_appManager',
        payload: new UpdateAppInstance.Request({
          instanceId: instance.instanceId,
          alias: alias,
          properties: clonedFields
        })
      })).then(response => {
        const result = (response as UpdateAppInstance.Response).result;

        if (result.warnings && result.warnings.length > 0) {
          this.service.toast(result.warnings.join(';'), 'warning');
        } else {
          this.service.toast(this.translate.instant('Edge.Config.App.successUpdate'), 'success');
        }
        instance.properties = result.instance.properties;
        instance.properties['ALIAS'] = result.instance.alias;
      }).catch(reason => {
        this.service.toast(this.translate.instant('Edge.Config.App.failUpdate', { error: reason.error.message }), 'danger');
      }).finally(() => {
        instance.isUpdating = false;
        this.service.stopSpinner(instance.instanceId);
      });
  }

  protected delete(instance: MyInstance) {
    this.service.startSpinnerTransparentBackground(instance.instanceId);
    instance.isDeleting = true;
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: '_appManager',
        payload: new DeleteAppInstance.Request({
          instanceId: instance.instanceId
        })
      })).then(response => {
        this.instances.splice(this.instances.indexOf(instance), 1);
        this.service.toast(this.translate.instant('Edge.Config.App.successDelete'), 'success');
        this.router.navigate(['device/' + (this.edge.id) + '/settings/app/']);
      }).catch(reason => {
        this.service.toast(this.translate.instant('Edge.Config.App.failDelete', { error: reason.error.message }), 'danger');
      }).finally(() => {
        instance.isDeleting = false;
        this.service.stopSpinner(instance.instanceId);
      });
  }
}
