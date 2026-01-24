// @ts-strict-ignore
import { Component, inject, model } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { JsonRpcUtils } from "src/app/shared/jsonrpc/jsonrpcutils";
import { AddTask } from "src/app/shared/jsonrpc/request/addTaskRequest";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { RouteService } from "src/app/shared/service/route.service";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { Mode } from "../../chargemode/chargemode";
import { computeIsoDuration, formatIsoLocalDateTime } from "../../jsonCalenderUtils";
import { validateTaskInputs } from "../form/task-form-validation";

@Component({
    templateUrl: "./add-task.component.html",
    standalone: false,
})
export class AddTaskComponent extends AbstractModal {

    protected startTime = model<string | null>(null);
    protected endTime = model<string | null>(null);
    protected selectedMode = model<Mode | null>(null);

    private routeService: RouteService = inject(RouteService);

    protected async saveTaskToEdge() {
        if (this.validateInputs() === false) {
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

        const task: AddTask.Task = {
            "@type": "Task",
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
                payload: new AddTask.Request({ task }),
            }))
        );

        if (err) {
            this.service.toast(this.translate.instant("EVSE_SINGLE.SCHEDULE.ADD_ERROR"), "danger");
            return;
        }

        this.service.toast(this.translate.instant("EVSE_SINGLE.SCHEDULE.ADD_SUCCESS"), "success");
        this.resetFields();
    }

    private validateInputs(): boolean {
        const result = validateTaskInputs(
            this.startTime(),
            this.endTime(),
            this.selectedMode(),
            this.translate,
        );

        if (!result.valid) {
            this.service.toast(result.message!, "danger");
            return false;
        }

        return true;
    }

    private resetFields(): void {
        this.selectedMode.set(null);
        this.startTime.set(null);
        this.endTime.set(null);
    }
}

