import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { GetApp } from './jsonrpc/getApp';
import { GetApps } from './jsonrpc/getApps';

@Component({
  selector: SingleAppComponent.SELECTOR,
  templateUrl: './single.component.html'
})
export class SingleAppComponent implements OnInit {

  private static readonly SELECTOR = "appSingle";
  public readonly spinnerId: string = SingleAppComponent.SELECTOR;

  public form = null;
  public model = null;

  private appId: string;
  private app: GetApps.App;

  private edge: Edge = null;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    //private http: HttpClient
  ) {
  }

  ngOnInit() {
    this.service.startSpinner(this.spinnerId);
    this.appId = this.route.snapshot.params["appId"];
    let appId = this.appId;
    this.service.setCurrentComponent("App " + appId, this.route).then(edge => {
      this.edge = edge;

      if ('appId' in history.state) {
        this.setApp(history.state)
      } else {

        edge.sendRequest(this.websocket,
          new ComponentJsonApiRequest({
            componentId: "_appManager",
            payload: new GetApp.Request({ appId: appId })
          })).then(response => {
            let app = (response as GetApp.Response).result.app;
            this.setApp(app)
          })
          .catch(reason => {
            console.error(reason.error);
            this.service.toast("Error while receiving App[" + appId + "]: " + reason.error.message, 'danger');
          });

      }
    });
  }

  private setApp(app: GetApps.App) {
    this.app = app;
    this.form = new FormGroup({});
    this.service.stopSpinner(this.spinnerId);
  }

}