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

//TODO

[1.1.3] Authentication reply

[1.1.3.1] Authentication successful

```
{
	authenticate: {
		mode: "allow",
		token: String,
		role: "admin" | "installer" | "owner" | "guest"
	}, metadata: {
		user: {
			id: Integer
		},
		devices: [{
			name: String,
			comment: String,
			producttype: "Pro 9-12" | "MiniES 3-3" | "PRO Hybrid 9-10" | "PRO Compact 3-10" | "COMMERCIAL 40-45" | "INDUSTRIAL",
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

## [1.2] OpenEMS UI <-> OpenEMS Backend <-> OpenEMS Edge

Following commands are all the same, no matter if UI is connected to Edge or to Backend. Backend is transparently proxying requests to a connected Edge if necessary.

### [1.2.1] Receive current configuration

[1.2.1.1] UI -> Edge/Backend

```
{
	device: String,
	config: {
		mode: "refresh"
	}
}
```

### [1.2.1] Current live data

[1.2.1.1] Subscribe to current data: UI -> Edge/Backend

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



## [2] OpenEMS Edge <-> OpenEMS Backend

### [2.1] Authenticate

[2.1.1] Authenticate OpenEMS Edge -> OpenEMS Backend

Using apikey in handshake

[2.1.2] Authentication reply

[2.1.2.1] Authentication successful

```
{
	authenticate: {
		mode: "allow"
	}
}
```

[2.1.2.2] Authentication failed
{
	authenticate: {
		mode: "deny",
		message: String
	}
}

### [2.2] Timestamped data: OpenEMS Edge -> OpenEMS Backend
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



// TODO rework from here...













[1.1.1] Authenticate Client -> Backend

```
{
	authenticate: {
		mode: login,
		[password: "...",]
		[token: "..."]
	}
}
```






[1.1.1] Client -> OpenEMS Backend

Cookie: session_id

[1.1.1.1] OpenEMS Backend -> Odoo

[1.1.1.1.1] Authenticate
Cookie: session_id

[1.1.1.1.2] Odoo reply on success
```
{
	result: {
		user: Integer,
		devices: [{
			name: String,
			role: ADMIN | INSTALLER | OWNER | GUEST
		}]
	}
}
```

[1.1.1.1.3] Odoo reply on error
```
{ error }
// TODO
```

#### [1.1.2] At OpenEMS






[1.1.2.2] Reply
```
successful
{
	authenticate: {
		mode: allow, username, token
	}, metadata: {
		config: {},
		backend: "openems"
	}
}
failed
{
	authenticate: {
		mode: deny
	}
}
```







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
successful
{
	authenticate: {
		mode: allow, username, token
	}, metadata: {
		config: {},
		backend: "openems"
	}
}
failed
{
	authenticate: {
		mode: deny
	}
}
```

### [1.2] Connect to device




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