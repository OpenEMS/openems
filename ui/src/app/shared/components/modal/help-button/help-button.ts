import { Component, Input, OnChanges, SimpleChange, ChangeDetectionStrategy } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { Service } from "src/app/shared/shared";
import { TFlattenKeys } from "src/app/shared/type/utility";
import { ObjectUtils } from "src/app/shared/utils/object/object-utils";
import { Environment, environment } from "src/environments";

@Component({
    selector: "oe-help-button",
    templateUrl: "./help-button.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [IonicModule],
})
export class HelpButtonComponent implements OnChanges {
    /** Overwrites default docs link */
    @Input() public useDefaultPrefix: boolean = true;
    @Input() public key: TFlattenKeys<typeof environment.links> | null = null;
    @Input() public color: string = "var(--ion-title-color)";

    protected link: string | null = null;

    constructor(private service: Service) {}

    /**
     * Sets the link to navigate to.
     *
     * @param key The key
     * @param useDefaultPrefix If default docs prefix should be used
     * @returns A link, or if key not found in environment.links null
     */
    public static getLink(
        key: HelpButtonComponent["key"],
        service: Service,
        useDefaultPrefix?: HelpButtonComponent["useDefaultPrefix"],
    ) {
        const flattenedKeys = ObjectUtils.flattenObjectWithValues<Environment["links"]>(environment.links);

        if (key == null || !(key in flattenedKeys)) {
            console.error("Key [" + key + "] not found in Environment Links");
            return null;
        }

        const link = flattenedKeys[key];
        if (link === null || link === "") {
            return null;
        }

        if (useDefaultPrefix === true) {
            return environment.docsUrlPrefix.replace("{language}", service.getDocsLang()) + link;
        }

        return link.replace("{language}", service.getDocsLang());
    }

    ngOnChanges(changes: { key: SimpleChange; useDefaultPrefix: SimpleChange }) {
        if (changes["key"] || changes["useDefaultPrefix"]) {
            this.link = HelpButtonComponent.getLink(
                changes.key?.currentValue ?? null,
                this.service,
                changes.useDefaultPrefix?.currentValue ?? true,
            );
        }
    }
}
