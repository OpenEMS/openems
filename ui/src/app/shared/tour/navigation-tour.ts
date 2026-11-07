import { EnvironmentProviders, InjectionToken, Type, makeEnvironmentProviders } from "@angular/core";
import { TourStep } from "./tour-step";

export interface NavigationTour {
    /**
     * Builds the steps for the desktop layout when the tour starts.
     *
     * @returns The ordered steps; handbook links may contain a `{language}` placeholder.
     */
    getDesktopSteps(): TourStep[];

    /**
     * Builds the steps for the mobile layout when the tour starts.
     *
     * @returns The ordered steps; handbook links may contain a `{language}` placeholder.
     */
    getMobileSteps(): TourStep[];
}

export interface NavigationTourConfig {
    /**
     * Loads the tour implementation when the tour starts.
     *
     * Theme files are evaluated while the environment is being set up, so they must not import the tour and its
     * dependencies statically; use a dynamic `import()` instead.
     */
    readonly load: () => Promise<Type<NavigationTour>>;

    /** Icon presenting the step texts as its speech bubble, or `null` to show the texts plainly. */
    readonly avatar: { readonly name: string; readonly src: string } | null;
}

/** Navigation tour of the current theme, or `null` if the theme offers none. */
export const NAVIGATION_TOUR = new InjectionToken<NavigationTourConfig | null>("NAVIGATION_TOUR", {
    providedIn: "root",
    factory: () => null,
});

/**
 * Registers the navigation tour of a theme; add the result to the theme's `ENVIRONMENT_PROVIDERS`.
 *
 * @param config Loader and avatar of the theme's tour.
 * @returns The providers registering the tour.
 */
export function provideNavigationTour(config: NavigationTourConfig): EnvironmentProviders {
    return makeEnvironmentProviders([{ provide: NAVIGATION_TOUR, useValue: config }]);
}
