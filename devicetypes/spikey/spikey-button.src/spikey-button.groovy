metadata {
    definition (name: "Spikey Button", namespace: "spikey", author: "ilmaruk") {
        capability "Signal Strength"
        capability "Button"
        capability "Temperature Measurement"
        capability "Battery"

        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0020, 0402", outClusters: "0006",
                manufacturer: "AlertMe.com", model: "BTN00140004", deviceJoinName: "Spikey Button"
    }

    tiles {
        standardTile("button", "device.button", width: 2, height: 2) {
            state("pushed", label: "Pushed", icon: "st.button.button.pushed", backgroundColor: "#00c000")
            state("held", label: "Held", icon: "st.button.button.held", backgroundColor: "#c00000")
        }
    }
}

def parse(String description) {
    log.debug "description: $description"
}

def pushed() {
	log.debug "button pushed"
}

def held() {
	log.debug "button held"
}