metadata {
    definition (name: "Spikey Contact", namespace: "spikey", author: "ilmaruk") {
        capability "Signal Strength"
        capability "Contact Sensor"
        capability "Temperature Measurement"
        capability "Sensor"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"

        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0020, 0402, 0500", manufacturer: "AlertMe.com", model: "WDS00140002", deviceJoinName: "Spikey Contact"
    }

    tiles(scale: 2) {
        standardTile("contact", "device.contact", width: 4, height: 4) {
            state("closed", label: "Closed", icon: "st.contact.contact.closed", backgroundColor: "#00c000")
            state("open", label: "Open", icon: "st.contact.contact.open", backgroundColor: "#c00000")
        }
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label: '${currentValue}°', unit: "C",
					backgroundColors: [
							[value: 31, color: "#153591"],
							[value: 44, color: "#1e9cbb"],
							[value: 59, color: "#90d2a7"],
							[value: 74, color: "#44b621"],
							[value: 84, color: "#f1d801"],
							[value: 95, color: "#d04e00"],
							[value: 96, color: "#bc2323"]
					]
			)
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label: '${currentValue}%', unit: ""
		}
        standardTile("refresh", "device.battery", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
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
    log.debug "description: $description"
    
    Map map = zigbee.getEvent(description)
    if (map) {
    	log.debug map
	    log.debug map.name
        def result = createEvent(map)
        log.info "Parse returned ${result?.descriptionText}"
        return result
    }

    if (description.startsWith('read attr -')) {
    	return parseAttributeResponse(description)
    }

	String name = "contact"
    String value = null
    if (description == "zone status 0x0021 -- extended status 0x00" || description == "zone status 0x0025 -- extended status 0x00") {
    	// Open event
        value = "open"
    } else if (description == "zone status 0x0020 -- extended status 0x00" || description == "zone status 0x0024 -- extended status 0x00") {
        // Close event
        value = "closed"
    } else {
    	log.warning "Unknown description: $description"
        return null
    }
    
    return createEvent(name: name, value: value)
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
