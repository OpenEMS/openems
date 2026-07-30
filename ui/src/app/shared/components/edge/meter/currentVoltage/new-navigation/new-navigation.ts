import { Component, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { CurrentVoltageAsymmetricChartComponent } from "../chart/asymmetricMeter";
import { CurrentVoltageSymmetricChartComponent } from "../chart/symmetricMeter";

@Component({
    selector: "oe-current-voltage-overview",
    templateUrl: "./new-navigation.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        CommonUiModule,
        ComponentsBaseModule,
        CurrentVoltageAsymmetricChartComponent,
        CurrentVoltageSymmetricChartComponent,
    ],
})
export class CurrentVoltageOverviewComponent extends AbstractHistoryChartOverview {
    protected isMeterAsymmetric: boolean | null = null;

    protected override afterIsInitialized(): void {
        this.isMeterAsymmetric = this.config.hasComponentNature(
            "io.openems.edge.meter.api.ElectricityMeter",
            this.route.snapshot.params.componentId,
        );
    }
}
