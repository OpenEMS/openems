// @ts-strict-ignore
import { Location } from "@angular/common";
import { Component, inject, model } from "@angular/core";
import { ViewWillEnter } from "@ionic/angular";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { JsonRpcUtils } from "src/app/shared/jsonrpc/jsonrpcutils";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { DeleteTask } from "src/app/shared/jsonrpc/request/deleteTaskRequest";
import { GetAllTasks } from "src/app/shared/jsonrpc/request/getAllTasks";
import { UpdateTask } from "src/app/shared/jsonrpc/request/updateTaskRequest";
import { GetAllTasksResponse } from "src/app/shared/jsonrpc/response/getAllTasksResponse";
import { RouteService } from "src/app/shared/service/route.service";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { Mode } from "../../chargemode/chargemode";
import { computeIsoDuration, formatIsoLocalDateTime, parseISODuration } from "../../jsonCalenderUtils";
import { validateTaskInputs } from "../form/task-form-validation";

@Component({
    templateUrl: "./edit-task.component.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    styles: [`
        ::ng-deep formly-form{
            height: 100% !important;
        }`,
    ],
})

export class EditTaskComponent extends AbstractModal implements ViewWillEnter {
    public static formControlName: string = "task";

    protected formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected componentId: string | null = null;
    protected uid: string | null = null;

    protected startTime = model<string | null>(null);
    protected endTime = model<string | null>(null);
    protected selectedMode = model<Mode | null>(null);

    protected navigationService: NavigationService = inject(NavigationService);
    private routeService: RouteService = inject(RouteService);
    private location: Location = inject(Location);

    public override async onIsInitialized(): Promise<void> {
        const url = this.routeService.currentUrl();
        if (url === null) {
            return;
        }

        const [, , , , , componentId, , , uid] = url.split("/");

        this.componentId = componentId;
        this.uid = uid;

        if (this.edge === null) {
            return;
        }

        const [err, _result] = await JsonRpcUtils.handle(
            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId: this.componentId,
                payload: new GetAllTasks(),
            }))
        );

        if (err) {
            console.error("Error fetching tasks:", err);
            return;
        }

        const result = _result as GetAllTasksResponse;
        const task = result.result.tasks.find(t => t.uid === this.uid);

        if (task === null || task === undefined) {
            console.warn("Task with the given UID not found.");
            return;
        }

        this.startTime.set(this.extractTime(task.start));
        this.endTime.set(this.calculateEndTimeFromDuration(task.start, task.duration));
        this.selectedMode.set(task["openems.io:payload"].mode as Mode);
    }

    public ionViewWillEnter(): void {
        // Create a new navigation tree node for the task
        const newNavigationTree = new NavigationTree(
            "task",
            { baseString: `schedule/task/${this.uid}` },
            { name: "create-outline" },
            this.translate.instant("EVSE_SINGLE.SCHEDULE.EDIT_TASK"),
            "label",
            [],
            null
        );

        // Retrieve the existing navigation tree
        const oldNavigationTree = this.navigationService.navigationTree();
        if (oldNavigationTree == null) {
            return;
        }

        // Find the parent node by its ID
        const parentNode = oldNavigationTree.getChildren()?.find(child => child.id === this.componentId) ?? null;
        if (parentNode == null) {
            console.warn("Parent node not found for componentId:", this.componentId);
            return;
        }

        // Find the 'schedule' node under the parent node
        const scheduleNode = parentNode.getChildren()?.find(child => child.id === "schedule") ?? null;
        if (scheduleNode == null) {
            console.warn("Schedule node not found under:", this.componentId);
            return;
        }

        // Set relationships between the nodes
        newNavigationTree.parent = scheduleNode;
        scheduleNode.setChild("schedule", newNavigationTree);
        scheduleNode.parent = parentNode;

        // Update the navigation system with the modified tree
        this.navigationService.navigationTree.set(oldNavigationTree);
        this.navigationService.currentNode.set(newNavigationTree);
    }

    protected async saveTask(): Promise<void> {
        if (this.validateInputs() == false) {
            return;
        }

        if (this.startTime() === null || this.endTime() === null || this.selectedMode() === null) {
            return;
        }

        const [startH, startM] = this.startTime().split(":").map(Number);
        const [endH, endM] = this.endTime().split(":").map(Number);

        const now = new Date();
        const startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), startH, startM);
        const endDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), endH, endM);

        if (DateUtils.isDateBefore(endDate, startDate)) {
            this.service.toast(this.translate.instant("EVSE_SINGLE.SCHEDULE.VALIDATION_ERROR_6"), "danger");
            return;
        }

        const task: UpdateTask.Task = {
            "@type": "Task",
            "uid": this.uid ?? "",
            "start": formatIsoLocalDateTime(startDate),
            "duration": computeIsoDuration(startDate, endDate),
            "recurrenceRules": [{
                "frequency": "daily",
            }],
            "openems.io:payload": {
                "class": "Manual",
                "mode": this.selectedMode(),
            },
        };

        const fullUrl = this.routeService?.getCurrentUrl() ?? null;
        const regex = /\/([^/]+)\/schedule/;
        if (fullUrl === null) {
            return;
        }

        const match = fullUrl.match(regex);
        if (match === null || match.length < 2) {
            return;
        }
        const componentId = match[1];

        if (this.edge === null) {
            return;
        }

        const [err] = await JsonRpcUtils.handle(
            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId,
                payload: new UpdateTask.Request({ task }),
            }))
        );

        if (err) {
            this.service.toast(this.translate.instant("EVSE_SINGLE.SCHEDULE.EDIT_ERROR"), "danger");
            return;
        }

        this.service.toast(this.translate.instant("EVSE_SINGLE.SCHEDULE.EDIT_SUCCESS"), "success");
        this.location.back();
    }

    protected async deleteTask(): Promise<void> {
        if (this.edge === null) {
            return;
        }

        if (this.componentId === null || this.uid === null) {
            return;
        }

        const [err] = await JsonRpcUtils.handle(
            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId: this.componentId,
                payload: new DeleteTask.Request({ uid: this.uid }),
            }))
        );

        if (err) {
            this.service.toast(this.translate.instant("EVSE_SINGLE.SCHEDULE.DELETE_ERROR"), "danger");
            return;
        }

        this.service.toast(this.translate.instant("EVSE_SINGLE.SCHEDULE.DELETE_SUCCESS"), "success");
        this.location.back();
    }

    private extractTime(dateString: string): string {
        const date = new Date(dateString);

        const hours = date.getHours();
        const minutes = date.getMinutes();

        const formattedHours = hours < 10 ? `0${hours}` : hours;
        const formattedMinutes = minutes < 10 ? `0${minutes}` : minutes;

        return `${formattedHours}:${formattedMinutes}`;
    }

    private calculateEndTimeFromDuration(startDateString: string, isoDuration: string): string {
        const date = new Date(startDateString);

        const { hours, minutes } = parseISODuration(isoDuration) || {};

        date.setHours(date.getHours() + hours);
        date.setMinutes(date.getMinutes() + minutes);

        return this.extractTime(date.toISOString());
    }

    private validateInputs(): boolean {
        const result = validateTaskInputs(
            this.startTime(),
            this.endTime(),
            this.selectedMode(),
            this.translate
        );

        if (!result.valid) {
            this.service.toast(result.message!, "danger");
            return false;
        }

        return true;
    }
}
