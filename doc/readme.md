# Communication protocols

This chapter explains the communication protocol used between the different components.

## [1] OpenEMS UI <-> OpenEMS Edge/OpenEMS Backend

### [1.1] Authenticate

[1.1.1] Authenticate Client -> OpenEMS Backend

[1.1.1.1] Automatic

Using cookie information (session_id + token) in handshake

[1.1.1.1] Manual login

currently forwarded to Odoo login page

[1.1.2] Authenticate Client -> OpenEMS Edge

[1.1.2.1] Automatic

// TODO

[1.1.2.2] Manual login
```
{
	authenticate: {
		mode: "login",
		username?: string,
		password: string
	}
}
```

[1.1.2.2] Manual logout
```
{
	authenticate: {
		mode: "logout"
	}
}
```

[1.1.3] Authentication reply

[1.1.3.1] Authentication successful

```
{
	authenticate: {
		mode: "allow",
		token: string,
		role?: "admin" | "installer" | "owner" | "guest"
	}, metadata: {
		user: {
			id: Integer
		},
		edges?: [{
			id: number,
			name: string,
			comment: string,
			producttype: "Pro 9-12" | "MiniES 3-3" | "PRO Hybrid 9-10" | "PRO Compact 3-10" | "COMMERCIAL 40-45" | "INDUSTRIAL" | "",
			role: "admin" | "installer" | "owner" | "guest",
			online: boolean
		}]
	}
}
```

- authenticate.role is only sent for OpenEMS Edge
- metadata.devices is only sent for OpenEMS Backend

[1.1.3.2] Authentication failed
{
	authenticate: {
		mode: "deny"
	}
}

## [2] OpenEMS UI <-> OpenEMS Backend <-> OpenEMS Edge

Following commands are all the same, no matter if UI is connected to Edge or to Backend. 

Backend is transparently proxying requests to a connected Edge if necessary, adding the authentication role and the UI token as identifier to the message id.

```
{
	messageId: [string] | string,
	role: "admin" | "installer" | "owner" | "guest",
	...
}
```

### [2.1] Receive current configuration

[2.1.1] UI -> Edge/Backend

```
{
	messageId: UUID,
	edgeId: number,
	config: {
		mode: "query",
		language: 'de' | 'en' | ...
	}
}
```

[2.1.2] Edge/Backend -> UI

```
{
	messageId: UUID,
	config: {
		things: {
			[id: string]: {
				id: string,
				class: string | string[],
				[channel: string]: any
			}
		},
		meta: {
			[clazz: string]: {
				implements: [string],
				channels: {
					[channel: string]: {
						name: string,
						title: string,
						type: string | string[],
						optional: boolean,
						array: boolean,
						readRoles: string[],
						writeRoles: string[],
						defaultValue: string
					}
				}
			}
		}
	}
}
```

### [2.2] Current live data

[2.2.1] UI -> Edge/Backend

For 'unsubscribe' the channels object is empty.

```
{
	messageId: UUID,
	edgeId: number,
	currentData: {
		mode: "subscribe",
		channels: {
			[thingId: string]: string[]
		}
	}
}
```

[2.2.2] Edge/Backend -> UI

```
{
	messageId: UUID,
	currentData: {[{ 
		channel: string,
		value: any
    }]}
}
```

### [2.3] Query historic data

[2.3.1] UI -> Edge/Backend
```
{
	id: [string],
	device: string,
	historicData: {
		mode: "query",
		fromDate: date,
		toDate: date, 
		timezone: number /* offset in seconds */,
		channels: {
			thing: [
				channel: string
			] 
		}
		// kwhChannels: {
		//	address: 'grid' | 'production' | 'storage',
		// }
	}
}
```

[2.3.2] Edge/Backend -> UI
```
{
	id: [UUID],
    historicData: {
		data: [{
			time: string,
			channels: {
				thing: {
					'channel': 'value'
				} 
			}
		}]
		// kWh: {
		//	'meter0/ActivePower': {
		//		'sell': ...,
		//		'buy': ...,
		//		'type': 'grid' | 'production' | 'storage'
		//	},
		//	'meter1/ActivePower': {
		//		'value': value,
		//		'type': 'grid' | 'production' | 'storage'
		//	},
		//	'ess0/ActivePower': {
		//		'charge: ...,
		//		'discharge': ...,
		//		'type': 'grid' | 'production' | 'storage'
		//	}
		//}
	}
}
```

### [2.4] Notification

```
{
 	notification: {
		id: string[],
		type: "success" | "error" | "warning" | "info",
		message: "...",
		code?: number,
		params?: string[]
	}
}
```

### [2.5] Current system log

[2.2.1] UI -> Edge/Backend

```
{
	id: [string],
	device: string,
	log: {
		mode: "subscribe"
	}
}
```

```
{
	id: [string],
	device: string,
	log: {
		mode: "unsubscribe"
	}
}
```

[2.2.2] Edge/Backend -> UI

```
{
	log: {
		timestamp: number,
		level: string,
		source: string,
		message: string
    }
}
```

### [2.6] Set configuration

### [2.6.1] Update existing things/channels

```
{
	device: string,
	id: [UUID],
	config: {
		mode: "update",
		thing: "...",
		channel: "...",
		value: "..." | { ... }
	}
}
```

### [2.7] Remote system control

[2.7.1] UI -> Edge/Backend

{
	device: string,
	id: [UUID],
	system: {
		mode: "execute",
		password: "...",
		command: "...",
		background: boolean,
		timeout: number
	}
}

[2.7.2] Edge/Backend -> UI

```
{
	device: string,
	id: [UUID],
	system: {
		mode: "executeReply",
		output: "..."
	}
}
```
## [3] OpenEMS Edge <-> OpenEMS Backend

### [3.1] Timestamped data
```
{
	timedata: {
		timestamp (Long): {
			channel: String,
			value: String | Number
		}
	}
}
```






// TODO from here



### [1.5.2] Create
```
{
	configure: [{
		mode: "create",
		object: { ... },
		parent: "..."
	}]
}
```

### [1.5.3] Delete
```
{
	configure: [{
		mode: "delete",
		thing: "..."
	}]
}
```

### [1.5.4] Reply
```
{
 	metadata: {
		config: {}
	}, notification: {
		type: "success" | "error" | "warning" | "info",
		message: "..."
	}
}
```

### [1.6] System
```
{
 	system: {
 		mode: "systemd-restart",
 		service: "fems-pagekite" | "..."
 	}
 	system: {
 		mode: "manualpq-start",
 		ess: "...",
 		active: true | false
 		p: ...,
 		q: ...
 	}
}
```

# Generate the docs

## Charts

Charts are defined in mmd-files and use the "mermaid" tool: 

http://knsv.github.io/mermaid
http://knsv.github.io/mermaid/live_editor