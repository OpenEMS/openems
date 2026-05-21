// @ts-strict-ignore
import { Component, effect } from "@angular/core";
import { NgxSpinnerComponent } from "ngx-spinner";
import { filter, timer } from "rxjs";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { LiveDataServiceProvider } from "src/app/shared/provider/live-data-service-provider";
import { UserService } from "src/app/shared/service/user.service";
import { CancellationToken } from "src/app/shared/type/cancellation-token";
import { environment } from "src/environments";
import { ChangelogComponent } from "../../../changelog/view/component/changelog.component";
import { CommonUiModule } from "../../../shared/common-ui.module";
import { Edge, Service, UserPermission, Utils, Websocket } from "../../../shared/shared";
import { ExecuteUpdate } from "./jsonrpc/executeUpdate";
import { GetUpdateables, Updateable } from "./jsonrpc/getUpdateables";
import { GetUpdateState, UpdateState } from "./jsonrpc/getUpdateState";
import { MaintenanceComponent } from "./maintenance/maintenance";
import { OeSystemUpdateComponent } from "./oe-system-update.component";

@Component({
    selector: SystemComponent.SELECTOR,
    templateUrl: "./system.component.html",
    standalone: true,
    imports: [
        CommonUiModule,
        ChangelogComponent,
        OeSystemUpdateComponent,
        MaintenanceComponent,
        NgxSpinnerComponent,
        ComponentsBaseModule,
        LiveDataServiceProvider,
    ],
})
export class SystemComponent {

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

    constructor(
        protected utils: Utils,
        private service: Service,
        private userService: UserService,
        private websocket: Websocket,
    ) {
        effect(async (onCleanup) => {
            const cancellationToken = CancellationToken.byCleanup(onCleanup);

            const user = this.userService.currentUser();
            this.edge = this.service.currentEdge();
            if (!this.edge) {
                return;
            }

            this.canSeeSystemRestart = UserPermission.isAllowedToSeeSystemRestart(user, this.edge);
            this.canSeeAdditionalUpdates = UserPermission.isAllowedToSeeAdditionalUpdates(this.edge);

            if (this.canSeeAdditionalUpdates) {
                this.updateables = await this.fetchUpdateables();
                await this.initializeUpdateStateFetcher(cancellationToken);
            }
        });
    }

    protected executeUpdate(updateableState: UpdateableState) {
        this.edge.sendRequest<ExecuteUpdate.Response>(this.websocket, new ComponentJsonApiRequest({
            componentId: "_updateManager",
            payload: new ExecuteUpdate.Request({ id: updateableState.updateable.id }),
        })).then(_ => {
            updateableState.updateState = { type: "running", percentCompleted: 0, logs: [] };
        });
    }

    private async initializeUpdateStateFetcher(cancellationToken: CancellationToken): Promise<void> {
        let isUpdatePending = false;

        const source = timer(1, SystemComponent.REFRESH_UPDATE_STATE_INTERVAL);
        source.pipe(
            cancellationToken.observablePipe(),
            filter(_ => this.edge.isOnline && !isUpdatePending)
        ).subscribe(async _ => {
            isUpdatePending = true;
            try {
                const updateablesToFetch = this.updateables.filter(x => this.doesUpdateableRequireStateUpdate(x));
                const promises = updateablesToFetch.map(x => this.updateUpdateableState(x));
                await Promise.allSettled(promises);
            } catch (err) {
                console.error("Failed to update updateable states", err);
            } finally {
                isUpdatePending = false;
            }
        });
    }

    private async fetchUpdateables(): Promise<UpdateableState[]> {
        const result = (await this.edge.sendRequest<GetUpdateables.Response>(this.websocket, new ComponentJsonApiRequest({
            componentId: "_updateManager",
            payload: new GetUpdateables.Request(),
        }))).result;

        return result.updateables.map(u => <UpdateableState>{ updateable: u });
    }

    private doesUpdateableRequireStateUpdate(updateable: UpdateableState): boolean {
        if (updateable.updateState == null) {
            return true;
        }

        switch (updateable.updateState.type) {
            case "running":
            case "unknown":
                return true;
            default:
                return false;
        }
    }

    private async updateUpdateableState(updateable: UpdateableState): Promise<void> {
        try {
            const response = await this.edge.sendRequest<GetUpdateState.Response>(this.websocket, new ComponentJsonApiRequest({
                componentId: "_updateManager",
                payload: new GetUpdateState.Request({ id: updateable.updateable.id }),
            }));

            updateable.updateState = response.result.state;
        } catch (err) {
            console.error(`Failed to fetch update state for updateable ${updateable.updateable.id}`, err);
        }
    }
}

type UpdateableState = {
    updateable: Updateable,
    updateState?: UpdateState
};
