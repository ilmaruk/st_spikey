metadata {
    definition (name: "Spikey Keyfob", namespace: "spikey", author: "ilmaruk") {
        capability "Actuator"
        capability "Button"
    	capability "Holdable Button"
        capability "Battery"
        capability "Signal Strength"
        capability "Configuration"
        capability "Refresh"

        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0020, 0500", outClusters: "0501",
        	manufacturer: "AlertMe.com", model: "KEY00140002", deviceJoinName: "Spikey Keyfob"
    }
    
    tiles {
        standardTile("refresh", "device.battery", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 1, height: 1) {
			state "battery", label: '${currentValue}%', unit: ""
		}
        main(["refresh", "battery"])
    }
}

def configure() {
	log.debug "Configuring."
	return zigbee.configureReporting(0x0001, 0x0021, 0x20, 30, 300, 0x01) +
        zigbee.readAttribute(0x0001, 0x0021)
}

def refresh() {
	log.debug "Refreshing"
	return zigbee.readAttribute(0x0001, 0x0021) +
    	zigbee.configureReporting(0x0001, 0x0021, 0x20, 30, 300, 0x01)
}

def parse(String description) {
	log.debug "description $description"
    
    def shield = zigbee.parse(description)
    log.debug shield
    
    Map map = zigbee.getEvent(description)
    if (map) {
    	def response = zigbee.createEvent(map)
        log.debug response
        return response
    } else {
    	log.warn "Cannot get event from $description"
    }
    
    if (description.startsWith('read attr -')) {
    	return parseAttributeResponse(description)
    }
}


private def parseAttributeResponse(String description) {
	Map descMap = zigbee.parseDescriptionAsMap(description)
	log.trace "ZigBee DTH - Executing parseAttributeResponse() for device ${device.displayName} with description map:- $descMap"
	def result = []
	Map responseMap = [:]
	def clusterInt = descMap.clusterInt
	def attrInt = descMap.attrInt
	def deviceName = device.displayName
	if (clusterInt == 0x0001 && attrInt == 0x0021) {
		responseMap.name = "battery"
		responseMap.value = Math.round(Integer.parseInt(descMap.value, 16) / 2)
		responseMap.descriptionText = "Battery is at ${responseMap.value}%"
	} else {
		log.trace "ZigBee DTH - parseAttributeResponse() - ignoring attribute response"
		return null
	}

	if (responseMap.data) {
		responseMap.data.lockName = deviceName
	} else {
		responseMap.data = [ lockName: deviceName ]
	}
	result << createEvent(responseMap)
	log.info "ZigBee DTH - parseAttributeResponse() returning with result:- $result"
	return result
}

def ping() {
	log.trace "ZigBee DTH - Executing ping() for device ${device.displayName}"
	refresh()
}
