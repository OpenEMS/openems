import { Component, model } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { JsonRpcUtils } from "src/app/shared/jsonrpc/jsonrpcutils";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { ComponentsBaseModule } from "../../components.module";
import { EdgeConfig } from "../../edge/edgeconfig";
import { validateTaskInputs } from "../form/task-form-validation";
import { TaskFormComponent } from "../form/task-form.component";
import { JsCalendarAddTaskComponent } from "../js-calendar-add-task";
import { JsCalendar } from "../js-calendar-task";

@Component({
    selector: "oe-components-scheduler-add-task",
    templateUrl: "./add-task.component.html",
    standalone: true,
    imports: [
        CommonUiModule,
        TaskFormComponent,
        ComponentsBaseModule,
    ],
})
export class AddTaskComponent extends JsCalendarAddTaskComponent {

    public payload = model<JsCalendar.OpenEMSPayload<any>>(new JsCalendar.BaseOpenEMSPayload());
    public allowedPeriods = model<JsCalendar.Task["recurrenceRules"][number]["frequency"][]>([]);
    public startTime = model<string | null>(null);
    public endTime = model<string | null>(null);
    public recurrenceRuleByDay = model<JsCalendar.Task["recurrenceRules"][number] | null>(null);

    protected override updateComponent(config: EdgeConfig): void {
        const componentId = this.routeService.getRouteParam<string>("componentId");
        this.component = config.getComponentSafely(componentId);
    }

    protected async saveTaskToEdge() {

        if (this.validateInputs() === false || this.startTime() === null || this.endTime() === null) {
            return;
        }

        const startDate = DateUtils.stringToDate(this.startTime());
        const endDate = DateUtils.stringToDate(this.endTime());
        const validatorResult = this.payload().validator(this.translate);

        if (validatorResult.valid == false) {
            this.service.toast(validatorResult.errors.map(el => el.message).join(","), "danger");
            return;
        }

        if (DateUtils.isDateBefore(endDate, startDate)) {
            this.service.toast(this.translate.instant("JS_SCHEDULE.VALIDATION_ERROR_6"), "danger");
            return;
        }
        const recurrenceRuleByDay = this.recurrenceRuleByDay();
        const localDateTime = JsCalendar.Utils.formatIsoLocalDateTime(startDate);

        if (localDateTime == null) {
            return;
        }

        const task: JsCalendar.Task = {
            "@type": "Task",
            "start": localDateTime,
            "recurrenceRules": recurrenceRuleByDay != null ? [recurrenceRuleByDay] : [],
            ...JsCalendar.Utils.computeIsoDuration(startDate, endDate),
            ...this.payload().toOpenEMSPayload(),
        };

        if (this.edge === null || this.component === null) {
            return;
        }

        const [err] = await JsonRpcUtils.handle(
            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId: this.component.id,
                payload: new JsCalendar.AddTaskRequest({ task }),
            }))
        );

        if (err) {
            this.service.toast(this.translate.instant("JS_SCHEDULE.ADD_ERROR"), "danger");
            return;
        }

        this.service.toast(this.translate.instant("JS_SCHEDULE.ADD_SUCCESS"), "success");
        this.resetFields();
    }

    private validateInputs(): boolean {
        const result = validateTaskInputs(
            this.startTime(),
            this.endTime(),
            this.payload(),
            this.recurrenceRuleByDay(),
            this.translate,
        );

        if (!result.valid) {
            this.service.toast(result.message!, "danger");
            return false;
        }

        return true;
    }

    private resetFields(): void {
        this.startTime.set(null);
        this.endTime.set(null);
        this.recurrenceRuleByDay.set(null);
    }
}

