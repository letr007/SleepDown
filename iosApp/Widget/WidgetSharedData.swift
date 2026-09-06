import Foundation
import WidgetKit
import shared

/// The app and widget exchange only this versioned catalog in the App Group container.
enum WidgetDeepLink {
    static let scheme = "sleepdown"

    struct Request: Identifiable, Equatable {
        let id = UUID()
        let destination: Destination
    }

    struct Destination: Equatable {
        let timetableID: String?
        let epochDay: Int64?
    }

    static func weekURL(timetableID: String? = nil, epochDay: Int64? = nil) -> URL {
        var components = URLComponents()
        components.scheme = scheme
        components.host = "week"
        var queryItems: [URLQueryItem] = []
        if let timetableID { queryItems.append(URLQueryItem(name: "tableID", value: timetableID)) }
        if let epochDay { queryItems.append(URLQueryItem(name: "epochDay", value: String(epochDay))) }
        if !queryItems.isEmpty { components.queryItems = queryItems }
        return components.url ?? URL(string: "\(scheme)://week")!
    }

    static func destination(from url: URL) -> Destination? {
        guard url.scheme == scheme, url.host == "week",
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return nil }
        let tableID = components.queryItems?.first(where: { $0.name == "tableID" })?.value
        let dayValue = components.queryItems?.first(where: { $0.name == "epochDay" })?.value
        let day = dayValue.flatMap(Int64.init)
        // Only civil dates in years 1...9999 are accepted at the external URL boundary.
        if dayValue != nil {
            guard let day, (-719_162...2_932_896).contains(day) else { return nil }
        }
        return Destination(timetableID: tableID, epochDay: day)
    }

    static func epochDay(from url: URL) -> Int64? { destination(from: url)?.epochDay }
}

enum WidgetSharedData {
    static let appGroupID = "group.com.letr.sleepdown"
    static let catalogFileName = "widget-timetables-v1.json"
    static let selectedTimetableKey = "selectedTimetableID"
    static let catalogSchemaVersion = 1

    enum PublishError: LocalizedError, Equatable {
        case appGroupUnavailable
        case sharedDefaultsUnavailable
        case encodingFailed(String)
        case writeFailed(String)

        var errorDescription: String? {
            switch self {
            case .appGroupUnavailable:
                return AppLocalization.string("widget.app_group_unavailable")
            case .sharedDefaultsUnavailable:
                return AppLocalization.string("widget.shared_defaults_unavailable")
            case .encodingFailed(let message):
                return AppLocalization.string("widget.data_encoding_failed", message)
            case .writeFailed(let message):
                return AppLocalization.string("widget.data_write_failed", message)
            }
        }
    }

    enum CatalogState {
        case unconfigured
        case missing
        case valid(Catalog)
        case corrupted(String)
    }

    struct Catalog: Codable, Equatable, Sendable {
        let schemaVersion: Int
        let generatedAt: Date
        let selectedTimetableID: String?
        let timetables: [Record]
    }

    struct Record: Codable, Equatable, Identifiable, Sendable {
        let id: String
        let name: String
        let sortOrder: Int32
        let payload: String
    }

    @discardableResult
    static func publish(timetables: [Timetable], selectedTimetableID: String?) -> Bool {
        if case .success = publishResult(
            timetables: timetables,
            selectedTimetableID: selectedTimetableID
        ) {
            return true
        }
        return false
    }

    static func publishResult(
        timetables: [Timetable],
        selectedTimetableID: String?
    ) -> Result<Void, PublishError> {
        let records: [Record]
        do {
            records = try timetables.map { timetable in
                Record(
                    id: timetable.id,
                    name: timetable.name,
                    sortOrder: timetable.sortOrder,
                    payload: try BackupFormat.shared.encode(timetable: timetable)
                )
            }
        } catch {
            return .failure(.encodingFailed(error.localizedDescription))
        }

        let catalog = Catalog(
            schemaVersion: catalogSchemaVersion,
            generatedAt: Date(),
            selectedTimetableID: selectedTimetableID,
            timetables: records
        )
        guard let url = catalogURL else {
            return .failure(.appGroupUnavailable)
        }
        guard let defaults = UserDefaults(suiteName: appGroupID) else {
            return .failure(.sharedDefaultsUnavailable)
        }

        do {
            let data = try JSONEncoder().encode(catalog)
            try data.write(to: url, options: .atomic)
            defaults.set(selectedTimetableID, forKey: selectedTimetableKey)
            defaults.set(catalogSchemaVersion, forKey: "widgetCatalogSchemaVersion")
            WidgetCenter.shared.reloadAllTimelines()
            return .success(())
        } catch {
            return .failure(.writeFailed(error.localizedDescription))
        }
    }

    static func catalogState() -> CatalogState {
        guard let url = catalogURL else { return .unconfigured }

        var isDirectory = ObjCBool(false)
        guard FileManager.default.fileExists(atPath: url.path, isDirectory: &isDirectory) else {
            return .missing
        }
        if isDirectory.boolValue {
            return .corrupted(AppLocalization.string("widget.catalog_path_directory"))
        }

        do {
            let data = try Data(contentsOf: url)
            let catalog = try JSONDecoder().decode(Catalog.self, from: data)
            guard catalog.schemaVersion == catalogSchemaVersion else {
                return .corrupted(AppLocalization.string("widget.catalog_version_unsupported"))
            }
            // Payload validation is scoped to the selected table when building its entries.
            return .valid(catalog)
        } catch {
            return .corrupted(AppLocalization.string("widget.catalog_unreadable", error.localizedDescription))
        }
    }

    static func loadCatalog() -> Catalog? {
        guard
            let url = catalogURL,
            let data = try? Data(contentsOf: url),
            let catalog = try? JSONDecoder().decode(Catalog.self, from: data),
            catalog.schemaVersion == catalogSchemaVersion
        else {
            return nil
        }
        return catalog
    }

    static func loadTimetable(id: String) -> Timetable? {
        guard
            let record = loadCatalog()?.timetables.first(where: { $0.id == id }),
            let timetable = try? BackupFormat.shared.decode(json: record.payload)
        else {
            return nil
        }
        return timetable
    }

    private static var catalogURL: URL? {
        guard let container = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: appGroupID
        ) else {
            return nil
        }
        return container.appendingPathComponent(catalogFileName, isDirectory: false)
    }
}

enum WidgetDateSupport {
    private static var utcCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        calendar.locale = Locale(identifier: "en_US_POSIX")
        return calendar
    }()

    static var localCalendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        return calendar
    }

    static func nowComponents(for date: Date = Date(), calendar: Calendar = localCalendar) -> (epochDay: Int64, minuteOfDay: Int32) {
        let local = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let civilDay = utcCalendar.date(from: DateComponents(
            calendar: utcCalendar,
            timeZone: utcCalendar.timeZone,
            year: local.year,
            month: local.month,
            day: local.day
        )) ?? date
        let epochDay = Int64(floor(civilDay.timeIntervalSince1970 / 86_400))
        let minute = Int32((local.hour ?? 0) * 60 + (local.minute ?? 0))
        return (epochDay, minute)
    }

    static func date(epochDay: Int64, minuteOfDay: Int32 = 0, calendar: Calendar = localCalendar,
                     repeatedTimePolicy: Calendar.RepeatedTimePolicy = .first) -> Date? {
        let civil = utcCalendar.dateComponents([.year, .month, .day], from: Date(timeIntervalSince1970: TimeInterval(epochDay) * 86_400))
        guard let start = calendar.date(from: civil) else { return nil }
        return calendar.date(bySettingHour: Int(minuteOfDay) / 60, minute: Int(minuteOfDay) % 60,
            second: 0, of: start, matchingPolicy: .nextTime, repeatedTimePolicy: repeatedTimePolicy)
    }

    static func formatTime(_ minuteOfDay: Int32) -> String {
        let totalMinutes = max(0, Int(minuteOfDay))
        return String(format: "%02d:%02d", totalMinutes / 60, totalMinutes % 60)
    }

    static func formatDate(_ epochDay: Int64) -> String {
        let civil = utcCalendar.dateComponents(
            [.year, .month, .day],
            from: Date(timeIntervalSince1970: TimeInterval(epochDay) * 86_400)
        )
        var local = DateComponents()
        local.calendar = localCalendar
        local.timeZone = localCalendar.timeZone
        local.year = civil.year
        local.month = civil.month
        local.day = civil.day
        local.hour = 12
        let date = localCalendar.date(from: local) ?? Date()
        let formatter = DateFormatter()
        formatter.locale = AppLocalization.currentLocale
        formatter.setLocalizedDateFormatFromTemplate("MMMd")
        return formatter.string(from: date)
    }
}

/// Presentation rules shared by the extension and app-hosted widget tests.
enum WidgetPresentation {
    static func record(in catalog: WidgetSharedData.Catalog, configuredID: String?) -> WidgetSharedData.Record? {
        let id = configuredID ?? catalog.selectedTimetableID
            ?? catalog.timetables.sorted {
                $0.sortOrder == $1.sortOrder ? $0.name < $1.name : $0.sortOrder < $1.sortOrder
            }.first?.id
        return catalog.timetables.first { $0.id == id }
    }

    static func snapshot(
        timetable: Timetable, kind: WidgetSnapshotKind, nowEpochDay: Int64,
        nowMinuteOfDay: Int32, includesTomorrow: Bool = false
    ) -> WidgetSnapshot {
        let source = WidgetSnapshotBuilder.shared.build(kind: kind, timetable: timetable,
            nowEpochDay: nowEpochDay, nowMinuteOfDay: nowMinuteOfDay)
        var items = source.items.filter {
            $0.epochDay > nowEpochDay || ($0.epochDay == nowEpochDay && $0.endMinuteOfDay >= nowMinuteOfDay)
        }
        if includesTomorrow {
            let tomorrow = WidgetSnapshotBuilder.shared.buildToday(timetable: timetable,
                nowEpochDay: nowEpochDay + 1, nowMinuteOfDay: 0)
            items += tomorrow.items.map { item in
                WidgetSnapshotItem(occurrenceId: item.occurrenceId, courseId: item.courseId,
                    courseName: item.courseName, epochDay: item.epochDay,
                    startMinuteOfDay: item.startMinuteOfDay, endMinuteOfDay: item.endMinuteOfDay,
                    teacher: item.teacher, room: item.room, note: item.note, color: item.color,
                    isCurrent: false, conflictCount: item.conflictCount, isPreferred: item.isPreferred)
            }
        }
        if kind == .week {
            items = items.filter { item in
                let day = item.epochDay - source.anchorEpochDay
                return (day != 5 || timetable.showSaturday) && (day != 6 || timetable.showSunday)
            }
            if timetable.sundayFirst {
                let sunday = source.anchorEpochDay + 6
                items = items.filter { $0.epochDay == sunday } + items.filter { $0.epochDay != sunday }
            }
        }
        return WidgetSnapshot(kind: kind, timetableId: timetable.id, anchorEpochDay: source.anchorEpochDay,
            generatedAtMinuteOfDay: nowMinuteOfDay, items: items)
    }

    static func timelineDates(timetable: Timetable, now: Date, calendar: Calendar = WidgetDateSupport.localCalendar) -> [Date] {
        let civil = WidgetDateSupport.nowComponents(for: now, calendar: calendar)
        let midnight = calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: now))!
        let today = WidgetSnapshotBuilder.shared.buildToday(timetable: timetable,
            nowEpochDay: civil.epochDay, nowMinuteOfDay: civil.minuteOfDay)
        var changes: Set<Date> = [now, midnight]
        if let transition = calendar.timeZone.nextDaylightSavingTimeTransition(after: now), transition < midnight {
            changes.insert(transition)
        }
        for item in today.items {
            // isCurrent ends at endMinute; remaining-course visibility ends a minute later.
            for minute in [item.startMinuteOfDay, item.endMinuteOfDay, item.endMinuteOfDay + 1] where minute < 1440 {
                for policy in [Calendar.RepeatedTimePolicy.first, .last] {
                    if let date = WidgetDateSupport.date(epochDay: civil.epochDay, minuteOfDay: minute,
                        calendar: calendar, repeatedTimePolicy: policy), date > now, date < midnight {
                        changes.insert(date)
                    }
                }
            }
        }
        // The provider requests a new timeline at the final supplied boundary if this cap is reached.
        return Array(changes.sorted().prefix(64))
    }

    static func visibleItemLimit(height: CGFloat, rowHeight: CGFloat, footerHeight: CGFloat = 24) -> Int {
        max(0, Int(max(0, height - footerHeight) / max(1, rowHeight)))
    }
}
