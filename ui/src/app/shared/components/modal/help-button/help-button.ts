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
    IonicModule,
],
})
export class HelpButtonComponent implements OnChanges {

    /** Overwrites default docs link */
    @Input() public useDefaultPrefix: boolean = true;
    @Input() public key: keyof typeof environment.links | null = null;

    protected link: string | null = null;

    constructor(private service: Service) { }

    ngOnChanges(changes: { key: SimpleChange, useDefaultPrefix: SimpleChange }) {
        if (changes["key"] || changes["useDefaultPrefix"]) {
            this.setLink(changes.key?.currentValue ?? null, changes.useDefaultPrefix?.currentValue ?? true);
        }
    }

    /**
     * Sets the link to navigate to.
     *
     * @param key the key
     * @param useDefaultPrefix if default docs prefix should be used
     * @returns a link, or if key not found in environment.links null
     */
    private setLink(key: HelpButtonComponent["key"], useDefaultPrefix?: HelpButtonComponent["useDefaultPrefix"]) {
        const flattenedKeys = ObjectUtils.flattenObjectWithValues<Environment["links"]>(environment.links);
        if (key == null || !(key in flattenedKeys)) {
            console.error("Key [" + key + "] not found in Environment Links");
            this.link = null;
            return;
        }

        const link = flattenedKeys[key];
        if (link === null || link === "") {
            this.link = null;
            return;
        }

        if (useDefaultPrefix === true) {
            this.link = environment.docsUrlPrefix.replace("{language}", this.service.getDocsLang()) + link;
            return;
        }

        this.link = link;
    }
}
