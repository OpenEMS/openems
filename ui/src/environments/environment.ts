import { Environment } from "../app/shared/type/environment";

export const environment: Environment = {
  production: false,
  debugMode: true,

  // For OpenEMS Edge
  // url: "ws://" + location.hostname + ":8085",
  // backend: "OpenEMS Edge"

  // For OpenEMS Backend
  url: "ws://" + location.hostname + ":8078",
  // url: "ws://fems-test.beegy-dev.cc:10003",
  backend: "OpenEMS Backend",
};
