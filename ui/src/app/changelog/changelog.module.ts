import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';
import { ChangelogComponent } from './changelog.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    ChangelogComponent,
  ]
})
export class ChangelogModule { }
