import { environment } from "src/environments";
import { Service } from "../../service/service";

/**
 * Manages links to documentation pages
 */
export class DocsUtils {
    public static createDataProtectionLink(service: Service) {
        const link = environment.links.DATA_PROTECTION;

        if (link == null) {
            return null;
        }
        return link.replace("{language}", service.getDocsLang());
    }
}
