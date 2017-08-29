export module DefaultTypes {
  export interface Config {
    persistence: any[],
    scheduler: any,
    things: any[]
  }

  export interface MessageMetadataDevice {
    name: string,
    comment: string,
    producttype: string,
    role: string,
    online: boolean
  }
}

/*
  private influxdb: {
    ip: string,
    username: string,
    password: string,
    fems: string
  }
*/