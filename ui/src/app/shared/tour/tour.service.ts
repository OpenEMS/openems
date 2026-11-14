import { Injector, Service, computed, inject, runInInjectionContext, signal } from "@angular/core";
import { Router, isActive as isRouteActive } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { compareVersions } from "compare-versions";
import { DeviceDetectorService } from "ngx-device-detector";
import { environment } from "src/environments";
import { version as uiVersion } from "../../../../package.json";
import { NavigationService } from "../components/navigation/service/navigation.service";
import { TutorialState, UserSettings } from "../jsonrpc/shared";
import { Service as TService } from "../service/service";
import { UserService } from "../service/user.service";
import { Language } from "../type/language";
import { DocsUtils } from "../utils/docs/docs.utils";
import { NAVIGATION_TOUR, NavigationTour } from "./navigation-tour";
import { TourStep } from "./tour-step";
import { getActiveTourTarget, getTourVersion } from "./tour.utils";

/** Manages tour progress, target resolution, and completion state. */
@Service()
export class TourService {
    public readonly activeStep = signal<TourStep | null>(null);
    public readonly isActive = signal(false);
    public readonly stepNumber = computed(() => this.currentIndex() + 1);
    public readonly stepCount = computed(() => this.steps().length);
    public readonly isFirstStep = computed(() => this.currentIndex() === 0);
    public readonly isLastStep = computed(() => {
        return this.currentIndex() === this.steps().length - 1;
    });

    private readonly steps = signal<TourStep[]>([]);
    private readonly currentIndex = signal(0);
    private readonly router = inject(Router);
    private readonly service = inject(TService);
    private readonly userService = inject(UserService);
    private readonly deviceService = inject(DeviceDetectorService);
    private readonly translate = inject(TranslateService);
    private readonly navigationService = inject(NavigationService);
    private readonly navigationTour = inject(NAVIGATION_TOUR);
    private readonly injector = inject(Injector);
    /** Tour implementation of the theme, loaded on the first start. */
    private loadedNavigationTour: Promise<NavigationTour> | null = null;
    /** Id of the latest step request; older pending requests are discarded. */
    private stepRequestId = 0;
    /** Set once the tour ended, so it does not start automatically again in this session, even if storing failed. */
    private hasEndedForSession = false;

    /**
     * Checks whether the navigation tour is available for the current device layout.
     *
     * Requires a tour provided by the theme and a backend connection: a local edge cannot store the tutorial state, so
     * the tour would start again in every session. The tour is released step by step: desktop first, smartphones and
     * tablets follow later. It also requires the new side menu navigation, as the tour steps point to its entries.
     *
     * @returns Whether {@link startNavigationTour} would start a tour.
     */
    public canStartNavigationTour(): boolean {
        return (
            this.navigationTour != null &&
            environment.backend !== "OpenEMS Edge" &&
            this.deviceService.isDesktop() &&
            this.navigationService.position() === "left"
        );
    }

    /** Starts the theme's navigation tour matching the current device layout. */
    public startNavigationTour(): void {
        this.startNavigationTourWhen(() => true);
    }

    /**
     * Starts the navigation tour automatically, unless the user already completed or dismissed this tour version or the
     * tour already ended in this session.
     */
    public startNavigationTourIfNeeded(): void {
        if (this.hasEndedForSession) {
            return;
        }

        const user = this.userService.currentUser();
        if (user == null) {
            return;
        }

        const tutorialState = user.settings[UserSettings.TUTORIAL_STATE];
        this.startNavigationTourWhen((steps) => !this.hasSeenTourVersion(tutorialState, steps));
    }

    public start(steps: TourStep[]): void {
        if (steps.length === 0 || this.isActive()) {
            return;
        }

        this.steps.set(steps);
        this.currentIndex.set(0);
        this.isActive.set(true);

        this.showStep();
    }

    public next(): void {
        if (!this.isActive()) {
            return;
        }

        if (this.currentIndex() >= this.steps().length - 1) {
            this.complete();
            return;
        }

        this.currentIndex.update((index) => index + 1);

        this.showStep();
    }

    public previous(): void {
        if (!this.isActive() || this.currentIndex() <= 0) {
            return;
        }

        this.currentIndex.update((index) => index - 1);
        this.showStep();
    }

    /** Dismisses the tour and stores that it should not start automatically again. */
    public cancel(): void {
        this.persistTutorialState("skipped");
        this.reset();
    }

    /** Closes the tour without storing a state, so it is offered again in a later session. */
    public skip(): void {
        this.reset();
    }

    /**
     * Checks whether the user finished or dismissed the tour with a UI version that already contained all given steps.
     *
     * @param tutorialState State stored in the user settings.
     * @param steps Steps of the current tour.
     * @returns Whether no step was added or changed since the stored UI version.
     */
    private hasSeenTourVersion(tutorialState: TutorialState | undefined, steps: TourStep[]): boolean {
        const tourVersion = getTourVersion(steps);
        if (tutorialState == null || tourVersion == null) {
            return false;
        }

        // Ignore pre-release tags like -SNAPSHOT, which would otherwise rank below the release of the same version
        const [storedVersion] = tutorialState.uiVersion.split("-");
        return compareVersions(storedVersion, tourVersion) >= 0;
    }

    /** Finishes the tour and stores that it was completed. */
    private complete(): void {
        this.persistTutorialState("completed");
        this.reset();
    }

    /** Stops the tour after a technical problem without storing a state, so a failure does not dismiss it for good. */
    private abort(): void {
        this.reset();
    }

    /**
     * Loads the theme's tour if needed and starts it with the steps of the current layout.
     *
     * @param shouldStart Decides based on the steps of the current layout whether the tour starts.
     */
    private startNavigationTourWhen(shouldStart: (steps: TourStep[]) => boolean): void {
        // Mobile is released later and will use tour.getMobileSteps()
        if (!this.canStartNavigationTour()) {
            return;
        }

        this.loadNavigationTour()
            .then((tour) => {
                const steps = tour.getDesktopSteps();
                if (shouldStart(steps)) {
                    this.start(this.resolveHandbookLanguage(steps));
                }
            })
            .catch((error: unknown) => {
                // Allow a later attempt, e.g. after a failed chunk download
                this.loadedNavigationTour = null;
                console.error("Failed to load the navigation tour.", error);
            });
    }

    private loadNavigationTour(): Promise<NavigationTour> {
        const config = this.navigationTour;
        if (config == null) {
            return Promise.reject(new Error("The theme provides no navigation tour."));
        }

        this.loadedNavigationTour ??= config
            .load()
            .then((tourClass) => runInInjectionContext(this.injector, () => new tourClass()));
        return this.loadedNavigationTour;
    }

    /**
     * Replaces the {language} placeholder of the handbook links with the current docs language.
     *
     * Handbooks are only available in German and English; every other language falls back to English.
     *
     * @param steps Steps as provided by the theme.
     * @returns The steps with resolved handbook links.
     */
    private resolveHandbookLanguage(steps: TourStep[]): TourStep[] {
        const docsLanguage = Language.getDocsLang(this.translate.getCurrentLang());

        return steps.map((step) => ({
            ...step,
            handbookUrl: DocsUtils.replaceDocsLanguage(step.handbookUrl, docsLanguage),
        }));
    }

    private persistTutorialState(state: TutorialState["state"]): void {
        if (this.steps().length === 0) {
            return;
        }

        const tutorialState: TutorialState = { state, uiVersion };
        this.userService
            .updateUserSettingsWithProperty(UserSettings.TUTORIAL_STATE, tutorialState)
            .catch((error: unknown) => console.error("Failed to store tutorial state.", error));
    }

    /**
     * Starts showing the current step and stops the tour if preparing it fails.
     *
     * Each call gets a new request id, so pending older requests are discarded.
     */
    private showStep(): void {
        const requestId = ++this.stepRequestId;
        this.showCurrentStep(requestId).catch((error: unknown) => {
            if (!this.isLatestStepRequest(requestId)) {
                return;
            }

            console.error("Failed to show tour step.", error);
            this.abort();
        });
    }

    /**
     * Prepares and activates the current step.
     *
     * Navigates to the step route and runs its preparation callback before waiting for a target. Missing optional steps
     * are skipped; a missing required target aborts the tour.
     *
     * @param requestId Id of the step request; the step is discarded once a newer request starts.
     */
    private async showCurrentStep(requestId: number): Promise<void> {
        const step = this.steps()[this.currentIndex()];

        if (step == null) {
            this.complete();
            return;
        }

        const isAtStepRoute = await this.navigateToStepRoute(step);
        if (!this.isLatestStepRequest(requestId)) {
            return;
        }
        if (!isAtStepRoute) {
            this.abort();
            return;
        }

        await step.beforeShow?.();
        if (!this.isLatestStepRequest(requestId)) {
            return;
        }

        if (step.position === "center" && (step.targets?.length ?? 0) === 0) {
            this.activeStep.set(step);
            return;
        }

        const target = await this.waitForTarget(step, requestId);
        if (!this.isLatestStepRequest(requestId)) {
            return;
        }

        if (target == null) {
            if (step.optional === true) {
                this.skipMissingStep();
                return;
            }

            console.warn(`Tour target "${step.targets?.[0]?.id}" not found.`);
            this.abort();
            return;
        }

        this.activeStep.set(step);
    }

    /**
     * Navigates to the route of a step unless it is already active.
     *
     * @param step Step whose route should be shown.
     * @returns Whether the step route is shown; steps without route or without selected edge stay on the current page.
     */
    private async navigateToStepRoute(step: TourStep): Promise<boolean> {
        const currentEdge = this.service.currentEdge();
        if (step.route == null || currentEdge == null) {
            return true;
        }

        const targetUrl = "/device/" + currentEdge.id + step.route;
        const isAlreadyAtTarget = isRouteActive(targetUrl, this.router, {
            paths: "exact",
            queryParams: "ignored",
            fragment: "ignored",
            matrixParams: "ignored",
        })();
        if (isAlreadyAtTarget) {
            return true;
        }

        const hasNavigated = await this.router.navigateByUrl(targetUrl);
        if (!hasNavigated) {
            console.warn(`Tour route "${targetUrl}" could not be reached.`);
        }
        return hasNavigated;
    }

    /**
     * Waits for the targets of a step to appear in the active DOM.
     *
     * Pages render their content asynchronously, so all targets are awaited before the step is shown. If only
     * additional targets are still missing when the attempts run out, the step is shown with its primary target.
     *
     * @param step Step whose targets should be resolved.
     * @param requestId Id of the step request; the lookup stops once a newer request starts.
     * @param attempts Maximum number of target lookups before giving up.
     * @returns The primary target, or `null` when it does not appear in time.
     */
    private async waitForTarget(step: TourStep, requestId: number, attempts = 100): Promise<HTMLElement | null> {
        let targets = this.getTargets(step);

        for (let i = 1; i < attempts && targets.includes(null); i++) {
            await new Promise<void>((resolve) => {
                setTimeout(resolve, 20);
            });
            if (!this.isLatestStepRequest(requestId)) {
                return null;
            }

            targets = this.getTargets(step);
        }

        return targets[0] ?? null;
    }

    private skipMissingStep(): void {
        if (this.currentIndex() >= this.steps().length - 1) {
            this.complete();
            return;
        }

        this.currentIndex.update((index) => index + 1);
        this.showStep();
    }

    private isLatestStepRequest(requestId: number): boolean {
        return requestId === this.stepRequestId;
    }

    /**
     * Resolves all targets configured for a step.
     *
     * @param step Step whose targets should be resolved.
     * @returns The active target element per configured target, or `null` for targets that cannot be resolved yet.
     */
    private getTargets(step: TourStep): (HTMLElement | null)[] {
        return (step.targets ?? []).map((target) =>
            getActiveTourTarget(target.id, {
                childSelector: target.childSelector,
                allowClosedAccordion: step.openParentAccordion === true,
            }),
        );
    }

    private reset(): void {
        this.hasEndedForSession = true;
        this.stepRequestId++;
        this.activeStep.set(null);
        this.steps.set([]);
        this.currentIndex.set(0);
        this.isActive.set(false);
    }
}
