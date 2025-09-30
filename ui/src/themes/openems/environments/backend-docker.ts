// @ts-strict-ignore
import { Environment } from "src/environments";
import { theme } from "./theme";

// In docker test environment variable WINDOW.ENV is injected.
// cf.
//  - tools/docker/ui/root/etc/s6-overlay/s6-RC.D/init-nginx/run
//  - tools/docker/ui/assets/ENV.TEMPLATE.JS
const window_env = (window as any).env as { [key: string]: string };

export const environment: Environment = {
    ...theme, ...{
        backend: "OpenEMS Backend",
        url: window_env.websocket,

        production: true,
        debugMode: false,
    },
};
