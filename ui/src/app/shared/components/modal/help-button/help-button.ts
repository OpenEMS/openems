import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChange } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { Service } from "src/app/shared/shared";
import { TFlattenKeys } from "src/app/shared/type/utility";
import { DocsUtils } from "src/app/shared/utils/docs/docs.utils";
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
    @Input() public key: TFlattenKeys<typeof environment.links> | null = null;
    @Input() public color: string = "var(--ion-title-color)";

    protected link: string | null = null;

    constructor(private service: Service) {}

    /**
     * Sets the link to navigate to.
     *
     * @param key The key
     * @returns A link, or if key not found in environment.links null
     */
    public static getLink(key: HelpButtonComponent["key"], service: Service) {
        const flattenedKeys = ObjectUtils.flattenObjectWithValues<Environment["links"]>(environment.links);

        if (key == null || !(key in flattenedKeys)) {
            console.error("Key [" + key + "] not found in Environment Links");
            return null;
        }

        const link = flattenedKeys[key];
        if (link === null || link === "") {
            return null;
        }

        return DocsUtils.replaceDocsLanguage(link, service.getDocsLang());
    }

    ngOnChanges(changes: { key: SimpleChange }) {
        if (changes["key"]) {
            this.link = HelpButtonComponent.getLink(changes.key?.currentValue ?? null, this.service);
        }
    }
}
