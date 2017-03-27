import { Environment } from "./environment.type";

export const environment: Environment = {
  production: false,
  /*
   * OpenEMS
   */
  backend: "openems",
  url: "ws://" + location.hostname + ":8085",

  /*
   * FEMS-Server
   */
  // backend: "femsserver",
  // url: "ws://" + location.hostname + ":8087",
};
