import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { SettingsComponent } from './settings.component';
import { SystemLogComponent } from './systemlog/systemlog.component';
import { ComponentInstallComponent } from './componentInstall/componentInstall.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    SettingsComponent,
    SystemLogComponent,
    ComponentInstallComponent
  ]
})
export class SettingsModule { }
