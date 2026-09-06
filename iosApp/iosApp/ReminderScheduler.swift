@preconcurrency import BackgroundTasks
import Combine
import Foundation
import UserNotifications
@preconcurrency import shared

@MainActor
final class ReminderScheduler: ObservableObject {
    static let backgroundTaskIdentifier = "com.letr.sleepdown.refresh-reminders"
    static let backgroundRefreshInterval: TimeInterval = 15 * 60

    enum AuthorizationState: Equatable {
        case notDetermined
        case denied
        case authorized
        case provisional
        case ephemeral
        case unknown

        var title: String {
            switch self {
            case .notDetermined: return AppLocalization.string("reminder.authorization.not_determined")
            case .denied: return AppLocalization.string("reminder.authorization.denied")
            case .authorized: return AppLocalization.string("reminder.authorization.authorized")
            case .provisional: return AppLocalization.string("reminder.authorization.provisional")
            case .ephemeral: return AppLocalization.string("reminder.authorization.ephemeral")
            case .unknown: return AppLocalization.string("reminder.authorization.unknown")
            }
        }

        var isUsable: Bool {
            self == .authorized || self == .provisional || self == .ephemeral
        }
    }

    @Published private(set) var authorizationState: AuthorizationState = .notDetermined
    @Published private(set) var scheduledCount = 0
    @Published private(set) var remainingCount = 0
    @Published private(set) var lastError: String?

    private let center: UNUserNotificationCenter
    private let batchSize = 64
    private var remainingPlans: [ReminderPlan] = []
    private var managedPlanIDs = Set<String>()
    private var inFlightPlanIDs = Set<String>()
    private var scheduleGeneration = UUID()
    private var backgroundTaskGeneration: UUID?

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
    }

    static func backgroundRefreshDate(now: Date = Date()) -> Date {
        now.addingTimeInterval(backgroundRefreshInterval)
    }

    @available(iOS 15.0, *)
    static func makeBackgroundRefreshRequest(now: Date = Date()) -> BGAppRefreshTaskRequest {
        let request = BGAppRefreshTaskRequest(identifier: backgroundTaskIdentifier)
        request.earliestBeginDate = backgroundRefreshDate(now: now)
        return request
    }

    @discardableResult
    func requestBackgroundRefresh() -> Bool {
        guard #available(iOS 15.0, *) else { return true }
        do {
            try BGTaskScheduler.shared.submit(Self.makeBackgroundRefreshRequest())
            return true
        } catch {
            lastError = AppLocalization.string(
                "reminder.background_refresh_error",
                error.localizedDescription
            )
            return false
        }
    }

    static func notificationSound(for plan: ReminderPlan) -> UNNotificationSound? {
        plan.silent ? nil : .default
    }

    func refreshAuthorization() {
        center.getNotificationSettings { [weak self] settings in
            Task { @MainActor [weak self] in
                self?.authorizationState = Self.authorizationState(for: settings.authorizationStatus)
            }
        }
    }

    private static func authorizationState(for status: UNAuthorizationStatus) -> AuthorizationState {
        switch status {
        case .notDetermined: return .notDetermined
        case .denied: return .denied
        case .authorized: return .authorized
        case .provisional: return .provisional
        case .ephemeral: return .ephemeral
        @unknown default: return .unknown
        }
    }

    func requestAuthorization() {
        center.requestAuthorization(options: [.alert, .sound, .badge]) { [weak self] _, error in
            Task { @MainActor [weak self] in
                if let error {
                    self?.lastError = AppLocalization.string("reminder.operation_error", error.localizedDescription)
                }
                self?.refreshAuthorization()
            }
        }
    }

    func schedule(
        timetable: Timetable?,
        scheduleBackgroundRefresh: Bool = true,
        completion: ((Bool) -> Void)? = nil
    ) {
        scheduleGeneration = UUID()
        remainingPlans = []
        managedPlanIDs = []
        inFlightPlanIDs = []
        scheduledCount = 0
        remainingCount = 0
        center.removeAllPendingNotificationRequests()

        guard let timetable, authorizationState.isUsable else {
            completion?(true)
            return
        }

        let plans = ReminderPlanner.shared.plan(timetable: timetable)
            .sorted {
                if $0.triggerEpochDay != $1.triggerEpochDay {
                    return $0.triggerEpochDay < $1.triggerEpochDay
                }
                return $0.triggerMinuteOfDay < $1.triggerMinuteOfDay
            }
        let now = Date()
        remainingPlans = plans.filter { plan in
            guard let date = localDate(for: plan) else { return false }
            return date > now
        }
        managedPlanIDs = Set(remainingPlans.map(\.id))
        updateRemainingCount()
        supplementPendingRequests { [weak self] success in
            guard let self else {
                completion?(false)
                return
            }
            let refreshSuccess = !self.remainingPlans.isEmpty
                ? (!scheduleBackgroundRefresh || self.requestBackgroundRefresh())
                : true
            completion?(success && refreshSuccess)
        }
    }

    /// Fills the next batch after delivered requests leave capacity. Call this
    /// when the app becomes active and after changing the selected timetable.
    func supplementPendingRequests(completion: ((Bool) -> Void)? = nil) {
        guard authorizationState.isUsable else {
            refreshPendingCount(completion: completion)
            return
        }

        let generation = scheduleGeneration
        center.getPendingNotificationRequests { [weak self] requests in
            Task { @MainActor [weak self] in
                guard let self, self.scheduleGeneration == generation else {
                    completion?(false)
                    return
                }
                self.reconcilePendingRequests(requests)

                let pendingIDs = Set(requests.map(\.identifier)).intersection(self.managedPlanIDs)
                let capacity = max(0, self.batchSize - pendingIDs.count - self.inFlightPlanIDs.count)
                let candidates = self.remainingPlans
                    .filter { !self.inFlightPlanIDs.contains($0.id) }
                    .prefix(capacity)
                guard !candidates.isEmpty else {
                    self.updateRemainingCount()
                    completion?(true)
                    return
                }

                var pendingAdds = candidates.count
                var allSucceeded = true
                for plan in candidates {
                    self.inFlightPlanIDs.insert(plan.id)
                    self.add(plan, generation: generation) { success in
                        allSucceeded = allSucceeded && success
                        pendingAdds -= 1
                        guard pendingAdds == 0 else { return }
                        self.refreshPendingCount { refreshed in
                            completion?(allSucceeded && refreshed)
                        }
                    }
                }
                self.updateRemainingCount()
            }
        }
    }

    func refreshPendingCount(completion: ((Bool) -> Void)? = nil) {
        let generation = scheduleGeneration
        center.getPendingNotificationRequests { [weak self] requests in
            Task { @MainActor [weak self] in
                guard let self, self.scheduleGeneration == generation else {
                    completion?(false)
                    return
                }
                self.reconcilePendingRequests(requests)
                completion?(true)
            }
        }
    }

    private func add(
        _ plan: ReminderPlan,
        generation: UUID,
        completion: @escaping (Bool) -> Void
    ) {
        let planID = plan.id
        let components = TimetableDates.localDateComponents(
            forEpochDay: plan.triggerEpochDay,
            minuteOfDay: plan.triggerMinuteOfDay
        )

        let content = UNMutableNotificationContent()
        content.title = Self.localizedNotificationTitle(for: plan)
        content.body = plan.body
        content.sound = Self.notificationSound(for: plan)
        content.badge = 1
        content.threadIdentifier = "sleepdown.reminders"

        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let request = UNNotificationRequest(
            identifier: planID,
            content: content,
            trigger: trigger
        )
        center.add(request) { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self, self.scheduleGeneration == generation else {
                    completion(false)
                    return
                }
                self.inFlightPlanIDs.remove(planID)
                if let error {
                    self.lastError = AppLocalization.string("reminder.operation_error", error.localizedDescription)
                }
                completion(error == nil)
            }
        }
    }

    @available(iOS 15.0, *)
    func handleBackgroundRefresh(_ task: BGAppRefreshTask, timetable: Timetable?) {
        let generation = UUID()
        backgroundTaskGeneration = generation
        task.expirationHandler = { [weak self] in
            Task { @MainActor [weak self] in
                guard let self, self.backgroundTaskGeneration == generation else { return }
                self.backgroundTaskGeneration = nil
                task.setTaskCompleted(success: false)
            }
        }

        center.getNotificationSettings { [weak self] settings in
            guard self != nil else {
                task.setTaskCompleted(success: false)
                return
            }
            Task { @MainActor [weak self] in
                guard let self, self.backgroundTaskGeneration == generation else { return }
                self.authorizationState = Self.authorizationState(for: settings.authorizationStatus)
                self.schedule(
                    timetable: timetable,
                    scheduleBackgroundRefresh: false
                ) { [weak self] success in
                    guard let self, self.backgroundTaskGeneration == generation else { return }
                    self.backgroundTaskGeneration = nil
                    let refreshSuccess = self.remainingPlans.isEmpty || self.requestBackgroundRefresh()
                    task.setTaskCompleted(success: success && refreshSuccess)
                }
            }
        }
    }

    private static func localizedNotificationTitle(for plan: ReminderPlan) -> String {
        let kind = String(describing: plan.kind).lowercased()
        if kind.contains("lessonstart") || kind.contains("lesson_start") {
            return AppLocalization.string("reminder.notification.lesson_start")
        }
        if kind.contains("lessonend") || kind.contains("lesson_end") {
            return AppLocalization.string("reminder.notification.lesson_end")
        }
        return AppLocalization.string("reminder.notification.generic")
    }

    private func reconcilePendingRequests(_ requests: [UNNotificationRequest]) {
        let pendingIDs = Set(requests.map(\.identifier))
        let managedPendingIDs = pendingIDs.intersection(managedPlanIDs)
        scheduledCount = managedPendingIDs.count
        remainingPlans.removeAll { managedPendingIDs.contains($0.id) }
        updateRemainingCount()
    }

    private func updateRemainingCount() {
        remainingCount = remainingPlans.count
    }

    private func localDate(for plan: ReminderPlan) -> Date? {
        let components = TimetableDates.localDateComponents(
            forEpochDay: plan.triggerEpochDay,
            minuteOfDay: plan.triggerMinuteOfDay
        )
        return Calendar.current.date(from: components)
    }
}
