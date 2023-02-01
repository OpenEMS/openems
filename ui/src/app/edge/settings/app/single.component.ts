import { Component, HostListener, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { GetApp } from './jsonrpc/getApp';
import { GetAppDescriptor } from './jsonrpc/getAppDescriptor';
import { GetApps } from './jsonrpc/getApps';

@Component({
  selector: SingleAppComponent.SELECTOR,
  templateUrl: './single.component.html'
})
export class SingleAppComponent implements OnInit {

  private static readonly SELECTOR = "appSingle";
  public readonly spinnerId: string = SingleAppComponent.SELECTOR;

  public form: FormGroup | null = null;
  public model: any | null = null;

  private appId: string | null = null;
  private app: GetApps.App | null = null;
  private descriptor: GetAppDescriptor.AppDescriptor | null = null;
  private isXL: boolean = true;

  // for stopping spinner when all responses are recieved
  private readonly requestCount: number = 2;
  private recievedResponse: number = 0;

  private edge: Edge | null = null;

  public constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private sanitizer: DomSanitizer
  ) {
  }

  public ngOnInit() {
    this.service.startSpinner(this.spinnerId);
    this.updateIsXL();
    this.appId = this.route.snapshot.params["appId"];
    let appName = this.route.snapshot.queryParams['name'];
    let appId = this.appId;
    this.service.setCurrentComponent(appName, this.route).then(edge => {
      this.edge = edge;

      // set appname, image ...
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
          }).catch(reason => {
            console.error(reason.error);
            this.service.toast("Error while receiving App[" + appId + "]: " + reason.error.message, 'danger');
          });
      }
      // set app descriptor
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GetAppDescriptor.Request({ appId: appId })
        })).then(response => {
          let descriptor = (response as GetAppDescriptor.Response).result;
          this.descriptor = GetAppDescriptor.postprocess(descriptor, this.sanitizer);
        }).catch(reason => {
          console.error(reason.error);
          this.service.toast("Error while receiving AppDescriptor for App[" + appId + "]: " + reason.error.message, 'danger');
        }).finally(() => {
          this.increaseRecievedResponse();
        });
    });
  }

  @HostListener('window:resize', ['$event'])
  private onResize(event) {
    this.updateIsXL();
  }

  private updateIsXL() {
    this.isXL = 1200 <= window.innerWidth;
  }

  protected iFrameStyle() {
    let styles = {
      'height': (this.isXL) ? '100%' : window.innerHeight + 'px'
    };
    return styles;
  }

  private setApp(app: GetApps.App) {
    this.app = app;
    this.form = new FormGroup({});
    this.increaseRecievedResponse();
  }

  private increaseRecievedResponse() {
    this.recievedResponse++;
    if (this.recievedResponse == this.requestCount) {
      this.recievedResponse = 0;
      this.service.stopSpinner(this.spinnerId);
    }
  }

}