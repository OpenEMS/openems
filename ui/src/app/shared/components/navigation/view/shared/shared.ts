import { TSignalValue } from "src/app/shared/type/utility";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { NavigationComponent } from "../../action-sheet-modal";
import { NavigationService } from "../../service/navigation.service";

export namespace ViewUtils {

    export function getTotalHeaderFooterHeight(): { header: number; footer: number } {
        const bars = getVisibleBars();

        const header = bars.headers;
        const footers = bars.footers;

        const headerHeight = header.reduce((sum, el) => sum + el.clientHeight, 0);
        const footerHeight = footers.reduce((sum, el) => sum + el.clientHeight, 0);

        return { header: headerHeight, footer: footerHeight };
    }


    // Ionic cached pages remain in the DOM even after navigating back.
    // This becomes a problem when reloading on routes like history/autarchy or history/production:
    // After a reload, the previous route's <ion-footer> or <oe-footer-subnavigation>
    // stays in the DOM (but is visually hidden). When returning to the Energy Monitor page,
    // these cached elements would still be detected and included in the height calculation.
    function getVisibleBars(): { headers: HTMLElement[]; footers: HTMLElement[] } {
        const allHeaders = Array.from(
            document.querySelectorAll<HTMLElement>("ion-header")
        );

        const allIonFooters = Array.from(
            document.querySelectorAll<HTMLElement>("ion-footer")
        );

        const isVisible = (el: HTMLElement) => {
            const rect = el.getBoundingClientRect();

            if (rect.width === 0 || rect.height === 0) {
                return false;
            }
            const style = window.getComputedStyle(el);
            if (parseFloat(style.opacity || "1") === 0) {
                return false;
            }

            return true;
        };

        return {
            headers: allHeaders.filter(isVisible),
            footers: allIonFooters.filter(isVisible),
        };
    }

    export function getWindowVisualViewPort() {
        return window.visualViewport?.height ?? window.innerHeight;
    }

    export function getViewHeightInPx(position: TSignalValue<NavigationService["position"]> | null) {
        const { header, footer } = ViewUtils.getTotalHeaderFooterHeight();

        if (position == null || position == "disabled") {
            return getWindowVisualViewPort() - header - footer;
        }
        if (position === "bottom") {
            const actionSheetModal = getActionSheetModalHeightInPx();
            return getWindowVisualViewPort() - header - footer - actionSheetModal;
        }

        return getWindowVisualViewPort() - header - footer;
    }

    export function getActionSheetModalHeightInPx() {
        return getWindowVisualViewPort() * NavigationComponent.INITIAL_BREAKPOINT;
    }

    export function getActionSheetModalHeightInVh(position: TSignalValue<NavigationService["position"]> | null) {
        if (position == "bottom") {
            return (getActionSheetModalHeightInPx() / getWindowVisualViewPort()) * 100;
        }
        return 0;
    }

    /**
    * Gets the available chart content height in [vh].
    *
    * @param windowHeight the window height
    * @param customChartHeightPercentage optional chart height in percent (0–100) to scale the available height to.
    * @returns the available height
    */
    export function getChartContentHeightInVh(windowHeight: number, position: TSignalValue<NavigationService["position"]> | null, customChartHeightPercentage?: number | null): number | null {
        return NumberUtils.multiplySafely(NumberUtils.divideSafely(ViewUtils.getViewHeightInPx(position), getWindowVisualViewPort()), 100);
    }
}
