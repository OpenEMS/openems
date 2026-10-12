// @ts-strict-ignore
import { Component, effect, ChangeDetectionStrategy, signal, computed } from "@angular/core";
import { NgxSpinnerComponent } from "ngx-spinner";
import { filter, timer } from "rxjs";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { LiveDataServiceProvider } from "src/app/shared/provider/live-data-service-provider";
import { UserService } from "src/app/shared/service/user.service";
import { CancellationToken } from "src/app/shared/type/cancellation-token";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";
import { ChangelogComponent } from "../../../changelog/view/component/changelog.component";
import { CommonUiModule } from "../../../shared/common-ui.module";
import { Edge, Service, UserPermission, Utils, Websocket } from "../../../shared/shared";
import { ExecuteUpdate } from "./jsonrpc/executeUpdate";
import { GetUpdateables, Updateable } from "./jsonrpc/getUpdateables";
import { GetUpdateState, UpdateState } from "./jsonrpc/getUpdateState";
import { MaintenanceComponent } from "./maintenance/maintenance";
import { OeSystemUpdateComponent } from "./oe-system-update.component";
import { UpdateableCardComponent } from "./updateable-card.component";

@Component({
    selector: SystemComponent.SELECTOR,
    templateUrl: "./system.component.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        CommonUiModule,
        ChangelogComponent,
        OeSystemUpdateComponent,
        MaintenanceComponent,
        NgxSpinnerComponent,
        ComponentsBaseModule,
        LiveDataServiceProvider,
        UpdateableCardComponent,
    ],
})
export class SystemComponent {
    private static readonly SELECTOR = "system";
    private static readonly REFRESH_UPDATE_STATE_INTERVAL: number = 5_000; // 5s

    protected readonly environment = environment;
    protected readonly spinnerId: string = SystemComponent.SELECTOR;
    protected readonly Role = Role;
    protected showLog: boolean = false;
    protected readonly ESTIMATED_REBOOT_TIME = 600; // Seconds till the openems service is restarted after update
    protected edge: Edge;
    protected restartTime: number = this.ESTIMATED_REBOOT_TIME;
    protected canSeeSystemRestart: boolean = false;

    protected readonly updateables = signal<UpdateableState[]>([]);

    protected readonly allowedUpdateables = computed(() => this.getAllowedUpdateables());

    protected readonly updateablesForOnlyAdmins = computed(() => this.getUpdateablesForOnlyAdmins());

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
            try {
                this.updateables.set(await this.fetchUpdateables());
            } catch {
                // Edge version does not support Updateables or the user is not permitted
            }
            await this.initializeUpdateStateFetcher(cancellationToken);
        });
    }

    protected executeUpdate(updateableState: UpdateableState) {
        this.edge
            .sendRequest<ExecuteUpdate.Response>(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "_updateManager",
                    payload: new ExecuteUpdate.Request({ id: updateableState.updateable.id }),
                }),
            )
            .then((_) => {
                // Replace the entry immutably via the signal so the change is rendered immediately
                this.updateables.update((list) =>
                    list.map((u) =>
                        u.updateable.id === updateableState.updateable.id
                            ? { ...u, updateState: { type: "running", percentCompleted: 0, logs: [] } }
                            : u,
                    ),
                );
            });
    }

    protected getAllowedUpdateables(): UpdateableState[] {
        return this.getUpdateablesForAtLeastInstaller().filter((x) =>
            Role.isAtLeast(this.edge.role, x.updateable.requiredMinRole ?? Role.ADMIN),
        );
    }

    protected getUpdateablesForOnlyAdmins(): UpdateableState[] {
        return this.updateables().filter((x) => Role.isAtLeast(x.updateable.requiredMinRole ?? Role.ADMIN, Role.ADMIN));
    }

    private async initializeUpdateStateFetcher(cancellationToken: CancellationToken): Promise<void> {
        let isUpdatePending = false;

        const source = timer(1, SystemComponent.REFRESH_UPDATE_STATE_INTERVAL);
        source
            .pipe(
                cancellationToken.observablePipe(),
                filter((_) => this.edge.isOnline && !isUpdatePending),
            )
            .subscribe(async (_) => {
                isUpdatePending = true;
                try {
                    const updateablesToFetch = this.updateables().filter((x) =>
                        this.doesUpdateableRequireStateUpdate(x),
                    );
                    const promises = updateablesToFetch.map((x) => this.updateUpdateableState(x));
                    await Promise.allSettled(promises);
                } catch (err) {
                    console.error("Failed to update updateable states", err);
                } finally {
                    isUpdatePending = false;
                }
            });
    }

    private async fetchUpdateables(): Promise<UpdateableState[]> {
        const result = (
            await this.edge.sendRequest<GetUpdateables.Response>(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "_updateManager",
                    payload: new GetUpdateables.Request(),
                }),
            )
        ).result;

        return result.updateables.map((u) => <UpdateableState>{ updateable: u });
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
            const response = await this.edge.sendRequest<GetUpdateState.Response>(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "_updateManager",
                    payload: new GetUpdateState.Request({ id: updateable.updateable.id }),
                }),
            );

            this.updateables.update((list) =>
                list.map((u) =>
                    u.updateable.id === updateable.updateable.id ? { ...u, updateState: response.result.state } : u,
                ),
            );
        } catch (err) {
            console.error(`Failed to fetch update state for updateable ${updateable.updateable.id}`, err);
        }
    }

    private getUpdateablesForAtLeastInstaller(): UpdateableState[] {
        return this.updateables().filter((x) =>
            Role.isAtMost(x.updateable.requiredMinRole ?? Role.ADMIN, Role.INSTALLER),
        );
    }
}

export type UpdateableState = {
    updateable: Updateable;
    updateState?: UpdateState;
};
