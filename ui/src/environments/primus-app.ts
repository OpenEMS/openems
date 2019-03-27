import { Environment } from "../app/shared/type/environment";

export const environment: Environment = {
  production: false,
  debugMode: true,
  url: "wss://www.energydepot.de/primus-ui-dev",
  //url: "wss://ahu:KNKonstanz2018!@www.energydepot.de/primus-ui-dev",
  backend: "App",
};
