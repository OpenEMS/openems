import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';
import { ChangelogComponent } from './view/component/changelog.component';
import { ChangelogViewComponent } from './view/view';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    ChangelogComponent,
    ChangelogViewComponent
  ],
  exports: [
    ChangelogComponent,
    ChangelogViewComponent
  ]
})
export class ChangelogModule { }
