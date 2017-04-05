# Communication protocols

This chapter explains the communication protocol used between the different components.

## [1] Client (Browser) <-> Backend (OpenEMS/FemsServer)

### [1.1] Authenticate

#### [1.1.1] At FemsServer

[1.1.1.1] Authenticate
Cookie: session_id

[1.1.1.2] On success
```
{ result: { devices: ["...",] } }
```

[1.1.1.3] On error
```
{ error }
```

[1.1.1.4] Reply
```
{
	authenticate: {
		mode: allow, username, token
	}, metadata: {
		devices: [{
			name, config, online
		}],
		backend: "femsserver"
	}
}
```

#### [1.1.2] At OpenEMS

[1.1.2.1] Authenticate
```
{
	authenticate: {
		mode: login,
		[password: "...",] 
		[token: "..."]
	}
}
```

[1.1.2.2] Reply
```
{
	authenticate: {
		mode: allow, username, token
	}, metadata: {
		config: {},
		backend: "openems"
	}
}
```

### [1.2] Current data

[1.2.1] Subscribe
```
{
	device: "...",
	subscribe: {
		channels: {
			thing0: [
				channel
			]
		},
		log: "all" | "info" | "warning" | "error"
	}
}
```

[1.2.2] Forward to OpenEMS
```
{
	subscribe: ...
}
```

[1.2.3] Reply from OpenEMS
```
{
	currentdata: [{ 
		channel, value
    }]
}
```

[1.2.4] Reply
```
{
	device: "...",
    currentdata: [{ 
    	channel, value
    }]
}
```

[1.2.5] Unsubscribe
```
{
	device: "...",
	subscribe: {
		channels: {},
		log: ""
	}
}
```

### [1.3] Notification
```
{
	device: "...",
	notification: {
		message: "...
	}
}
```

### [1.3] Log
```
{
	device: "...",
	log: {
		timestamp: ...,
		level: ...,
		source: ...,
		message: ...
	}
}
```

### [1.4] Query history data

[1.4.1]
```
{
	device: "...",
	query: {
		mode: "history",
		fromDate: "01.01.2017",
		toDate: "01.01.2017", 
		timezone: /* offset in seconds */,
		data: {
			channels: {
				thing: [channel] 
			}
		},
		kWh: {
			"thing/channel": 'grid' | 'production' | 'storage',
		}
	}
}
```

[1.4.2]
```
{
    queryreply: {
    	mode: "history",
		fromDate: "2017-01-01",
		toDate: "2017-01-01", 
		timezone: /* offset in seconds */,
		data: [{
			time: ...,
			channels: {
				'thing': {
					'channel': 'value'
				} 
			}
		}],
		kWh: {
			'meter0/ActivePower': {
				'sell': ...,
				'buy': ...,
				'type': 'grid' | 'production' | 'storage'
			},
			'meter1/ActivePower': {
				'value': value,
				'type': 'grid' | 'production' | 'storage'
			},
			'ess0/ActivePower': {
				'charge: ...,
				'discharge': ...,
				'type': 'grid' | 'production' | 'storage'
			}
		}
	}
}
```


### [1.5] Configuration

### [1.5.1] Update
```
{
	configure: [{
		mode: "update",
		thing: "...",
		channel: "...",
		value: "..." | { ... }
	}]
}
```
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
}
```

## [2] OpenEMS <-> FemsServer

### [2.1] Authenticate
```
{
	metadata: {
		config: {},
		backend: "openems"
	}
}
```

### [2.2] timestamped data
```
{
	timedata: {
		timestamp: [{
			channel, value
		}]
	}
}
```

# Generate the docs

## Charts

Charts are defined in mmd-files and use the "mermaid" tool: 

http://knsv.github.io/mermaid
http://knsv.github.io/mermaid/live_editor