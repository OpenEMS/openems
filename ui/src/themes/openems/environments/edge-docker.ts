// @ts-strict-ignore
import { Environment } from "src/environments";
import { theme } from "./theme";

// In docker test environment, variable window.env is injected.
// cf.
//  - tools/docker/ui/root/etc/s6-overlay/s6-rc.d/init-nginx/run
//  - tools/docker/ui/assets/env.template.js
const window_env = (window as any).env as { [key: string]: string };

export const environment: Environment = {
    ...theme, ...{

        backend: 'OpenEMS Edge',
        url: window_env.websocket,

        production: true,
        debugMode: false,
    },
};
