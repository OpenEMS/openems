import { Component } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    templateUrl: "./new-navigation.html",
    standalone: false,
})
export class CurrentVoltageOverviewComponent extends AbstractHistoryChartOverview {

    protected isMeterAsymmetric: boolean | null = null;

    protected override afterIsInitialized(): void {
        this.isMeterAsymmetric = this.config.hasComponentNature("io.openems.edge.meter.api.ElectricityMeter",
            this.route.snapshot.params.componentId);
    }
}
