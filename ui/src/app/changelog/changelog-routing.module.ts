import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { ChangelogComponent } from "./view/component/changelog.component";

const routes: Routes = [
    {
        path: "",
        component: ChangelogComponent,
    },
];
@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class ChangelogRoutingModule { }
