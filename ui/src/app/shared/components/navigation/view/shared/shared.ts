import { TSignalValue } from "src/app/shared/type/utility";
import { NavigationComponent } from "../../navigation.component";
import { NavigationService } from "../../service/navigation.service";

export namespace ViewUtils {

    export function getTotalHeaderFooterHeight(): { header: number; footer: number } {
        const headers = Array.from(document.querySelectorAll("ion-header"));
        const footers = Array.from(document.querySelectorAll("ion-footer"));

        const headerHeight = headers.reduce((sum, el) => sum + el.clientHeight, 0);
        const footerHeight = footers.reduce((sum, el) => sum + el.clientHeight, 0);

        return { header: headerHeight, footer: footerHeight };
    }

    export function getViewHeight(position: TSignalValue<NavigationService["position"]> | null) {
        const { header, footer } = ViewUtils.getTotalHeaderFooterHeight();
        if (position == null || position == "disabled") {
            return window.innerHeight - header - footer;
        }

        if (position === "bottom") {
            const actionSheetModal = window.innerHeight * NavigationComponent.breakPoint();
            return window.innerHeight - header - footer - actionSheetModal;
        }

        return window.innerHeight - header - footer;
    }
}
