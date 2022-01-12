import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { InstallAppComponent } from './install.component';
import { IndexComponent } from './index.component';
import { UpdateAppComponent } from './update.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    IndexComponent,
    InstallAppComponent,
    UpdateAppComponent,
  ],
  exports: [
    IndexComponent,
    InstallAppComponent,
    UpdateAppComponent,
  ],
})
export class AppModule { }
