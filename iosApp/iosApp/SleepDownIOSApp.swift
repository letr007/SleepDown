import SwiftUI
import shared

@main
struct SleepDownIOSApp: App {
    var body: some Scene {
        WindowGroup {
            KotlinRootViewController()
                .ignoresSafeArea()
        }
    }
}

private struct KotlinRootViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.SleepDownViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
