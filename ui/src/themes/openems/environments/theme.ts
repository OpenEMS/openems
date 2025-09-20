import { Theme } from "src/environments";
import { OemMeta } from "./oem-meta";

export const theme = {
    theme: "OpenEMS" as Theme,

    uiTitle: "OpenEMS UI",
    edgeShortName: "OpenEMS",
    edgeLongName: "Open Energy Management System",
    defaultLanguage: "de",

    docsUrlPrefix: "https://github.com/OpenEMS/openems/blob/develop/",
    PRODUCT_TYPES: () => null,
    ...OemMeta,
};
