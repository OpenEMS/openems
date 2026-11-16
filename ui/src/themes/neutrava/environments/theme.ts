import { Environment, Theme } from "src/environments";
import { OemMeta } from "./oem-meta";

export const theme: Omit<
    Environment,
    "url" | "backend" | "production" | "debugMode"
> = {
    theme: "OpenEMS" as Theme,

    uiTitle: "neutrava EMS",
    uiTitleShort: "neutrava",
    edgeShortName: "neutrava EMS",
    edgeLongName: "neutrava Energiemanagement-System",

    docsUrlPrefix: "https://www.neutrava.ch/",
    PRODUCT_TYPES: () => null,
    ENVIRONMENT_PROVIDERS: [],
    ...OemMeta,
};
