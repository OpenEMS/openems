import { TSignalValue } from "src/app/shared/type/utility";
import { NavigationComponent } from "../../NAVIGATION.COMPONENT";
import { NavigationService } from "../../service/NAVIGATION.SERVICE";

export namespace ViewUtils {

    export function getTotalHeaderFooterHeight(): { header: number; footer: number } {
        const headers = ARRAY.FROM(DOCUMENT.QUERY_SELECTOR_ALL("ion-header"));
        const footers = ARRAY.FROM(DOCUMENT.QUERY_SELECTOR_ALL("ion-footer"));

        const headerHeight = HEADERS.REDUCE((sum, el) => sum + EL.CLIENT_HEIGHT, 0);
        const footerHeight = FOOTERS.REDUCE((sum, el) => sum + EL.CLIENT_HEIGHT, 0);

        return { header: headerHeight, footer: footerHeight };
    }

    export function getViewHeight(position: TSignalValue<NavigationService["position"]> | null) {
        const { header, footer } = VIEW_UTILS.GET_TOTAL_HEADER_FOOTER_HEIGHT();
        if (position == null || position == "disabled") {
            return WINDOW.INNER_HEIGHT - header - footer;
        }

        if (position === "bottom") {
            const actionSheetModal = WINDOW.INNER_HEIGHT * NAVIGATION_COMPONENT.BREAK_POINT();
            return WINDOW.INNER_HEIGHT - header - footer - actionSheetModal;
        }

        return WINDOW.INNER_HEIGHT - header - footer;
    }
}
