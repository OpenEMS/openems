export module DefaultTypes {
  export interface Thing {
    id: string,
    class: string
  }

  export interface Config {
    bridge: {
      [id: string]: Thing
    }
  }

  export interface ChannelAddresses {
    [thing: string]: string[];
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