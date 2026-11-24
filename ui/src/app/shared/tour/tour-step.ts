import { TutorialTargetId } from "./tutorial-target-id";

export type TourPosition = "top" | "right" | "bottom" | "left" | "center" | "auto";

export interface TourTarget {
    /** Value of the target element's `data-tutorial-id` attribute. */
    id: TutorialTargetId;

    /** Optional selector for a descendant to highlight instead of the target element. */
    childSelector?: string;
}

export interface TourContentItem {
    text: string;
    color: string;
}

export interface TourContentBlock {
    heading?: string;
    text?: string;
    items?: TourContentItem[];
}

export interface TourStep {
    id: string;
    /**
     * UI version in which the step was added or last changed notably, e.g. `2026.11.1`.
     *
     * Users who finished or dismissed the tour with an older UI version get the tour automatically again.
     */
    version: string;
    title?: string;
    /** Plain text shown when no `contentBlocks` are configured. */
    text?: string;
    contentBlocks?: TourContentBlock[];
    targets?: TourTarget[];
    handbookUrl?: string | null;
    position?: TourPosition;
    optional?: boolean;
    highlight?: boolean;
    openParentAccordion?: boolean;
    closeOpenedAccordions?: boolean;
    closeAllAccordions?: boolean;
    route?: string;
    beforeShow?: () => void | Promise<void>;
}
