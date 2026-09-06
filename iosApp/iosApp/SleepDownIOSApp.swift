import BackgroundTasks
import SwiftUI
import shared

@main
struct SleepDownIOSApp: App {
    @StateObject private var store: TimetableStore
    @StateObject private var reminders: ReminderScheduler

    init() {
        AppLocalization.synchronizeWidgetLanguage()
        let store = TimetableStore()
        let reminders = ReminderScheduler()
        _store = StateObject(wrappedValue: store)
        _reminders = StateObject(wrappedValue: reminders)
        registerBackgroundRefreshTask(store: store, reminders: reminders)
    }

    var body: some Scene {
        WindowGroup {
            TimetableRootView()
                .environmentObject(store)
                .environmentObject(reminders)
        }
    }

    private func registerBackgroundRefreshTask(store: TimetableStore, reminders: ReminderScheduler) {
        guard #available(iOS 15.0, *) else { return }
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: ReminderScheduler.backgroundTaskIdentifier,
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            Task { @MainActor [weak store, weak reminders] in
                guard let store, let reminders else {
                    refreshTask.setTaskCompleted(success: false)
                    return
                }
                reminders.handleBackgroundRefresh(
                    refreshTask,
                    timetable: store.selectedTimetable
                )
            }
        }
    }
}
