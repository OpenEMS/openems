import { Component, DestroyRef, ElementRef, HostListener, computed, effect, inject, signal, viewChild, } from "@angular/core";

import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { addIcons } from "ionicons";
import { NAVIGATION_TOUR } from "./navigation-tour";
import { TourPosition, TourStep } from "./tour-step";
import { TourService } from "./tour.service";
import { IonicAccordionElement, IonicAccordionGroupElement, getActiveTourTarget, getBoundingRect, getOpenAccordionValues, getPaddedHighlightRects, getUntransformedSize, selectTooltipPlacement, } from "./tour.utils";
import { TutorialTargetId } from "./tutorial-target-id";

interface TooltipPosition {
    top: number;
    left: number;
}

interface OpenedAccordion {
    group: IonicAccordionGroupElement;
    value: string;
}

type AccordionGroupValue = IonicAccordionGroupElement["value"];

@Component({
    selector: "app-tour-overlay",
    standalone: true,
    imports: [IonicModule, TranslateModule],
    templateUrl: "./tour-overlay.component.html",
    styleUrl: "./tour-overlay.component.scss",
})
export class TourOverlayComponent {
    private readonly tooltip = viewChild<ElementRef<HTMLElement>>("tooltip");

    private readonly tourService = inject(TourService);
    /** Avatar of the theme tour presenting the step texts, if the theme provides one. */
    private readonly avatar = inject(NAVIGATION_TOUR)?.avatar ?? null;

    private readonly highlightRects = signal<DOMRect[]>([]);
    private readonly viewportSize = signal({ width: window.innerWidth, height: window.innerHeight });
    private readonly tooltipPosition = signal<TooltipPosition | null>(null);

    private readonly paddedHighlightRects = computed(() =>
        getPaddedHighlightRects(this.highlightRects(), this.highlightPadding),
    );

    private readonly highlightStyles = computed(() =>
        this.paddedHighlightRects().map((rect) => ({
            top: `${rect.top}px`,
            left: `${rect.left}px`,
            width: `${rect.width}px`,
            height: `${rect.height}px`,
        })),
    );

    private tourOpenedAccordions: OpenedAccordion[] = [];
    private readonly originalAccordionValues = new Map<IonicAccordionGroupElement, AccordionGroupValue>();
    private hasCapturedAccordionState = false;
    private tooltipResizeObserver: ResizeObserver | null = null;
    private observedTooltip: HTMLElement | null = null;
    // Pages often finish rendering after the step is shown (e.g. charts), so highlights follow target size changes
    private readonly targetResizeObserver = new ResizeObserver(() => this.updatePosition());
    private readonly host: HTMLElement = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
    private inertElements: HTMLElement[] = [];

    constructor() {
        if (this.avatar != null) {
            addIcons({ [this.avatar.name]: this.avatar.src });
        }

        // Ionic scrolls inside ion-content instead of the window, so scroll events must be captured on the document.
        const onScroll = (): void => this.updatePosition();
        document.addEventListener("scroll", onScroll, { capture: true, passive: true });
        inject(DestroyRef).onDestroy(() => {
            document.removeEventListener("scroll", onScroll, { capture: true });
            this.tooltipResizeObserver?.disconnect();
            this.targetResizeObserver.disconnect();
            this.setPageInert(false);
        });

        effect(() => {
            this.setPageInert(this.tourService.isActive());
        });

        effect(() => {
            const step = this.tourService.activeStep();

            this.observeTargetSizes();

            if (step == null) {
                this.tooltipResizeObserver?.disconnect();
                this.tooltipResizeObserver = null;
                this.observedTooltip = null;
                this.highlightRects.set([]);
                this.tooltipPosition.set(null);
                this.restoreAccordionState();
                return;
            }

            if (!this.hasCapturedAccordionState) {
                this.captureAccordionState();
            }

            if (step.position === "center") {
                this.highlightRects.set([]);
                this.applyAccordionClosing(step.closeOpenedAccordions === true, step.closeAllAccordions === true);

                requestAnimationFrame(() => {
                    this.centerTooltip();
                });

                return;
            }

            this.prepareStep(
                step.targets,
                step.openParentAccordion === true,
                step.closeOpenedAccordions === true,
                step.closeAllAccordions === true,
            );
        });
    }

    private get horizontalInset(): number {
        return window.innerWidth * 0.04;
    }

    private get verticalInset(): number {
        return window.innerHeight * 0.06;
    }

    private get targetGap(): number {
        return Math.min(window.innerWidth, window.innerHeight) * 0.04;
    }

    private get highlightPadding(): number {
        const viewport = this.viewportSize();
        return Math.min(viewport.width, viewport.height) * 0.005;
    }

    @HostListener("window:resize")
    private onResize(): void {
        this.viewportSize.set({ width: window.innerWidth, height: window.innerHeight });
        this.updatePosition();
    }

    /**
     * Makes the rest of the app inert while the tour is active, so it cannot be focused or used via keyboard.
     *
     * Only elements that were not inert before are changed and later restored.
     *
     * @param inert Whether the page next to the overlay should be inert.
     */
    private setPageInert(inert: boolean): void {
        if (!inert) {
            for (const element of this.inertElements) {
                element.inert = false;
            }
            this.inertElements = [];
            return;
        }

        if (this.inertElements.length > 0) {
            return;
        }

        const siblings = Array.from(this.host.parentElement?.children ?? []).filter(
            (element): element is HTMLElement => element instanceof HTMLElement && element !== this.host,
        );
        this.inertElements = siblings.filter((element) => !element.inert);

        for (const element of this.inertElements) {
            element.inert = true;
        }
    }

    private applyAccordionClosing(closeOpenedAccordions: boolean, closeAllAccordions: boolean): void {
        if (closeOpenedAccordions) {
            this.closeTourOpenedAccordions();
        }

        if (closeAllAccordions) {
            this.closeAllOpenedAccordions();
        }
    }

    private centerTooltip(): void {
        const tooltip = this.tooltip()?.nativeElement;

        if (tooltip == null) {
            return;
        }

        this.observeTooltipSize(tooltip);

        const rect = getUntransformedSize(tooltip);

        this.tooltipPosition.set(
            this.getPositionInsideViewport(
                {
                    left: (window.innerWidth - rect.width) / 2,
                    top: (window.innerHeight - rect.height) / 2,
                },
                rect,
            ),
        );
    }

    /**
     * Observes tooltip size changes and recalculates its position as content resizes.
     *
     * @param tooltip Tooltip element to observe.
     */
    private observeTooltipSize(tooltip: HTMLElement): void {
        if (this.observedTooltip === tooltip) {
            return;
        }

        this.tooltipResizeObserver?.disconnect();
        this.observedTooltip = tooltip;
        this.tooltipResizeObserver = new ResizeObserver(() => {
            const step = this.tourService.activeStep();

            if (step == null) {
                return;
            }

            if (step.position === "center") {
                this.centerTooltip();
                return;
            }

            const target = this.getStepTarget();

            if (target != null) {
                this.calculateTooltipPosition(step.position ?? "auto", target.getBoundingClientRect());
            }
        });
        this.tooltipResizeObserver.observe(tooltip);
    }

    /**
     * Observes the targets of the active step, so highlights follow when their size changes.
     *
     * Targets inside accordions the step opens are included, so they are tracked once the accordion has expanded.
     */
    private observeTargetSizes(): void {
        this.targetResizeObserver.disconnect();

        const step = this.tourService.activeStep();
        const targets = (step?.targets ?? [])
            .map((target) =>
                getActiveTourTarget(target.id, {
                    childSelector: target.childSelector,
                    allowClosedAccordion: step?.openParentAccordion === true,
                }),
            )
            .filter((target): target is HTMLElement => target !== null);

        for (const target of targets) {
            this.targetResizeObserver.observe(target);
        }
    }

    /**
     * Resolves the primary target, applies accordion options, and scrolls it into view.
     *
     * @param targets Targets configured for the active step; the first is the primary target.
     * @param openParentAccordion Whether to open ancestor accordion groups.
     * @param closeOpenedAccordions Whether to close accordions opened by earlier tour steps.
     * @param closeAllAccordions Whether to close every accordion group before opening ancestors.
     */
    private prepareStep(
        targets: TourStep["targets"],
        openParentAccordion: boolean,
        closeOpenedAccordions: boolean,
        closeAllAccordions: boolean,
    ): void {
        const primaryTarget = targets?.[0] ?? null;

        if (primaryTarget == null) {
            return;
        }

        const parentTarget = getActiveTourTarget(primaryTarget.id, { allowClosedAccordion: openParentAccordion });
        const target = getActiveTourTarget(primaryTarget.id, {
            childSelector: primaryTarget.childSelector,
            allowClosedAccordion: openParentAccordion,
        });

        if (parentTarget == null || target == null) {
            return;
        }

        this.applyAccordionClosing(closeOpenedAccordions, closeAllAccordions);

        if (openParentAccordion) {
            this.openParentAccordions(parentTarget);
        }

        if (openParentAccordion || closeOpenedAccordions || closeAllAccordions) {
            this.trackTargetDuringTransition(target);
            return;
        }

        requestAnimationFrame(() => {
            target.scrollIntoView({
                behavior: "instant",
                block: "center",
                inline: "nearest",
            });

            this.updatePosition();
        });
    }

    private getStepTarget(): HTMLElement | null {
        const step = this.tourService.activeStep();
        const target = step?.targets?.[0] ?? null;

        if (target == null) {
            return null;
        }

        return getActiveTourTarget(target.id, { childSelector: target.childSelector });
    }

    /**
     * Resolves and deduplicates all available targets configured for the active step.
     *
     * @returns Active target elements to highlight.
     */
    private getStepHighlightTargets(): HTMLElement[] {
        const step = this.tourService.activeStep();

        if (step == null) {
            return [];
        }

        return [
            ...new Set(
                (step.targets ?? [])
                    .map((target) => getActiveTourTarget(target.id, { childSelector: target.childSelector }))
                    .filter((target): target is HTMLElement => target !== null),
            ),
        ];
    }

    private closeAllOpenedAccordions(): void {
        const accordionGroups = document.querySelectorAll<IonicAccordionGroupElement>("ion-accordion-group");

        for (const accordionGroup of accordionGroups) {
            accordionGroup.value = [];
        }

        this.tourOpenedAccordions = [];
    }

    private captureAccordionState(): void {
        const accordionGroups = document.querySelectorAll<IonicAccordionGroupElement>("ion-accordion-group");

        for (const accordionGroup of accordionGroups) {
            const value = accordionGroup.value;
            this.originalAccordionValues.set(accordionGroup, Array.isArray(value) ? [...value] : value);
        }

        this.hasCapturedAccordionState = true;
    }

    private restoreAccordionState(): void {
        // Groups rendered after the tour started have no captured value, so close what the tour opened in them
        this.closeTourOpenedAccordions();

        for (const [accordionGroup, value] of this.originalAccordionValues) {
            if (accordionGroup.isConnected) {
                accordionGroup.value = Array.isArray(value) ? [...value] : value;
            }
        }

        this.originalAccordionValues.clear();
        this.hasCapturedAccordionState = false;
    }

    /**
     * Keeps the highlight aligned while Ionic animates an accordion.
     *
     * The target's bounds change during the transition, so its position must be recalculated before scrolling it into
     * view.
     *
     * @param target Target element whose position should be tracked.
     */
    private trackTargetDuringTransition(target: HTMLElement): void {
        const transitionDuration = 500;
        const startedAt = performance.now();
        const trackedStep = this.tourService.activeStep();

        const update = (): void => {
            // Stop only when the step changed: while an accordion is still opening, the target can be hidden for a few
            // frames and cannot be resolved yet.
            if (this.tourService.activeStep() !== trackedStep) {
                return;
            }

            this.updatePosition();

            if (performance.now() - startedAt < transitionDuration) {
                requestAnimationFrame(update);
                return;
            }

            target.scrollIntoView({
                behavior: "instant",
                block: "center",
                inline: "nearest",
            });

            requestAnimationFrame(() => {
                this.updatePosition();
            });
        };

        requestAnimationFrame(update);
    }

    /**
     * Opens ancestor accordion groups, recording only the values opened by the tour.
     *
     * @param target Element whose ancestor accordions should be opened.
     */
    private openParentAccordions(target: HTMLElement): void {
        const accordions = this.getParentAccordions(target).reverse();

        for (const accordion of accordions) {
            const accordionValue = accordion.value ?? accordion.getAttribute("value");
            const accordionGroup = accordion.closest<IonicAccordionGroupElement>("ion-accordion-group");

            if (accordionValue == null || accordionValue === "" || accordionGroup == null) {
                continue;
            }

            const openedValues = getOpenAccordionValues(accordionGroup.value);

            if (openedValues.includes(accordionValue)) {
                continue;
            }

            accordionGroup.value = [...openedValues, accordionValue];

            this.tourOpenedAccordions.push({
                group: accordionGroup,
                value: accordionValue,
            });
        }
    }

    private closeTourOpenedAccordions(): void {
        for (const openedAccordion of [...this.tourOpenedAccordions].reverse()) {
            openedAccordion.group.value = getOpenAccordionValues(openedAccordion.group.value).filter(
                (value) => value !== openedAccordion.value,
            );
        }

        this.tourOpenedAccordions = [];
    }

    /**
     * Collects accordion ancestors from the target outward.
     *
     * @param target Element whose accordion ancestors should be found.
     * @returns Unique ancestor accordion elements, nearest first.
     */
    private getParentAccordions(target: HTMLElement): IonicAccordionElement[] {
        const accordions: IonicAccordionElement[] = [];
        let currentElement: HTMLElement | null = target;

        while (currentElement != null) {
            const accordion: IonicAccordionElement | null =
                currentElement.closest<IonicAccordionElement>("ion-accordion");

            if (accordion == null || accordions.includes(accordion)) {
                break;
            }

            accordions.push(accordion);
            currentElement = accordion.parentElement;
        }

        return accordions;
    }

    private updatePosition(): void {
        const step = this.tourService.activeStep();
        const target = this.getStepTarget();

        if (step == null || target == null) {
            return;
        }

        this.highlightRects.set(
            this.getStepHighlightTargets().map((highlightTarget) => highlightTarget.getBoundingClientRect()),
        );

        requestAnimationFrame(() => {
            this.calculateTooltipPosition(step.position ?? "auto", target.getBoundingClientRect());
        });
    }

    /**
     * Calculates and clamps the tooltip position relative to the primary target.
     *
     * @param preferredPosition Requested placement, or `auto` to choose a side that keeps all targets visible.
     * @param targetRect Viewport rectangle of the primary target.
     */
    private calculateTooltipPosition(preferredPosition: TourPosition, targetRect: DOMRect): void {
        if (preferredPosition === "center") {
            this.centerTooltip();
            return;
        }

        const tooltip = this.tooltip()?.nativeElement;

        if (tooltip == null) {
            return;
        }

        this.observeTooltipSize(tooltip);

        const tooltipRect = getUntransformedSize(tooltip);

        if (preferredPosition === "auto") {
            const highlightTargetRects = this.getStepHighlightTargets().map((target) => target.getBoundingClientRect());
            this.tooltipPosition.set(
                this.findBestPosition(
                    targetRect,
                    highlightTargetRects.length > 0 ? highlightTargetRects : [targetRect],
                    tooltipRect,
                ),
            );
            return;
        }

        this.tooltipPosition.set(
            this.getPositionInsideViewport(this.getPosition(preferredPosition, targetRect, tooltipRect), tooltipRect),
        );
    }

    /**
     * Selects a placement that keeps every target of the step visible.
     *
     * Placements are tried on the right, bottom, left and top of the primary target, then of the area enclosing all
     * targets, then of each further target. The first placement that covers neither a target nor the navigation wins,
     * otherwise the one covering the least navigation and then the least target area.
     *
     * @param primaryTarget Viewport rectangle of the primary target.
     * @param targets Viewport rectangles of all targets of the step.
     * @param tooltip Viewport dimensions of the tooltip.
     * @returns Viewport coordinates for the tooltip, already clamped to the viewport.
     */
    private findBestPosition(primaryTarget: DOMRect, targets: DOMRect[], tooltip: DOMRect): TooltipPosition {
        const sides: TourPosition[] = ["right", "bottom", "left", "top"];
        const anchors = [
            primaryTarget,
            getBoundingRect(targets),
            ...targets.filter((target) => target !== primaryTarget),
        ];
        const placements = anchors.flatMap((anchor) =>
            sides.map((side) => ({
                ...this.getPositionInsideViewport(this.getPosition(side, anchor, tooltip), tooltip),
                width: tooltip.width,
                height: tooltip.height,
            })),
        );
        const placement = selectTooltipPlacement(
            placements,
            getPaddedHighlightRects(targets, this.highlightPadding),
            getActiveTourTarget(TutorialTargetId.NAVIGATION)?.getBoundingClientRect() ?? null,
        );

        if (placement != null) {
            return { top: placement.top, left: placement.left };
        }

        return this.getPositionInsideViewport(this.getPosition("bottom", primaryTarget, tooltip), tooltip);
    }

    /**
     * Calculates tooltip coordinates for a placement beside the target.
     *
     * @param position Side of the target on which to place the tooltip.
     * @param target Viewport rectangle of the primary target.
     * @param tooltip Viewport dimensions of the tooltip.
     * @returns Unclamped viewport coordinates for the tooltip.
     */
    private getPosition(position: TourPosition, target: DOMRect, tooltip: DOMRect): TooltipPosition {
        switch (position) {
            case "left":
                return {
                    top: target.top + target.height / 2 - tooltip.height / 2,
                    left: target.left - tooltip.width - this.targetGap,
                };

            case "top":
                return {
                    top: target.top - tooltip.height - this.targetGap,
                    left: target.left + target.width / 2 - tooltip.width / 2,
                };

            case "bottom":
                return {
                    top: target.bottom + this.targetGap,
                    left: target.left + target.width / 2 - tooltip.width / 2,
                };

            case "right":
            default:
                return {
                    top: target.top + target.height / 2 - tooltip.height / 2,
                    left: target.right + this.targetGap,
                };
        }
    }

    /**
     * Clamps tooltip coordinates to the viewport's responsive insets.
     *
     * @param position Desired tooltip coordinates.
     * @param tooltip Viewport dimensions of the tooltip.
     * @returns Coordinates constrained to the available viewport area.
     */
    private getPositionInsideViewport(position: TooltipPosition, tooltip: DOMRect): TooltipPosition {
        const minimumLeft = this.horizontalInset;
        const maximumLeft = Math.max(minimumLeft, window.innerWidth - tooltip.width - this.horizontalInset);
        const minimumTop = this.verticalInset;
        const maximumTop = Math.max(minimumTop, window.innerHeight - tooltip.height - this.verticalInset);

        return {
            left: Math.min(Math.max(position.left, minimumLeft), maximumLeft),
            top: Math.min(Math.max(position.top, minimumTop), maximumTop),
        };
    }
}
