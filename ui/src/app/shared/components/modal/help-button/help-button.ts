import { CommonModule } from "@angular/common";
import { Component, Input, SimpleChange, OnChanges } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { Service } from "src/app/shared/shared";
import { environment } from "src/environments";

@Component({
    selector: "oe-help-button",
    templateUrl: "./help-BUTTON.HTML",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
    ],
})
export class HelpButtonComponent implements OnChanges {

    /** Overwrites default docs link */
    @Input() public useDefaultPrefix: boolean = false;
    @Input() public key: keyof typeof ENVIRONMENT.LINKS | null = null;

    protected link: string | null = null;

    constructor(private service: Service) { }

    ngOnChanges(changes: { key: SimpleChange, useDocsPrefix: SimpleChange }) {
        if (changes["key"] || changes["useDocsPrefix"]) {
            THIS.SET_LINK(CHANGES.KEY.CURRENT_VALUE, CHANGES.USE_DOCS_PREFIX.CURRENT_VALUE);
        }
    }

    private setLink(key: HelpButtonComponent["key"], docsBaseLink?: HelpButtonComponent["useDefaultPrefix"]) {
        const docsLink = THIS.USE_DEFAULT_PREFIX ? "" : ENVIRONMENT.DOCS_URL_PREFIX.REPLACE("{language}", THIS.SERVICE.GET_DOCS_LANG());
        if (key == null || !(key in ENVIRONMENT.LINKS)) {
            CONSOLE.ERROR("Key [" + key + "] not found in Environment Links");
            THIS.LINK = null;
            return;
        }

        const link = ENVIRONMENT.LINKS[key];
        if (link === null || link === "") {
            THIS.LINK = null;

        } else {
            THIS.LINK = docsLink + ENVIRONMENT.LINKS[key];
        }
    }

}
