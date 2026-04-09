import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule, Routes } from "@angular/router";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { CommonConsumptionHistory } from "../../../common/consumption/history/consumption-history";
import { ControllerIoHeatpumpComponent } from "./flat/flat";
import { HeatPumpHistory } from "./history/controller-io-heatpump-history";
import { ControllerIoHeatpumpOverviewComponent } from "./history/overview/overview";
import { ControllerIoHeatpumpModalComponent } from "./modal/modal";
import { ControllerIoHeatpumpHomeComponent } from "./new-navigation/new-navigation";
import { ControllerIoHeatpumpSettingsComponent } from "./settings/settings";

const routes: Routes = [
    {
        path: "",
        component: ControllerIoHeatpumpOverviewComponent,
    },
];

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
        CommonConsumptionHistory,
        ComponentsBaseModule,
        HeatPumpHistory,
        ControllerIoHeatpumpSettingsComponent,
        ControllerIoHeatpumpComponent,
        ControllerIoHeatpumpModalComponent,
        ControllerIoHeatpumpHomeComponent,
        RouterModule.forChild(routes),
    ],
    exports: [
        ControllerIoHeatpumpHomeComponent,
        ControllerIoHeatpumpComponent,
        ControllerIoHeatpumpModalComponent,
    ],
})
export class ControllerIoHeatpumpModule { }
