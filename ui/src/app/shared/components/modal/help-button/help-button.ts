import { CommonModule } from "@angular/common";
import { Component, Input, SimpleChange, OnChanges } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { Service } from "src/app/shared/shared";
import { environment } from "src/environments";

@Component({
    selector: "oe-help-button",
    templateUrl: "./help-button.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
    ],
})
export class HelpButtonComponent implements OnChanges {

    /** Overwrites default docs link */
    @Input() public useDefaultPrefix: boolean = false;
    @Input() public key: keyof typeof environment.links | null = null;

    protected link: string | null = null;

    constructor(private service: Service) { }

    ngOnChanges(changes: { key: SimpleChange, useDocsPrefix: SimpleChange }) {
        if (changes["key"] || changes["useDocsPrefix"]) {
            this.setLink(changes.key.currentValue, changes.useDocsPrefix.currentValue);
        }
    }

    private setLink(key: HelpButtonComponent["key"], docsBaseLink?: HelpButtonComponent["useDefaultPrefix"]) {
        const docsLink = this.useDefaultPrefix ? "" : environment.docsUrlPrefix.replace("{language}", this.service.getDocsLang());
        if (key == null || !(key in environment.links)) {
            console.error("Key [" + key + "] not found in Environment Links");
            this.link = null;
            return;
        }

        const link = environment.links[key];
        if (link === null || link === "") {
            this.link = null;

        } else {
            this.link = docsLink + environment.links[key];
        }
    }

}
