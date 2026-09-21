import { Component, ChangeDetectionStrategy } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { ChangelogComponent } from "./component/changelog.component";

@Component({
    selector: "changelogViewComponent",
    templateUrl: "./view.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [IonicModule, ChangelogComponent],
})
export class ChangelogViewComponent {}
