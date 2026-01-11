import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Image(systemName: "location.circle.fill")
                    .resizable()
                    .frame(width: 100, height: 100)
                    .foregroundColor(.blue)
                
                Text("Aurora Navigation")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Text("AI-Powered Smart Navigation")
                    .font(.subheadline)
                    .foregroundColor(.gray)
                
                Spacer().frame(height: 40)
                
                VStack(alignment: .leading, spacing: 15) {
                    FeatureRow(icon: "map.fill", title: "Smart Routes", description: "AI-powered route optimization")
                    FeatureRow(icon: "exclamationmark.triangle.fill", title: "Hazard Detection", description: "Real-time safety alerts")
                    FeatureRow(icon: "person.3.fill", title: "Social Features", description: "Share rides and chat with friends")
                    FeatureRow(icon: "brain.head.profile", title: "AI Assistant", description: "Natural language navigation")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(15)
                
                Spacer()
                
                Text("iOS app is under development")
                    .font(.footnote)
                    .foregroundColor(.gray)
                    .padding()
            }
            .padding()
            .navigationTitle("Aurora")
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let title: String
    let description: String
    
    var body: some View {
        HStack(spacing: 15) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.blue)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                Text(description)
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            
            Spacer()
        }
    }
}

#Preview {
    ContentView()
}
