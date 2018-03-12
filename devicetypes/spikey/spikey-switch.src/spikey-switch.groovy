metadata {
    definition (name: "Spikey Switch", namespace: "spikey", author: "ilmaruk") {
        capability "Actuator"
        capability "Switch"
        capability "Power Meter"
        capability "Configuration"
        capability "Refresh"

        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0702", outClusters: "0019", manufacturer: "Computime", model: "SPG00080001", deviceJoinName: "Spikey Switch"
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'On', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'Off', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'Turning On', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'Turning Off', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("power", key: "SECONDARY_CONTROL") {
                attributeState "power", label:'${currentValue}W'
            }
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 6, height: 1) {
            state "default", label:"Refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main "switch"
        details(["switch", "power", "refresh"])
    }
}

def parse(String description) {
    log.debug "description is $description"

    def event = zigbee.getEvent(description)
    if (event) {
    	log.debug "Sending event $event"
        return sendEvent(event)
    } else if (description.startsWith('read attr -')) {
    	return parseAttributeResponse(description)
    } else {
        log.warn "DID NOT PARSE MESSAGE for description : $description"
        log.debug zigbee.parseDescriptionAsMap(description)
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
	} else if (clusterInt == 0x0702 && attrInt == 0x0400) {
		responseMap.name = "power"
		responseMap.value = Integer.parseInt(descMap.value, 16)
		responseMap.descriptionText = "Power reading is ${responseMap.value}"
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

def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def refresh() {
    return zigbee.readAttribute(0x0006, 0x0000) +
        zigbee.readAttribute(0x0008, 0x0000) +
        zigbee.readAttribute(0x0702, 0x0400) +
        zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null) +
        zigbee.configureReporting(0x0008, 0x0000, 0x20, 1, 3600, 0x01) +
        zigbee.configureReporting(0x0702, 0x0400, 0x25, 1, 600, 0x05)
}

def configure() {
    log.debug "Configuring Reporting and Bindings."

    return zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null) +
        zigbee.configureReporting(0x0008, 0x0000, 0x20, 1, 3600, 0x01) +
        zigbee.configureReporting(0x0702, 0x0400, 0x25, 1, 600, 0x05) +
        zigbee.readAttribute(0x0006, 0x0000) +
        zigbee.readAttribute(0x0008, 0x0000) +
        zigbee.readAttribute(0x0702, 0x0400)
}