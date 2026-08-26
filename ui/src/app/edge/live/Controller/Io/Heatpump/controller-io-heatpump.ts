import { NgModule } from "@angular/core";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { CommonConsumptionHistory } from "../../../common/consumption/history/consumption-history";
import { ControllerIoHeatpumpBaseModeComponent } from "./basemode/basemode";
import { ControllerIoHeatpumpComponent } from "./flat/flat";
import { ControllerIoHeatpumpModalComponent } from "./modal/modal";
import { ControllerIoHeatpumpHomeComponent } from "./new-navigation/new-navigation";
import { HeatPumpScheduleComponent } from "./schedule/schedule.component";
import { ControllerIoHeatpumpSettingsComponent } from "./settings/settings";

@NgModule({
    imports: [
        SharedModule,
        ModalModule,
        CommonConsumptionHistory,
        ComponentsBaseModule,
        ControllerIoHeatpumpBaseModeComponent,
        ControllerIoHeatpumpSettingsComponent,
        ControllerIoHeatpumpComponent,
        ControllerIoHeatpumpModalComponent,
        ControllerIoHeatpumpHomeComponent,
        HeatPumpScheduleComponent,
    ],
    exports: [
        ControllerIoHeatpumpHomeComponent,
        ControllerIoHeatpumpComponent,
        ControllerIoHeatpumpModalComponent,
        HeatPumpScheduleComponent,
    ],
})
export class ControllerIoHeatpumpModule {}
