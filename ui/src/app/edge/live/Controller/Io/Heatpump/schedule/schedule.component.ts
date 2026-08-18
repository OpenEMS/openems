import { ChangeDetectionStrategy, Component, inject, model } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { EdgeConfig } from "src/app/shared/components/edge/edgeconfig";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { ScheduleComponent } from "src/app/shared/components/schedule/schedule.component";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { RouteService } from "src/app/shared/service/route.service";
import { HeatpumpPayload } from "./heatpump-payload";

@Component({
    templateUrl: "./schedule.component.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    imports: [ScheduleComponent, ComponentsBaseModule, CommonUiModule],
})
export class HeatPumpScheduleComponent extends AbstractModal {
    protected schedule = model<JsCalendar.ScheduleVM[]>([]);
    protected payload = model(new HeatpumpPayload());
    private readonly routeService: RouteService = inject(RouteService);

    protected override updateComponent(config: EdgeConfig): void {
        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
    }
}
