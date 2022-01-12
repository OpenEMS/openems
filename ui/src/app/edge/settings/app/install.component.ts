import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { AddAppInstance } from './jsonrpc/addAppInstance';
import { GetAppAssistant } from './jsonrpc/getAppAssistant';

@Component({
  selector: InstallAppComponent.SELECTOR,
  templateUrl: './install.component.html'
})
export class InstallAppComponent implements OnInit {

  private static readonly SELECTOR = "appInstall";
  public readonly spinnerId: string = InstallAppComponent.SELECTOR;

  public form = null;
  public model = null;
  public fields: FormlyFieldConfig[] = null;

  private appId: string;
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
    this.appId = appId;
    this.service.setCurrentComponent("App " + appId, this.route).then(edge => {
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GetAppAssistant.Request({ appId: appId })
        })).then(response => {
          let appAssistant = GetAppAssistant.postprocess((response as GetAppAssistant.Response).result);
          this.fields = appAssistant.fields;
          this.model = {};
          this.form = new FormGroup({});
          this.service.stopSpinner(this.spinnerId);

        })
        .catch(reason => {
          console.error(reason.error);
          this.service.toast("Error while receiving App Assistant for [" + appId + "]: " + reason.error.message, 'danger');
        });
    });
  }

  public submit() {
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_appManager",
        payload: new AddAppInstance.Request({
          appId: this.appId,
          properties: this.form.value
        })
      })).then(response => {
        this.form.markAsPristine();
        this.service.toast("Successfully installed App", 'success');
      }).catch(reason => {
        this.service.toast("Error installing App:" + reason.error.message, 'danger');
      });
  }

}