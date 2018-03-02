metadata {
    definition (name: "Spikey Contact", namespace: "spikey", author: "ilmaruk") {
        capability "Signal Strength"
        capability "Contact Sensor"
        capability "Temperature Measurement"
        capability "Sensor"
        capability "Battery"
        capability "Refresh"

        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0020, 0402, 0500", manufacturer: "AlertMe.com", model: "WDS00140002", deviceJoinName: "Spikey Contact"
    }

    tiles {
        valueTile("temperature", "device.temperature", width: 1, height: 2) {
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
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 1, height: 2) {
			state "battery", label: '${currentValue}% battery', unit: ""
		}
        standardTile("contact", "device.contact", width: 2, height: 2) {
            state("closed", label: "Closed", icon: "st.contact.contact.closed", backgroundColor: "#00c000")
            state("open", label: "Open", icon: "st.contact.contact.open", backgroundColor: "#c00000")
        }
    }
}

def configure() {
	return zigbee.batteryConfig() + zigbee.temperatureConfig(30, 300)
}

def parse(String description) {
    log.debug "description: $description"
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
        return
    }
    
    return createEvent(name: name, value: value)
}

def refresh() {
	log.debug "refresh called"

	def refreshCmds = zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020) +
			zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, 0x0000)

	return refreshCmds // + zigbee.enrollResponse()
}
