import { Component, inject, model, ChangeDetectionStrategy } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { ScheduleComponent } from "src/app/shared/components/schedule/schedule.component";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { EdgeConfig } from "../../../../../shared/components/edge/edgeconfig";
import { RouteService } from "../../../../../shared/service/route.service";
import { HeatManualPayload } from "./js-calendar-utils";

@Component({
    templateUrl: "./schedule.component.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [ScheduleComponent, ComponentsBaseModule, CommonUiModule, FlatWidgetButtonComponent],
})
export class HeatScheduleComponent extends AbstractModal {
    protected schedule = model<JsCalendar.ScheduleVM[]>([]);
    protected payload = model(new HeatManualPayload());
    protected isAskomaReadOnly: boolean = false;
    protected canWrite: boolean = this.payload().canWrite(this.edge);
    protected console: Console = console;
    private readonly routeService: RouteService = inject(RouteService);

    protected override updateComponent(config: EdgeConfig): void {
        this.component = config.getComponentSafely(this.routeService.getRouteParam("componentId"));
    }

    protected override onIsInitialized(): void {
        this.isAskomaReadOnly =
            this.component?.factoryId === "Heat.Askoma" && this.component.properties?.readOnly === true;
    }
}
