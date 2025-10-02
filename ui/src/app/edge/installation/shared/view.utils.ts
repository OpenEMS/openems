import { Edge } from "src/app/shared/shared";
import { Country } from "src/app/shared/type/country";
import { View } from "./enums";

export namespace ViewUtils {
    /**
     * Removes a view from the list if it exists.
     *
     * @typeParam T - The type of the view (typically `View`)
     * @param views - The list of views to remove from.
     * @param view - The specific view to remove.
     * @param action - callback to execute after removal.
     */
    export function remove<T>(views: T[], view: T, action: () => void = () => { }): void {
        const index = views.indexOf(view);
        if (index !== -1) {
            views.splice(index, 1);
            action();
        }
    }

    /**
     * Adds a view to the list if it does not already exist, at the specified index.
     *
     * @typeParam T - The type of the view (typically `View`)
     * @param views - The list of views to add to.
     * @param view - The specific view to add.
     * @param insertIndex - The index at which to insert the view.
     * @param action - callback to execute after insertion.
     */
    export function add<T>(views: T[], view: T, insertIndex: number, action: () => void = () => { }): void {
        if (!views.includes(view)) {
            views.splice(insertIndex, 0, view);
            action();
        }
    }

    /**
     * Checks if a specific view exists in the list.
     *
     * @typeParam T - The type of the view (typically `View`)
     * @param views - The list of views to search.
     * @param view - The view to look for.
     * @returns `true` if the view exists, otherwise `false`.
     */
    export function exists<T>(views: T[], view: T): boolean {
        return views.includes(view);
    }

    /**
     * Conditionally adds or removes a view based on the current country.
     *
     * @remarks
     * - If `location` is not equal to `targetCountry`, removes the view and optionally runs `shouldClear`.
     * - If `location` equals `targetCountry` and the view is missing, adds the view and optionally runs `shouldSet`.
     *
     * @typeParam T - The type of the view (typically `View`)
     * @param params - The parameters for conditional view handling.
     * @param params.location - The current country/location.
     * @param params.targetCountry - The country for which the view should be visible.
     * @param params.views - The list of views to modify.
     * @param params.viewToToggle - The view to conditionally add or remove.
     * @param params.insertIndex - Index at which to add the view if needed.
     * @param params.shouldClear - Optional callback to invoke when the view is removed.
     * @param params.shouldSet - Optional callback to invoke when the view is added.
     */
    export function handleViewPresence<T>({
        location,
        targetCountry,
        views,
        viewToToggle,
        insertIndex,
        shouldClear,
        shouldSet,
    }: {
        location: Country | undefined;
        targetCountry: Country;
        views: T[];
        viewToToggle: T;
        insertIndex: number;
        shouldClear?: () => void;
        shouldSet?: () => void;
    }): void {
        if (location && location !== targetCountry) {
            remove(views, viewToToggle, shouldClear);
            return;
        }

        add(views, viewToToggle, insertIndex, shouldSet);
    }

    /**
     * Conditionally removes the `PreInstallationUpdate` view based on the minimum edge version required.
     *
     * @remarks
     * Removes the view if `edge` is missing or its version is below `"2021.19.1"`.
     *
     * @param views - The list of views to modify.
     * @param edge - The current `Edge` instance with version information. can be null
     */
    export function handlePreinstallationUpdate(views: View[], edge: Edge | null): void {
        // TODO remove when every edge starts with at least the required version
        // only show update view if the update requests are implemented
        if (edge == null) {
            return;
        }

        // Common version-based view removal
        if (!edge.isVersionAtLeast("2021.19.1")) {
            remove(views, View.PreInstallationUpdate);
        }
    }
}
