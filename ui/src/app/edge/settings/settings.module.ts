import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { SettingsComponent } from './settings.component';
import { SystemLogComponent } from './systemlog/systemlog.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    SettingsComponent,
    SystemLogComponent
  ]
})
export class SettingsModule { }
