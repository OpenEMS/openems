import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ChangelogRoutingModule } from "./changelog-routing.module";
import { ChangelogComponent } from "./view/component/changelog.component";
import { ChangelogViewComponent } from "./view/view";

@NgModule({
  imports: [
    CommonModule,
    ChangelogComponent,
    ChangelogViewComponent,
    ChangelogRoutingModule,
  ],
  declarations: [
  ],
  exports: [
    ChangelogComponent,
  ],
})
export class ChangelogModule { }
