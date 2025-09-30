import { NgModule } from "@angular/core";
import { ChannelThreshold } from "./ChannelThreshold/CHANNEL_THRESHOLD.MODULE";
import { ControllerEss } from "./Ess/ESS.MODULE";
import { GridOptimizeCharge } from "./Ess/GridoptimizedCharge/GRID_OPTIMIZE_CHARGE.MODULE";
import { TimeOfUseTariff } from "./Ess/TimeOfUseTariff/TIME_OF_USE_TARIFF.MODULE";
import { ControllerHeat } from "./Heat/HEAT.MODULE";
import { ControllerIo } from "./Io/IO.MODULE";
import { ModbusTcpApi } from "./ModbusTcpApi/MODBUS_TCP_API.MODULE";

@NgModule({
  imports: [
    ControllerEss,
    ControllerHeat,
    ControllerIo,
    ChannelThreshold,
    TimeOfUseTariff,
    ModbusTcpApi,
    GridOptimizeCharge,
  ],
  exports: [
    ControllerEss,
    ControllerHeat,
    ControllerIo,
    ChannelThreshold,
    TimeOfUseTariff,
    ModbusTcpApi,
    GridOptimizeCharge,
  ],
})
export class Controller { }
