import SwiftUI

struct ContentView: View {
    @AppStorage("gateway_base_url") private var gatewayBaseUrl = "http://127.0.0.1:8765"
    @AppStorage("device_id") private var deviceId = ""
    @AppStorage("device_token") private var deviceToken = ""
    @State private var gatewayInput = "http://127.0.0.1:8765"
    @State private var pairingCode = ""
    @State private var deviceName = UIDevice.current.name

    private var isPaired: Bool {
        !deviceId.isEmpty && !deviceToken.isEmpty
    }

    var body: some View {
        NavigationStack {
            List {
                Section("Gateway") {
                    TextField("Gateway URL", text: $gatewayInput)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                    Button("Save Gateway") {
                        gatewayBaseUrl = normalized(gatewayInput)
                        clearPairing()
                    }
                    Text("Current: \(gatewayBaseUrl)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Section("Pairing") {
                    TextField("Device name", text: $deviceName)
                    TextField("Pairing code", text: $pairingCode)
                        .keyboardType(.numberPad)
                    if isPaired {
                        Label("Paired: \(deviceId)", systemImage: "checkmark.seal.fill")
                            .foregroundStyle(.green)
                        Button("Clear local pairing", role: .destructive) {
                            clearPairing()
                        }
                    } else {
                        Text("Use the gateway pairing code, then complete pairing from the Android/KMP implementation path once the iOS networking shell is wired.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Section("Status") {
                    Label("iOS app shell is installable", systemImage: "iphone")
                    Label("Shared KMP framework is embedded by Xcode build phase", systemImage: "shippingbox")
                    Label("Gateway URL and pairing placeholders persist locally", systemImage: "lock")
                }
            }
            .navigationTitle("Hermes Mobile")
            .onAppear {
                gatewayInput = gatewayBaseUrl
                if deviceName.isEmpty {
                    deviceName = UIDevice.current.name
                }
            }
        }
    }

    private func normalized(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    private func clearPairing() {
        deviceId = ""
        deviceToken = ""
        pairingCode = ""
    }
}

#Preview {
    ContentView()
}
