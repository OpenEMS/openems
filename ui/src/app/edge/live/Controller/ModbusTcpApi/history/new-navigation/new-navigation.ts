import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { RouteService } from "src/app/shared/service/route.service";
import { EdgeConfig } from "src/app/shared/shared";
import { ControllerModbusTcpApiChartComponent } from "../chart/chart";

@Component({
    templateUrl: "./new-navigation.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [ControllerModbusTcpApiChartComponent, CommonUiModule, ComponentsBaseModule],
})
export class ControllerModbusTcpApiHistoryComponent extends AbstractModal {
    private readonly routeService: RouteService = inject(RouteService);

    protected override updateComponent(config: EdgeConfig): void {
        this.component = config.getComponentSafely(this.routeService.getRouteParam<string>("componentId"));
    }
}
