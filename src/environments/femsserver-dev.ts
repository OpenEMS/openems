import { Environment } from "./environment.type";

export const environment: Environment = {
  production: false,
  backend: "femsserver",
  url: "ws://" + location.hostname + ":8078"
};