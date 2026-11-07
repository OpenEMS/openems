import { defineUniqueValues } from "../type/utility";

/**
 * All values of the `data-tutorial-id` attribute that tour steps can highlight.
 *
 * Navigation entries use their navigation tree id as `data-tutorial-id` value, unless a dedicated `tourId` is set.
 */
export const TutorialTargetId = defineUniqueValues({
    // Navigation entries
    LIVE: "live",
    ENERGY_JOURNEY: "energy-journey",
    HISTORY: "history",
    HISTORY_EXPORT: "history-export",
    APP_CENTER: "appcenter",
    FAVORITES: "favorites",
    PROFILE: "profile",
    NAVIGATION_INFO: "navigation-info",

    // Layout
    NAVIGATION: "navigation",

    // Page elements
    ENERGY_MONITOR: "energy-monitor",
    SYSTEM_STATUS: "system-status",
    HISTORY_VIEW: "history-view",
    HISTORY_VIEW_DATE_PICKER: "history-view-date-picker",
    ENERGY_JOURNEY_APP_CENTER: "energy-journey-appcenter",
    PROFILE_ALERTING: "profile-alerting",
    PROFILE_INFO: "profile-info",
});

export type TutorialTargetId = (typeof TutorialTargetId)[keyof typeof TutorialTargetId];
