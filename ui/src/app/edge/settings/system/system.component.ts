// @ts-strict-ignore
import { Component, effect, inject } from "@angular/core";
import { Subject, takeUntil, timer } from "rxjs";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { UserService } from "src/app/shared/service/user.service";
import { environment } from "src/environments";
import { Edge, Service, UserPermission, Utils, Websocket } from "../../../shared/shared";
import { ExecuteUpdate } from "./jsonrpc/executeUpdate";
import { GetUpdateables, Updateable } from "./jsonrpc/getUpdateables";
import { GetUpdateState, UpdateState } from "./jsonrpc/getUpdateState";

@Component({
  selector: SystemComponent.SELECTOR,
  templateUrl: "./system.component.html",
  standalone: false,
})
export class SystemComponent {
  protected utils = inject(Utils);
  private service = inject(Service);
  private userService = inject(UserService);
  private websocket = inject(Websocket);


  private static readonly SELECTOR = "system";
  private static readonly REFRESH_UPDATE_STATE_INTERVAL: number = 5_000; // 5s

  protected readonly environment = environment;
  protected readonly spinnerId: string = SystemComponent.SELECTOR;
  protected showLog: boolean = false;
  protected readonly ESTIMATED_REBOOT_TIME = 600; // Seconds till the openems service is restarted after update
  protected edge: Edge;
  protected restartTime: number = this.ESTIMATED_REBOOT_TIME;
  protected canSeeSystemRestart: boolean = false;

  protected canSeeAdditionalUpdates: boolean = false;
  protected updateables: UpdateableState[] = [];

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    effect(async (onCleanup) => {
      const subjectOnCleanup = new Subject<void>();
      onCleanup(() => {
        subjectOnCleanup.next();
        subjectOnCleanup.complete();
      });

      const user = this.userService.currentUser();
      this.edge = await this.service.currentEdge();
      if (!this.edge) {
        return;
      }

      this.canSeeSystemRestart = UserPermission.isAllowedToSeeSystemRestart(user, this.edge);

      this.canSeeAdditionalUpdates = UserPermission.isAllowedToSeeAdditionalUpdates(this.edge);
      if (!this.canSeeAdditionalUpdates) {
        return;
      }
      this.updateables = await this.fetchUpdateables(subjectOnCleanup);
    });
  }

  protected executeUpdate(updateableState: UpdateableState) {
    this.edge.sendRequest<ExecuteUpdate.Response>(this.websocket, new ComponentJsonApiRequest({
      componentId: "_updateManager",
      payload: new ExecuteUpdate.Request({ id: updateableState.updateable.id }),
    })).then(_ => {
      updateableState.updateState = { type: "running", percentCompleted: 0, logs: [] };
      this.subscribeUpdateState(updateableState);
    });
  }

  private async fetchUpdateables(subjectOnCleanup: Subject<void>): Promise<UpdateableState[]> {
    const result = (await this.edge.sendRequest<GetUpdateables.Response>(this.websocket, new ComponentJsonApiRequest({
      componentId: "_updateManager",
      payload: new GetUpdateables.Request(),
    }))).result;

    return result.updateables.map(u => {
      const updateableState: UpdateableState = { updateable: u, unsubscribe: new Subject<void>() };
      subjectOnCleanup.subscribe(() => {
        updateableState.unsubscribe.next();
        updateableState.unsubscribe.complete();
      });
      this.edge.sendRequest<GetUpdateState.Response>(this.websocket, new ComponentJsonApiRequest({
        componentId: "_updateManager",
        payload: new GetUpdateState.Request({ id: u.id }),
      })).then(response => {
        const result = response.result;
        updateableState.updateState = result.state;

        if (updateableState.updateState.type === "running") {
          this.subscribeUpdateState(updateableState);
        }
      });
      return updateableState;
    });
  }

  private subscribeUpdateState(updateableState: UpdateableState) {
    const source = timer(0, SystemComponent.REFRESH_UPDATE_STATE_INTERVAL);
    source.pipe(
      takeUntil(updateableState.unsubscribe),
    ).subscribe(_ => {
      if (!this.edge.isOnline) {
        return;
      }

      this.edge.sendRequest<GetUpdateState.Response>(this.websocket, new ComponentJsonApiRequest({
        componentId: "_updateManager",
        payload: new GetUpdateState.Request({ id: updateableState.updateable.id }),
      })).then(response => {
        const result = response.result;
        updateableState.updateState = result.state;

        if (result.state.type !== "running") {
          updateableState.unsubscribe.next();
        }
      });
    });
  }

}

type UpdateableState = {
  updateable: Updateable,
  updateState?: UpdateState,
  unsubscribe: Subject<void>,
};
