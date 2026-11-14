import { compareVersions } from "compare-versions";
import { TourStep } from "./tour-step";

export interface IonicAccordionElement extends HTMLElement {
    value?: string;
}

export interface IonicAccordionGroupElement extends HTMLElement {
    value: string | string[] | null;
}

export interface HighlightRect {
    top: number;
    left: number;
    width: number;
    height: number;
}

/**
 * Expands target rectangles by a padding without letting neighbouring highlights overlap.
 *
 * Targets that are separated (or just touching) would overlap once both are padded. For those, the facing edges are
 * moved to the middle between both targets, leaving a gap of one padding between the highlights.
 *
 * @param rects Viewport rectangles of the highlighted targets.
 * @param padding Space added around each target.
 * @returns The padded highlight rectangles, in the order of `rects`.
 */
export function getPaddedHighlightRects(rects: DOMRect[], padding: number): HighlightRect[] {
    const rectangles: Rectangles[] = rects.map((rect) => ({
        top: rect.top - padding,
        left: rect.left - padding,
        right: rect.right + padding,
        bottom: rect.bottom + padding,
    }));

    for (let i = 0; i < rects.length; i++) {
        for (let j = i + 1; j < rects.length; j++) {
            if (areOverlapping(rectangles[i], rectangles[j])) {
                separatePaddedEdges(rects, rectangles, [i, j], padding);
            }
        }
    }

    return rectangles.map((edge) => ({
        top: edge.top,
        left: edge.left,
        width: edge.right - edge.left,
        height: edge.bottom - edge.top,
    }));
}

interface Rectangles {
    top: number;
    left: number;
    right: number;
    bottom: number;
}

function areOverlapping(first: Rectangles, second: Rectangles): boolean {
    return (
        first.left < second.right && second.left < first.right && first.top < second.bottom && second.top < first.bottom
    );
}

/**
 * Moves the facing padded edges of two separated targets to the middle between them.
 *
 * Targets that overlap themselves stay unchanged, as no gap between them exists.
 *
 * @param rects Viewport rectangles of the highlighted targets.
 * @param edges Padded edges per target, adjusted in place.
 * @param pair Indices of the two targets.
 * @param padding Space added around each target.
 */
function separatePaddedEdges(rects: DOMRect[], edges: Rectangles[], [i, j]: [number, number], padding: number): void {
    const [first, second] = [rects[i], rects[j]];

    if (first.right <= second.left || second.right <= first.left) {
        const [leftIndex, rightIndex] = first.right <= second.left ? [i, j] : [j, i];
        const middle = (rects[leftIndex].right + rects[rightIndex].left) / 2;
        edges[leftIndex].right = Math.min(edges[leftIndex].right, middle - padding / 2);
        edges[rightIndex].left = Math.max(edges[rightIndex].left, middle + padding / 2);
        return;
    }

    if (first.bottom <= second.top || second.bottom <= first.top) {
        const [topIndex, bottomIndex] = first.bottom <= second.top ? [i, j] : [j, i];
        const middle = (rects[topIndex].bottom + rects[bottomIndex].top) / 2;
        edges[topIndex].bottom = Math.min(edges[topIndex].bottom, middle - padding / 2);
        edges[bottomIndex].top = Math.max(edges[bottomIndex].top, middle + padding / 2);
    }
}

/**
 * Calculates the area two rectangles share.
 *
 * @param first First rectangle.
 * @param second Second rectangle.
 * @returns The overlapping area, or `0` if the rectangles do not overlap.
 */
export function getOverlapArea(first: HighlightRect, second: HighlightRect): number {
    const width = Math.min(first.left + first.width, second.left + second.width) - Math.max(first.left, second.left);
    const height = Math.min(first.top + first.height, second.top + second.height) - Math.max(first.top, second.top);

    return width > 0 && height > 0 ? width * height : 0;
}

/**
 * Selects the tooltip placement that hides the least of the highlighted targets.
 *
 * Covering the navigation weighs more than covering highlights, as the steps explain its entries.
 *
 * @param placements Possible tooltip areas, in order of preference.
 * @param highlights Highlighted areas of the step.
 * @param navigation Area of the navigation, or `null` if it is not shown.
 * @returns The first placement covering neither a highlight nor the navigation, otherwise the one covering the least
 *   navigation and then the least highlighted area, or `null` without placements.
 */
export function selectTooltipPlacement<T extends HighlightRect>(
    placements: T[],
    highlights: HighlightRect[],
    navigation: HighlightRect | null,
): T | null {
    let bestPlacement: T | null = null;
    let smallestCoveredArea = { navigation: Number.POSITIVE_INFINITY, highlights: Number.POSITIVE_INFINITY };

    for (const placement of placements) {
        const coveredArea = {
            navigation: navigation == null ? 0 : getOverlapArea(placement, navigation),
            highlights: highlights.reduce((sum, highlight) => sum + getOverlapArea(placement, highlight), 0),
        };
        if (coveredArea.navigation === 0 && coveredArea.highlights === 0) {
            return placement;
        }

        const isBetter =
            coveredArea.navigation < smallestCoveredArea.navigation ||
            (coveredArea.navigation === smallestCoveredArea.navigation &&
                coveredArea.highlights < smallestCoveredArea.highlights);
        if (isBetter) {
            smallestCoveredArea = coveredArea;
            bestPlacement = placement;
        }
    }

    return bestPlacement;
}

/**
 * Calculates the smallest rectangle containing all given rectangles.
 *
 * @param rects Rectangles to enclose; must not be empty.
 * @returns The enclosing rectangle.
 */
export function getBoundingRect(rects: DOMRect[]): DOMRect {
    const left = Math.min(...rects.map((rect) => rect.left));
    const top = Math.min(...rects.map((rect) => rect.top));
    const right = Math.max(...rects.map((rect) => rect.right));
    const bottom = Math.max(...rects.map((rect) => rect.bottom));

    return new DOMRect(left, top, right - left, bottom - top);
}

/**
 * Normalizes an Ionic accordion group's value to an array.
 *
 * @param value Current accordion group value.
 * @returns A new array containing the open values, or an empty array.
 */
export function getOpenAccordionValues(value: IonicAccordionGroupElement["value"]): string[] {
    if (Array.isArray(value)) {
        return [...value];
    }

    return value == null ? [] : [value];
}

export interface ActiveTourTargetOptions {
    /** Selector for a descendant to resolve instead of the target element itself. */
    childSelector?: string;
    /** Whether to resolve hidden targets inside a closed accordion. */
    allowClosedAccordion?: boolean;
}

/**
 * Finds the active DOM element for a tour target, ignoring cached or hidden Ionic pages.
 *
 * @param targetId The value of the target element's `data-tutorial-id` attribute.
 * @param options How the target is resolved; by default the visible target element itself.
 * @returns The first matching element on an active page, or `null` if no matching element exists.
 */
export function getActiveTourTarget(targetId: string, options: ActiveTourTargetOptions = {}): HTMLElement | null {
    const { childSelector, allowClosedAccordion = false } = options;
    const parents = document.querySelectorAll<HTMLElement>(`[data-tutorial-id="${targetId}"]`);

    for (const parent of parents) {
        const target = childSelector == null ? parent : parent.querySelector<HTMLElement>(childSelector);

        // Ignore cached or aria-hidden elements, including nested child targets.
        if (
            target == null ||
            target.closest(".ion-page-hidden") != null ||
            target.closest('[aria-hidden="true"]') != null ||
            (!isVisibleTourTarget(target) && !(allowClosedAccordion && isInsideClosedAccordion(target)))
        ) {
            continue;
        }

        return target;
    }

    return null;
}

function isVisibleTourTarget(target: HTMLElement): boolean {
    const style = window.getComputedStyle(target);
    if (style.display === "none" || style.visibility === "hidden" || style.visibility === "collapse") {
        return false;
    }

    const rect = target.getBoundingClientRect();
    return target.getClientRects().length > 0 && (rect.width > 0 || rect.height > 0);
}

function isInsideClosedAccordion(target: HTMLElement): boolean {
    let accordion = target.closest<IonicAccordionElement>("ion-accordion");

    while (accordion != null) {
        const accordionValue = accordion.value ?? accordion.getAttribute("value");
        const accordionGroup = accordion.closest<IonicAccordionGroupElement>("ion-accordion-group");

        if (accordionValue != null && accordionValue !== "" && accordionGroup != null) {
            if (!getOpenAccordionValues(accordionGroup.value).includes(accordionValue)) {
                return true;
            }
        }

        accordion = accordion.parentElement?.closest<IonicAccordionElement>("ion-accordion") ?? null;
    }

    return false;
}

/**
 * Determines the version of a tour from its steps.
 *
 * @param steps Steps of the tour.
 * @returns The highest step version, or `null` for a tour without steps.
 */
export function getTourVersion(steps: TourStep[]): string | null {
    return steps.reduce<string | null>(
        (highest, step) => (highest == null || compareVersions(step.version, highest) > 0 ? step.version : highest),
        null,
    );
}

/**
 * Measures an element without its CSS transform, e.g. while it is scaled during an animation.
 *
 * @param element Element to measure.
 * @returns A rectangle at the origin with the layout size of the element.
 */
export function getUntransformedSize(element: HTMLElement): DOMRect {
    return new DOMRect(0, 0, element.offsetWidth, element.offsetHeight);
}
