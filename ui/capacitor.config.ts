import { CapacitorConfig } from '@capacitor/cli';
import { Theme } from 'src/environments';

let config: CapacitorConfig;

const baseConfig: CapacitorConfig = {
  webDir: 'target',
  server: {
    androidScheme: 'https',
    iosScheme: 'https',
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1000,
      launchAutoHide: false,
      launchFadeOutDuration: 1000,
      backgroundColor: "#ffffffff",
      androidSplashResourceName: "splash",
      androidScaleType: "CENTER_INSIDE",
      splashFullScreen: false,
      splashImmersive: true,
      useDialog: true,
    },
    CapacitorCookies: {
      enabled: true
    }
  }
}

switch (process.env.NODE_ENV as Theme) {
  // case 'EXAMPLE':
  //   config = {
  //     ...baseConfig,
  //     appId: 'io.openems.ui',
  //     appName: 'EXAMPL',
  //     server: {
  //       ...baseConfig.server,
  //       hostname: 'portal.openems.io'
  //     }
  //   }
  //   break;
  default:
    throw new Error(`Capacitor config for theme ${process.env.NODE_ENV} not implemented.`)
}
console.warn(config);

export default config;
