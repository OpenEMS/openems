import { Component, inject, model } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetOneTasks } from "src/app/shared/jsonrpc/request/getOneTasks";
import { GetOneTasksResponse, OneTask } from "src/app/shared/jsonrpc/response/getOneTasksResponse";
import { RouteService } from "src/app/shared/service/route.service";
import { AbstractModal } from "../../modal/abstractModal";
import { JsCalendar } from "../js-calendar-task";

@Component({
    selector: "oe-components-scheduler-one-tasks",
    templateUrl: "./one-tasks.html",
    imports: [
        CommonUiModule,
    ],
})
export class OneTasksComponent extends AbstractModal {

    public payload = model(new JsCalendar.BaseOpenEMSPayload());
    protected oneTasks: OneTask[] = [];
    private routeService = inject(RouteService);

    protected override onIsInitialized(): void {

        const now = new Date(Date.now());
        const THREE_DAYS_FROM_NOW = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000); // 3 days in milliseconds

        const componentId = this.routeService.getRouteParam<string>("componentId");

        if (this.edge == null || componentId == null) {
            return;
        }

        this.edge.sendRequest<GetOneTasksResponse>(this.websocket, new ComponentJsonApiRequest({
            componentId: componentId,
            payload: new GetOneTasks(now.toISOString(), THREE_DAYS_FROM_NOW.toISOString()),
        })).then(response => {
            const payload = this.payload();
            this.oneTasks = response.result.oneTasks.map(item => {
                return {
                    uid: item.uid,
                    start: item.start,
                    end: JsCalendar.Utils.calculateEndTimeFromDuration(item?.start ?? null, item?.duration ?? null),
                    duration: payload.toOneTasks(item, this.translate),
                } as OneTask;
            });
        });
    }
}
