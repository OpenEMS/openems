import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable, Subject, timer } from 'rxjs';
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
  public systemUpdateState: SystemUpdateState = { unknown: {} };
  public readonly spinnerId: string = SystemUpdateComponent.SELECTOR;
  public showLog: boolean = false;
  public readonly ESTIMATED_REBOOT_TIME = 600; // Seconds till the openems service is restarted after update

  public edge: Edge = null;
  private ngUnsubscribe = new Subject<void>();
  public restartTime: number = this.ESTIMATED_REBOOT_TIME;
  public isEdgeRestarting: boolean = false;
  public lastResult: SystemUpdateState = { unknown: {} }

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
        this.lastResult = result;
      }).catch(error => {
        if (this.lastResult.running?.percentCompleted >= 98) {
          this.startTimer()
          this.service.toast("Das " + environment.edgeShortName + " wird nun neugestartet", 'success');
          return
        }
        console.error(error);
        this.service.toast("Error while executing system update: " + error.message, 'danger');
      });
  }
  startTimer() {
    this.isEdgeRestarting = true;
    let timeLeft = this.ESTIMATED_REBOOT_TIME;
    let interval = setInterval(() => {
      if (timeLeft >= 0) {
        if (this.edge.isOnline) {
          timeLeft = 0
          this.restartTime = timeLeft;
        } else {
          this.restartTime = timeLeft--;
        }
      } else {
        clearInterval(interval)
      }
    }, 1000)
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
        if (this.lastResult.running?.percentCompleted >= 98) {
          return
        }
        this.service.toast("Error while executing system update: " + reason.error.message, 'danger');
      });
  }

  private stopRefreshSystemUpdateState() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}
