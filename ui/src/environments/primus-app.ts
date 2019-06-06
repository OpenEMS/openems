import { Environment } from "../app/shared/type/environment";

export const environment: Environment = {
  production: true,
  debugMode: false,
  url: "wss://www.energydepot.de/primus-ui-dev",
  //url: "wss://ahu:KNKonstanz2018!@www.energydepot.de/primus-ui-dev",
  backend: "App",
};
