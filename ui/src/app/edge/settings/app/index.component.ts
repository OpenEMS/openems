import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { environment } from 'src/environments';
import { Service, Websocket } from '../../../shared/shared';
import { GetApps } from './jsonrpc/getApps';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit {

  private static readonly SELECTOR = "appIndex";
  public readonly spinnerId: string = IndexComponent.SELECTOR;

  public apps: GetApps.App[] = [];

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
  ) {
  }

  ngOnInit() {
    this.service.startSpinner(this.spinnerId);
    this.service.setCurrentComponent(environment.edgeShortName + " Apps", this.route).then(edge => {
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_appManager",
          payload: new GetApps.Request()
        })).then(response => {
          this.apps = (response as GetApps.Response).result.apps;
          this.service.stopSpinner(this.spinnerId);

        }).catch(reason => {
          console.error(reason.error);
          this.service.toast("Error while receiving available apps: " + reason.error.message, 'danger');
        });
    });
  }
}