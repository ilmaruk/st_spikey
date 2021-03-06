metadata {
    definition (name: "Spikey Button", namespace: "spikey", author: "ilmaruk") {
        capability "Actuator"
        capability "Switch"
        capability "Temperature Measurement"
        capability "Battery"
        capability "Signal Strength"
        capability "Configuration"
        capability "Refresh"

        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0020, 0402", outClusters: "0006",
                manufacturer: "AlertMe.com", model: "BTN00140004", deviceJoinName: "Spikey Button/Switch"
    }

    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2) {
            state("on", label: "On", icon: "st.button.button.pushed", backgroundColor: "#00c000")
            state("off", label: "Off", icon: "st.button.button.held", backgroundColor: "#c00000")
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", label:"Refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main "switch"
        details(["switch", "refresh"])
    }
}

def parse(String description) {
	log.info "Parsing message: $description"
    
	Map descMap = zigbee.parseDescriptionAsMap(description)
    log.debug "Ignored event: $descMap"
    if (descMap.commandInt == 2) {
        // We're only interested in toggle commands
        String newState = device.currentValue("switch") == "on" ? "off" : "on"
        return createEvent(name: "switch", value: newState)
    }
}

def refresh() {
    log.info "Refreshing reporting and bindings."

    return zigbee.readAttribute(0x0006, 0x0000) +
        zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null)
}

def configure() {
    log.info "Configuring reporting and bindings."

    return zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null) +
        zigbee.readAttribute(0x0006, 0x0000)
}