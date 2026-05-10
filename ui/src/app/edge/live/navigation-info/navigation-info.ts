import { Component } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { environment } from "src/environments";
import { LiveDataService } from "../livedataservice";

@Component({
    templateUrl: "./navigation-info.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    styles: [`
        ::ng-deep formly-form{
            height: 100% !important;
        }`,
    ],
})
export class NavigationInfoComponent extends AbstractModal {
    protected link = environment.links.REDIRECT.BETA_CHANGE_LOG;;
}
