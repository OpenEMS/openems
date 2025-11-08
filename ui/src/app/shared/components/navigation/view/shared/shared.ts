import { TSignalValue } from "src/app/shared/type/utility";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
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

    export function getViewHeightInPx(position: TSignalValue<NavigationService["position"]> | null) {
        const { header, footer } = ViewUtils.getTotalHeaderFooterHeight();
        if (position == null || position == "disabled") {
            return window.innerHeight - header - footer;
        }

        if (position === "bottom") {
            const actionSheetModal = getActionSheetModalHeightInPx();
            return window.innerHeight - header - footer - actionSheetModal;
        }

        return window.innerHeight - header - footer;
    }

    export function getActionSheetModalHeightInPx() {
        return window.innerHeight * NavigationComponent.INITIAL_BREAKPOINT;
    }

    export function getActionSheetModalHeightInVh(position: TSignalValue<NavigationService["position"]> | null) {
        if (position == "bottom") {
            return (getActionSheetModalHeightInPx() / window.innerHeight) * 100;
        }
        return 0;
    }

    /**
    * Gets the available chart content height in [vh].
    *
    * @param windowHeight the window height
    * @returns the available height
    */
    export function getChartContentHeightInVh(windowHeight: number, position: TSignalValue<NavigationService["position"]> | null) {
        const rawViewHeight = ViewUtils.getViewHeightInPx(position);
        const ionPaddingInPx: string = getComputedStyle(document.documentElement).getPropertyValue("--ion-padding");
        const ionPadding: number = NumberUtils.parseNumberSafelyOrElse(
            StringUtils.splitByGetIndexSafely(ionPaddingInPx, "px", 0)
            , 0);
        if (position === "bottom") {
            return NumberUtils.multiplySafely(
                NumberUtils.divideSafely(
                    NumberUtils.subtractSafely(
                        rawViewHeight,
                        NumberUtils.multiplySafely(ionPadding,
                            5 /** Needed only for chart height purposes, TODO: find way to get the chart padding async dynamically*/)),
                    windowHeight),
                100);
        }
        return null;
    }
}
