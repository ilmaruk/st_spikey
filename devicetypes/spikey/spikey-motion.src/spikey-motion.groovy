metadata {
    definition (name: "Spikey Motion", namespace: "spikey", author: "ilmaruk") {
        capability "Signal Strength"
        capability "Motion Sensor"
        capability "Sensor"
        capability "Battery"
        capability "Temperature Measurement"
        capability "Configuration"
        capability "Refresh"

        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0020, 0402, 0406, 0500", manufacturer: "AlertMe.com", model: "PIR00140005", deviceJoinName: "Spikey Motion"
    }

    tiles {
        standardTile("motion", "device.motion", width: 3, height: 2) {
            state("active", label: "Motion", icon: "st.motion.motion.active", backgroundColor: "#53a7c0")
            state("inactive", label: "No Motion", icon: "st.motion.motion.inactive", backgroundColor: "#ffffff")
        }
        standardTile("refresh", "device.motion", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 1, height: 1) {
			state "temperature", label:'${currentValue}C', unit: "C",
			backgroundColors:[
				[value: 0, color: "#153591"],
				[value: 7, color: "#1e9cbb"],
				[value: 15, color: "#90d2a7"],
				[value: 23, color: "#44b621"],
				[value: 29, color: "#f1d801"],
				[value: 35, color: "#d04e00"],
				[value: 36, color: "#bc2323"]
			]
		}
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 1, height: 1) {
			state "battery", label: '${currentValue}%', unit: ""
		}
        main(["motion", "temperature", "refresh", "battery"])
    }
}

def configure() {
	log.debug "Configuring Reporting and Bindings."
	return zigbee.configureReporting(0x0402, 0x0000, 0x29, 30, 300, 0x0064) +
    	zigbee.configureReporting(0x0001, 0x0021, 0x20, 30, 21600, 0x01) +
    	zigbee.readAttribute(0x0402, 0x0000) +
        zigbee.readAttribute(0x0001, 0x0021)
}

def refresh() {
	log.debug "Refreshing."
	return zigbee.readAttribute(0x0402, 0x0000) +
    	zigbee.readAttribute(0x0001, 0x0021) +
    	zigbee.configureReporting(0x0402, 0x0000, 0x29, 30, 300, 0x0064) +
        zigbee.configureReporting(0x0001, 0x0021, 0x20, 30, 21600, 0x01)
}

def parse(String description) {
	log.debug "description $description"
    
    Map map = zigbee.getEvent(description)
    if (map) {
    	log.debug map
	    log.debug map.name
        def result = createEvent(map)
        log.info "Parse returned ${result?.descriptionText}"
        return result
    } else {
    	log.debug "Unable to getEvent"
    }
    
    if (description.startsWith('read attr -')) {
    	return parseAttributeResponse(description)
    }
    
	def name = null
	def value = description
	def descriptionText = null
	if (zigbee.isZoneType19(description)) {
		name = "motion"
		def isActive = zigbee.translateStatusZoneType19(description)
		value = isActive ? "active" : "inactive"
		descriptionText = isActive ? "${device.displayName} detected motion" : "${device.displayName} motion has stopped"
        
        if (isActive) {
        	runIn(28, resetStatus)
        }
	}

	def result = createEvent(
		name: name,
		value: value,
		descriptionText: descriptionText
	)

	log.info "Parse returned ${result?.descriptionText}"
	return result
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

def resetStatus() {
	log.debug "Resetting ${device.displayName} motion status"
    sendEvent(name: "motion", value: "inactive", descriptionText: "${device.displayName} motion reset", isStateChange: true)
}

def ping() {
	log.trace "ZigBee DTH - Executing ping() for device ${device.displayName}"
	refresh()
}
