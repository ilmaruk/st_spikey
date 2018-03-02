metadata {
    definition (name: "Spikey PIR", namespace: "smartthings", author: "ilmaruk") {
        capability "Signal Strength"
        capability "Motion Sensor"
        capability "Sensor"
        capability "Battery"

        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0020, 0402, 0406, 0500", manufacturer: "AlertMe.com", model: "PIR00140005", deviceJoinName: "Spikey PIR"
    }

    tiles {
        standardTile("motion", "device.motion", width: 1, height: 1) {
            state("active", label: "Motion", icon: "st.motion.motion.active", backgroundColor: "#53a7c0")
            state("inactive", label: "No Motion", icon: "st.motion.motion.inactive", backgroundColor: "#ffffff")
        }
    }
}

def configure() {
	configureReporting(0x0402, 0x0000, 0x29, 0, 300, null)
}

def parse(String description) {
	log.debug "description $description"
	def name = null
	def value = description
	def descriptionText = null
	if (zigbee.isZoneType19(description)) {
		name = "motion"
		def isActive = zigbee.translateStatusZoneType19(description)
		value = isActive ? "active" : "inactive"
		descriptionText = isActive ? "${device.displayName} detected motion" : "${device.displayName} motion has stopped"
        
        if (isActive) {
        	runIn(30, resetStatus)
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

def resetStatus() {
	log.debug "Resetting ${device.displayName} motion status"
    sendEvent(name: "motion", value: "inactive", descriptionText: "${device.displayName} motion reset", isStateChange: true)
}