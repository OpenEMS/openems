import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { Service } from "src/app/shared/shared";

@Component({
    templateUrl: "./phase-accurate.html",
    standalone: false,
})
export class CommonProductionSingleHistoryOverviewComponent extends AbstractHistoryChartOverview {
    protected componentType: { type: "sum" | "productionMeter" | "charger", displayName: string } | null = null;

    constructor(
        public override service: Service,
        protected override route: ActivatedRoute,
        public override modalCtrl: ModalController,
        private translate: TranslateService,
    ) {
        super(service, route, modalCtrl);
    }

    protected override afterIsInitialized() {
        this.componentType = this.getComponentType();
    }

    private getComponentType(): typeof this.componentType {
        if (!this.component) {
            return null;
        }

        if (this.config.hasComponentNature("io.openems.edge.ess.dccharger.api.EssDcCharger", this.component.id) && this.component.isEnabled) {
            return { type: "charger", displayName: this.component.alias };
        }

        if (this.config.isProducer(this.component) && this.component.isEnabled) {
            return { type: "productionMeter", displayName: this.component.alias };
        }

        if (this.component.factoryId === "Core.Sum") {
            return { type: "sum", displayName: this.translate.instant("GENERAL.TOTAL") };
        }

        return null;
    }
}
