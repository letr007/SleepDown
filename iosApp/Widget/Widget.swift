import Foundation
import SwiftUI
import UIKit
import WidgetKit
import shared

@available(iOS 17.0, *)
struct TimetableWidgetEntry: TimelineEntry {
    let date: Date
    let timetableName: String
    let snapshot: WidgetSnapshot?
    let statusMessage: String?
    var timetableID: String? = nil
}

@available(iOS 17.0, *)
struct TimetableWidgetProvider: AppIntentTimelineProvider {
    typealias Entry = TimetableWidgetEntry
    typealias Intent = TimetableSelectionIntent

    let snapshotKind: WidgetSnapshotKind
    let includesTomorrow: Bool

    init(snapshotKind: WidgetSnapshotKind, includesTomorrow: Bool = false) {
        self.snapshotKind = snapshotKind
        self.includesTomorrow = includesTomorrow
    }

    func placeholder(in context: Context) -> TimetableWidgetEntry {
        TimetableWidgetEntry(
            date: Date(),
            timetableName: AppLocalization.string("widget.timetable"),
            snapshot: nil,
            statusMessage: nil
        )
    }

    func snapshot(for configuration: TimetableSelectionIntent, in context: Context) async -> TimetableWidgetEntry {
        makeEntry(configuration: configuration, date: Date())
    }

    func timeline(for configuration: TimetableSelectionIntent, in context: Context) async -> Timeline<TimetableWidgetEntry> {
        let now = Date()
        guard case .valid(let catalog) = WidgetSharedData.catalogState(),
              let record = WidgetPresentation.record(in: catalog, configuredID: configuration.timetable?.id),
              let timetable = try? BackupFormat.shared.decode(json: record.payload) else {
            return Timeline(entries: [makeEntry(configuration: configuration, date: now)],
                policy: .after(now.addingTimeInterval(900)))
        }
        let dates = WidgetPresentation.timelineDates(timetable: timetable, now: now)
        let entries = dates.map { makeEntry(timetable: timetable, date: $0) }
        return Timeline(entries: entries, policy: .atEnd)
    }

    private func makeEntry(configuration: TimetableSelectionIntent, date: Date) -> TimetableWidgetEntry {
        let state = WidgetSharedData.catalogState()
        guard case .valid(let catalog) = state else {
            return TimetableWidgetEntry(
                date: date,
                timetableName: AppLocalization.string("widget.data"),
                snapshot: nil,
                statusMessage: statusMessage(for: state),
                timetableID: configuration.timetable?.id
            )
        }

        guard !catalog.timetables.isEmpty else {
            return TimetableWidgetEntry(
                date: date,
                timetableName: AppLocalization.string("widget.no_timetable"),
                snapshot: nil,
                statusMessage: AppLocalization.string("widget.no_timetable.create"),
                timetableID: configuration.timetable?.id
            )
        }
        guard let record = WidgetPresentation.record(in: catalog, configuredID: configuration.timetable?.id) else {
            return TimetableWidgetEntry(
                date: date,
                timetableName: AppLocalization.string("widget.no_timetable"),
                snapshot: nil,
                statusMessage: AppLocalization.string("widget.no_timetable.found"),
                timetableID: configuration.timetable?.id
            )
        }
        guard let timetable = try? BackupFormat.shared.decode(json: record.payload) else {
            return TimetableWidgetEntry(
                date: date,
                timetableName: AppLocalization.string("widget.data"),
                snapshot: nil,
                statusMessage: AppLocalization.string("widget.data_corrupted"),
                timetableID: record.id
            )
        }

        return makeEntry(timetable: timetable, date: date)
    }

    private func makeEntry(timetable: Timetable, date: Date) -> TimetableWidgetEntry {
        let now = WidgetDateSupport.nowComponents(for: date)
        let snapshot = WidgetPresentation.snapshot(
            timetable: timetable, kind: snapshotKind,
            nowEpochDay: now.epochDay, nowMinuteOfDay: now.minuteOfDay, includesTomorrow: includesTomorrow
        )
        return TimetableWidgetEntry(
            date: date,
            timetableName: timetable.name,
            snapshot: snapshot,
            statusMessage: nil,
            timetableID: timetable.id
        )
    }

    private func statusMessage(for state: WidgetSharedData.CatalogState) -> String {
        switch state {
        case .unconfigured:
            return AppLocalization.string("widget.app_group_unavailable")
        case .missing:
            return AppLocalization.string("widget.missing")
        case .valid:
            return AppLocalization.string("widget.generic_error")
        case .corrupted:
            return AppLocalization.string("widget.catalog_corrupted")
        }
    }
}

@available(iOS 17.0, *)
struct TimetableWidgetView: View {
    let entry: TimetableWidgetEntry
    let title: String
    let emptyMessage: String
    var includesTomorrow = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(verbatim: title)
                    .font(.headline)
                Spacer(minLength: 4)
                Text(verbatim: entry.timetableName)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            if let snapshot = entry.snapshot {
                SnapshotContent(snapshot: snapshot, emptyMessage: emptyMessage, includesTomorrow: includesTomorrow)
            } else {
                Text(verbatim: entry.statusMessage ?? emptyMessage)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .containerBackground(.background, for: .widget)
        .widgetURL(WidgetDeepLink.weekURL(timetableID: entry.timetableID,
            epochDay: entry.snapshot?.items.first?.epochDay ?? entry.snapshot?.anchorEpochDay))
    }
}

@available(iOS 17.0, *)
private struct SnapshotContent: View {
    let snapshot: WidgetSnapshot

    let emptyMessage: String
    let includesTomorrow: Bool
    @Environment(\.widgetFamily) private var family
    @ScaledMetric(relativeTo: .subheadline) private var rowHeight: CGFloat = 42
    @ScaledMetric(relativeTo: .caption) private var footerHeight: CGFloat = 24

    var body: some View {
        if includesTomorrow {
            HStack(alignment: .top, spacing: 12) {
                ForEach([snapshot.anchorEpochDay, snapshot.anchorEpochDay + 1], id: \.self) { day in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(verbatim: WidgetDateSupport.formatDate(day)).font(.caption.weight(.semibold))
                        courseList(snapshot.items.filter { $0.epochDay == day },
                            empty: AppLocalization.string("widget.empty.courses"))
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        } else {
            courseList(snapshot.items, empty: emptyMessage)
        }
    }

    @ViewBuilder
    private func courseList(_ items: [WidgetSnapshotItem], empty: String) -> some View {
        if items.isEmpty {
            Text(verbatim: empty).font(.caption).foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        } else {
            GeometryReader { geometry in
                let limit = WidgetPresentation.visibleItemLimit(height: geometry.size.height, rowHeight: rowHeight, footerHeight: footerHeight)
                let visible = Array(items.prefix(limit))
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(visible, id: \.occurrenceId) { item in
                        if family == .systemSmall {
                            courseRow(item)
                        } else {
                            Link(destination: WidgetDeepLink.weekURL(timetableID: snapshot.timetableId, epochDay: item.epochDay)) {
                                courseRow(item)
                            }.buttonStyle(.plain)
                        }
                    }
                    if items.count > limit {
                        let more = Text(verbatim: AppLocalization.string("widget.more", String(items.count - limit)))
                            .font(.caption.weight(.medium)).foregroundStyle(.tint).lineLimit(1)
                        if family == .systemSmall {
                            more
                        } else {
                            Link(destination: WidgetDeepLink.weekURL(timetableID: snapshot.timetableId, epochDay: items[limit].epochDay)) {
                                more
                            }
                        }
                    }
                }
            }
        }
    }

    private func courseRow(_ item: WidgetSnapshotItem) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Circle().fill(Color(uiColor: UIColor(argb: item.color))).frame(width: 7, height: 7)
            VStack(alignment: .leading, spacing: 2) {
                Text(item.courseName).font(.subheadline.weight(item.isCurrent ? .bold : .regular)).lineLimit(1)
                Text(verbatim: detail(for: item)).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .frame(height: rowHeight, alignment: .top)
        .accessibilityElement(children: .combine)
    }

    private func detail(for item: WidgetSnapshotItem) -> String {
        var parts = [String]()
        if snapshot.kind != .today {
            parts.append(WidgetDateSupport.formatDate(item.epochDay))
        }
        parts.append(AppLocalization.string(
            "time_range",
            WidgetDateSupport.formatTime(item.startMinuteOfDay),
            WidgetDateSupport.formatTime(item.endMinuteOfDay)
        ))
        if item.isCurrent { parts.append(AppLocalization.string("widget.current")) }
        if !item.room.isEmpty { parts.append(item.room) }
        if !item.teacher.isEmpty { parts.append(item.teacher) }
        if item.conflictCount > 1 {
            parts.append(AppLocalization.string("widget.conflict", String(item.conflictCount)))
        }
        if item.isPreferred {
            parts.append(AppLocalization.string("widget.selected"))
        }
        return parts.joined(separator: " · ")
    }
}

private extension UIColor {
    convenience init(argb: Int32) {
        let value = UInt32(bitPattern: argb)
        self.init(
            red: CGFloat((value >> 16) & 0xff) / 255,
            green: CGFloat((value >> 8) & 0xff) / 255,
            blue: CGFloat(value & 0xff) / 255,
            alpha: CGFloat((value >> 24) & 0xff) / 255
        )
    }
}

@available(iOS 17.0, *)
struct NextTimetableWidget: Widget {
    private let kind = "NextTimetableWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: TimetableSelectionIntent.self,
            provider: TimetableWidgetProvider(snapshotKind: .next)
        ) { entry in
            TimetableWidgetView(
                entry: entry,
                title: AppLocalization.string("widget.next.title"),
                emptyMessage: AppLocalization.string("widget.empty.next")
            )
        }
        .configurationDisplayName("widget.next.title")
        .description("widget.next.description")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@available(iOS 17.0, *)
struct TodayTimetableWidget: Widget {
    private let kind = "TodayTimetableWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: TimetableSelectionIntent.self,
            provider: TimetableWidgetProvider(snapshotKind: .today)
        ) { entry in
            TimetableWidgetView(
                entry: entry,
                title: AppLocalization.string("widget.today.title"),
                emptyMessage: AppLocalization.string("widget.empty.today")
            )
        }
        .configurationDisplayName("widget.today.title")
        .description("widget.today.description")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

@available(iOS 17.0, *)
struct WeekTimetableWidget: Widget {
    private let kind = "WeekTimetableWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: TimetableSelectionIntent.self,
            provider: TimetableWidgetProvider(snapshotKind: .week)
        ) { entry in
            TimetableWidgetView(
                entry: entry,
                title: AppLocalization.string("widget.week.title"),
                emptyMessage: AppLocalization.string("widget.empty.week")
            )
        }
        .configurationDisplayName("widget.week.title")
        .description("widget.week.description")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

@available(iOS 17.0, *)
struct TwoDayTimetableWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "TwoDayTimetableWidget", intent: TimetableSelectionIntent.self,
            provider: TimetableWidgetProvider(snapshotKind: .today, includesTomorrow: true)) { entry in
            TimetableWidgetView(entry: entry, title: AppLocalization.string("widget.two_day.title"),
                emptyMessage: AppLocalization.string("widget.empty.courses"), includesTomorrow: true)
        }
        .configurationDisplayName("widget.two_day.title")
        .description("widget.two_day.description")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

@main
@available(iOS 17.0, *)
struct SleepDownWidgetBundle: WidgetBundle {
    var body: some Widget {
        NextTimetableWidget()
        TodayTimetableWidget()
        TwoDayTimetableWidget()
        WeekTimetableWidget()
    }
}
