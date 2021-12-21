import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { environment } from 'src/environments';
import { Edge, Service, Utils, Websocket } from '../../../shared/shared';
import { ExecuteSystemUpdateRequest } from './executeSystemUpdateRequest';
import { GetSystemUpdateStateRequest } from './getSystemUpdateStateRequest';
import { GetSystemUpdateStateResponse, SystemUpdateState } from './getSystemUpdateStateResponse';

@Component({
  selector: SystemUpdateComponent.SELECTOR,
  templateUrl: './systemupdate.component.html'
})
export class SystemUpdateComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "systemUpdate";

  public readonly environment = environment;
  public systemUpdateState: SystemUpdateState = null;
  public readonly spinnerId: string = SystemUpdateComponent.SELECTOR;
  public showLog: boolean = false;

  public edge: Edge = null;
  private ngUnsubscribe = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent("", this.route).then(edge => {
      this.edge = edge;
      // Update System Update State now and every 15 seconds
      const source = timer(0, 15000);
      source.pipe(
        takeUntil(this.ngUnsubscribe)
      ).subscribe(ignore => {
        if (!edge.isOnline) {
          return;
        }
        this.refreshSystemUpdateState();
      });
    });
  }

  ngOnDestroy() {
    this.stopRefreshSystemUpdateState();
  }

  private refreshSystemUpdateState() {
    this.service.startSpinner(this.spinnerId);
    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_host",
        payload: new GetSystemUpdateStateRequest()
      })).then(response => {
        let result = (response as GetSystemUpdateStateResponse).result;
        this.systemUpdateState = result;
        this.service.stopSpinner(this.spinnerId);

        // Stop regular check if there is no Update available
        if (result.updated || result.running?.percentCompleted == 100) {
          this.stopRefreshSystemUpdateState();
        }

      }).catch(reason => {
        console.error(reason.error);
        this.service.toast("Error while executing system update: " + reason.error.message, 'danger');
      });
  }

  public executeSystemUpdate() {
    this.service.startSpinner(this.spinnerId);

    this.edge.sendRequest(this.websocket,
      new ComponentJsonApiRequest({
        componentId: "_host",
        payload: new ExecuteSystemUpdateRequest({ isDebug: environment.debugMode })
      })).then(response => {
        // Finished System Update (without restart of OpenEMS Edge)
        this.systemUpdateState = (response as GetSystemUpdateStateResponse).result;
        this.service.stopSpinner(this.spinnerId);
        this.stopRefreshSystemUpdateState();

      }).catch(reason => {
        console.error(reason.error);
        this.service.toast("Error while executing system update: " + reason.error.message, 'danger');
      });
  }

  private stopRefreshSystemUpdateState() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}