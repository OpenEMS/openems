import { Environment } from "src/environments";

export const environment: Environment = {
  title: "OpenEMS UI",
  shortName: "OpenEMS",

  backend: 'OpenEMS Backend',
  // gitpod puts the port number in front of the hostname
  url: "wss://8082-" + location.hostname.substring(location.hostname.indexOf("-") + 1),

  production: false,
  debugMode: true,
};