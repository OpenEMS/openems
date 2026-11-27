import { AfterViewInit, Component, computed, inject, OnDestroy } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";

import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { Service } from "src/app/shared/shared";
import { ChartComponent } from "../chart/chart";

@Component({
    selector: "oe-history",
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [CommonUiModule, ComponentsBaseModule, ChartComponent],
})
export class HistoryChartComponent implements AfterViewInit, OnDestroy {
    protected readonly service = inject(Service);
    protected readonly translate = inject(TranslateService);
    protected edge = computed(() => this.service.currentEdge());
    private navigationService: NavigationService = inject(NavigationService);

    ngAfterViewInit() {
        this.navigationService.headerTitle.set({ text: this.translate.instant("GENERAL.HISTORY") });
    }

    ngOnDestroy() {
        if (this.navigationService.headerTitle()?.text == this.translate.instant("GENERAL.HISTORY")) {
            this.navigationService.headerTitle.set({ text: null });
        }
    }
}
