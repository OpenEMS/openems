import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'io.openems.app',
  appName: 'OpenEMS',
  webDir: 'target',
  server: {
    androidScheme: 'https'
  }
};

export default config;
