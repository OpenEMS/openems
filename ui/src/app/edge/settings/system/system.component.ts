// @ts-strict-ignore
import { Component, effect } from "@angular/core";
import { Subject, takeUntil, timer } from "rxjs";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { UserService } from "src/app/shared/service/USER.SERVICE";
import { environment } from "src/environments";
import { Edge, Service, UserPermission, Utils, Websocket } from "../../../shared/shared";
import { ExecuteUpdate } from "./jsonrpc/executeUpdate";
import { GetUpdateables, Updateable } from "./jsonrpc/getUpdateables";
import { GetUpdateState, UpdateState } from "./jsonrpc/getUpdateState";

@Component({
  selector: SYSTEM_COMPONENT.SELECTOR,
  templateUrl: "./SYSTEM.COMPONENT.HTML",
  standalone: false,
})
export class SystemComponent {

  private static readonly SELECTOR = "system";
  private static readonly REFRESH_UPDATE_STATE_INTERVAL: number = 5_000; // 5s

  protected readonly environment = environment;
  protected readonly spinnerId: string = SYSTEM_COMPONENT.SELECTOR;
  protected showLog: boolean = false;
  protected readonly ESTIMATED_REBOOT_TIME = 600; // Seconds till the openems service is restarted after update
  protected edge: Edge;
  protected restartTime: number = this.ESTIMATED_REBOOT_TIME;
  protected canSeeSystemRestart: boolean = false;

  protected canSeeAdditionalUpdates: boolean = false;
  protected updateables: UpdateableState[] = [];

  constructor(
    protected utils: Utils,
    private service: Service,
    private userService: UserService,
    private websocket: Websocket,
  ) {
    effect(async (onCleanup) => {
      const subjectOnCleanup = new Subject<void>();
      onCleanup(() => {
        SUBJECT_ON_CLEANUP.NEXT();
        SUBJECT_ON_CLEANUP.COMPLETE();
      });

      const user = THIS.USER_SERVICE.CURRENT_USER();
      THIS.EDGE = await THIS.SERVICE.CURRENT_EDGE();
      if (!THIS.EDGE) {
        return;
      }

      THIS.CAN_SEE_SYSTEM_RESTART = USER_PERMISSION.IS_ALLOWED_TO_SEE_SYSTEM_RESTART(user, THIS.EDGE);

      THIS.CAN_SEE_ADDITIONAL_UPDATES = USER_PERMISSION.IS_ALLOWED_TO_SEE_ADDITIONAL_UPDATES(THIS.EDGE);
      if (!THIS.CAN_SEE_ADDITIONAL_UPDATES) {
        return;
      }
      THIS.UPDATEABLES = await THIS.FETCH_UPDATEABLES(subjectOnCleanup);
    });
  }

  protected executeUpdate(updateableState: UpdateableState) {
    THIS.EDGE.SEND_REQUEST<EXECUTE_UPDATE.RESPONSE>(THIS.WEBSOCKET, new ComponentJsonApiRequest({
      componentId: "_updateManager",
      payload: new EXECUTE_UPDATE.REQUEST({ id: UPDATEABLE_STATE.UPDATEABLE.ID }),
    })).then(_ => {
      UPDATEABLE_STATE.UPDATE_STATE = { type: "running", percentCompleted: 0, logs: [] };
      THIS.SUBSCRIBE_UPDATE_STATE(updateableState);
    });
  }

  private async fetchUpdateables(subjectOnCleanup: Subject<void>): Promise<UpdateableState[]> {
    const result = (await THIS.EDGE.SEND_REQUEST<GET_UPDATEABLES.RESPONSE>(THIS.WEBSOCKET, new ComponentJsonApiRequest({
      componentId: "_updateManager",
      payload: new GET_UPDATEABLES.REQUEST(),
    }))).result;

    return RESULT.UPDATEABLES.MAP(u => {
      const updateableState: UpdateableState = { updateable: u, unsubscribe: new Subject<void>() };
      SUBJECT_ON_CLEANUP.SUBSCRIBE(() => {
        UPDATEABLE_STATE.UNSUBSCRIBE.NEXT();
        UPDATEABLE_STATE.UNSUBSCRIBE.COMPLETE();
      });
      THIS.EDGE.SEND_REQUEST<GET_UPDATE_STATE.RESPONSE>(THIS.WEBSOCKET, new ComponentJsonApiRequest({
        componentId: "_updateManager",
        payload: new GET_UPDATE_STATE.REQUEST({ id: U.ID }),
      })).then(response => {
        const result = RESPONSE.RESULT;
        UPDATEABLE_STATE.UPDATE_STATE = RESULT.STATE;

        if (UPDATEABLE_STATE.UPDATE_STATE.TYPE === "running") {
          THIS.SUBSCRIBE_UPDATE_STATE(updateableState);
        }
      });
      return updateableState;
    });
  }

  private subscribeUpdateState(updateableState: UpdateableState) {
    const source = timer(0, SystemComponent.REFRESH_UPDATE_STATE_INTERVAL);
    SOURCE.PIPE(
      takeUntil(UPDATEABLE_STATE.UNSUBSCRIBE),
    ).subscribe(_ => {
      if (!THIS.EDGE.IS_ONLINE) {
        return;
      }

      THIS.EDGE.SEND_REQUEST<GET_UPDATE_STATE.RESPONSE>(THIS.WEBSOCKET, new ComponentJsonApiRequest({
        componentId: "_updateManager",
        payload: new GET_UPDATE_STATE.REQUEST({ id: UPDATEABLE_STATE.UPDATEABLE.ID }),
      })).then(response => {
        const result = RESPONSE.RESULT;
        UPDATEABLE_STATE.UPDATE_STATE = RESULT.STATE;

        if (RESULT.STATE.TYPE !== "running") {
          UPDATEABLE_STATE.UNSUBSCRIBE.NEXT();
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
