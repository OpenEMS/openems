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
  case 'FENECON':
    config = {
      ...baseConfig,
      appId: 'de.fenecon.fems',
      appName: 'FENECON',
      server: {
        ...baseConfig.server,
        hostname: 'portal.fenecon.de'
      }
    }
    break;
  case 'Heckert':
    config = {
      ...baseConfig,
      appId: 'com.heckertsolar.ems',
      appName: 'Heckert',
      server: {
        ...baseConfig.server,
        hostname: 'symphon-e.heckert-solar.com'
      }
    }
    break;
  default:
    throw new Error(`Capacitor config for theme ${process.env.NODE_ENV} not implemented.`)
}
console.warn(config);

export default config;
