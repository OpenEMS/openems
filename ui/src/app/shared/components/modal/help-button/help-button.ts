import { CommonModule } from "@angular/common";
import { Component, Input, OnChanges, SimpleChange } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { Service } from "src/app/shared/shared";
import { ObjectUtils } from "src/app/shared/utils/object/object.utils";
import { Environment, environment } from "src/environments";

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

    ngOnChanges(changes: { key: SimpleChange, useDefaultPrefix: SimpleChange }) {
        if (changes["key"] || changes["useDefaultPrefix"]) {
            this.setLink(changes.key.currentValue, changes.useDefaultPrefix.currentValue);
        }
    }

    private setLink(key: HelpButtonComponent["key"], docsBaseLink?: HelpButtonComponent["useDefaultPrefix"]) {
        const flattenedKeys = ObjectUtils.flattenObjectWithValues<Environment["links"]>(environment.links);
        if (key == null || !(key in flattenedKeys)) {
            console.error("Key [" + key + "] not found in Environment Links");
            this.link = null;
            return;
        }

        const link = flattenedKeys[key];
        if (link === null || link === "") {
            this.link = null;

        } else {
            this.link = link;
        }
    }

}
