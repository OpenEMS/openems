import { NgModule } from '@angular/core';
import { SharedModule } from './../../../shared/shared.module';
import { EvcsComponent } from './evcs/evcs.component';
import { ModbusApiComponent } from './modbusapi/modbusapi.component';
import { WidgetComponent } from './widget.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    WidgetComponent,
    EvcsComponent,
    ModbusApiComponent
  ],
  exports: [
    WidgetComponent
  ]
})
export class WidgetModule { }



