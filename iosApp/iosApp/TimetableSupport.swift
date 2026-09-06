import Foundation
import shared

struct TimeTableNodeValue: Codable, Equatable, Identifiable {
    var id: Int32 { node }
    var node: Int32
    var startMinuteOfDay: Int32
    var endMinuteOfDay: Int32

    var domain: TimeTableNode {
        TimeTableNode(
            node: node,
            startMinuteOfDay: startMinuteOfDay,
            endMinuteOfDay: endMinuteOfDay
        )
    }
}

struct ReusableTimeTable: Codable, Equatable, Identifiable {
    var id: String
    var name: String
    var nodes: [TimeTableNodeValue]

    var domain: ScheduleTimeTable {
        ScheduleTimeTable(
            id: id,
            name: name,
            nodes: nodes.map(\.domain)
        )
    }

    static let defaultID = "default-schedule"

    static func defaultDefinition() -> ReusableTimeTable {
        let startTimes: [Int32] = [480, 535, 590, 645, 720, 775, 830, 885, 960, 1015, 1070, 1125]
        let nodes = startTimes.enumerated().map { index, start in
            TimeTableNodeValue(
                node: Int32(index + 1),
                startMinuteOfDay: start,
                endMinuteOfDay: start + 45
            )
        }
        return ReusableTimeTable(
            id: defaultID,
            name: AppLocalization.string("default.schedule.name"),
            nodes: nodes
        )
    }
}

enum TimetableDates {
    static var utcCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        calendar.locale = Locale(identifier: "en_US_POSIX")
        return calendar
    }()

    static func epochDay(for date: Date) -> Int64 {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        let day = utcCalendar.date(from: components) ?? date
        return Int64(floor(day.timeIntervalSince1970 / 86_400))
    }

    static func utcDayAndMinute(for date: Date) -> (epochDay: Int64, minuteOfDay: Int32) {
        let components = utcCalendar.dateComponents([.hour, .minute], from: date)
        return (
            epochDay: Int64(floor(date.timeIntervalSince1970 / 86_400)),
            minuteOfDay: Int32((components.hour ?? 0) * 60 + (components.minute ?? 0))
        )
    }

    static func civilDateComponents(forEpochDay epochDay: Int64) -> DateComponents {
        let utcDate = Date(timeIntervalSince1970: TimeInterval(epochDay) * 86_400)
        return utcCalendar.dateComponents([.year, .month, .day], from: utcDate)
    }

    static func localDateComponents(forEpochDay epochDay: Int64, minuteOfDay: Int32) -> DateComponents {
        let totalMinutes = Int64(minuteOfDay)
        let dayOffset = totalMinutes >= 0 ? totalMinutes / 1_440 : (totalMinutes - 1_439) / 1_440
        let normalizedEpochDay = epochDay + dayOffset
        let normalizedMinute = totalMinutes - dayOffset * 1_440
        let civil = civilDateComponents(forEpochDay: normalizedEpochDay)
        var components = DateComponents()
        components.calendar = Calendar.current
        components.timeZone = TimeZone.current
        components.year = civil.year
        components.month = civil.month
        components.day = civil.day
        components.hour = Int(normalizedMinute / 60)
        components.minute = Int(normalizedMinute % 60)
        return components
    }

    static func date(forEpochDay epochDay: Int64, minuteOfDay: Int32 = 720) -> Date {
        let components = localDateComponents(forEpochDay: epochDay, minuteOfDay: minuteOfDay)
        return Calendar.current.date(from: components)
            ?? Date(timeIntervalSince1970: TimeInterval(epochDay) * 86_400 + TimeInterval(minuteOfDay) * 60)
    }

    static func mondayEpochDay(containing date: Date = Date()) -> Int64 {
        let epoch = epochDay(for: date)
        let weekday = Int((epoch + 3).positiveModulo(7)) + 1
        return epoch - Int64(weekday - 1)
    }

    static func dayOfWeek(forEpochDay epochDay: Int64, firstDayEpochDay: Int64) -> Int32 {
        Int32((epochDay - firstDayEpochDay).positiveModulo(7) + 1)
    }

    static func week(forEpochDay epochDay: Int64, firstDayEpochDay: Int64) -> Int32 {
        Int32((epochDay - firstDayEpochDay).floorDiv(7) + 1)
    }

    static func displayDate(forEpochDay epochDay: Int64) -> Date {
        date(forEpochDay: epochDay, minuteOfDay: 720)
    }

    static func range(for timetable: Timetable, week: Int32) -> EpochDayRange {
        let start = timetable.firstDayEpochDay + Int64(max(week - 1, 0)) * 7
        return EpochDayRange(startEpochDay: start, endEpochDay: start + 6)
    }
}

private extension Int64 {
    func positiveModulo(_ divisor: Int64) -> Int64 {
        let remainder = self % divisor
        return remainder >= 0 ? remainder : remainder + divisor
    }

    func floorDiv(_ divisor: Int64) -> Int64 {
        let quotient = self / divisor
        let remainder = self % divisor
        return remainder >= 0 ? quotient : quotient - 1
    }
}

extension Timetable {
    func replacing(
        name: String? = nil,
        firstDayEpochDay: Int64? = nil,
        maxWeek: Int32? = nil,
        timeTable: ScheduleTimeTable? = nil,
        courses: [Course]? = nil,
        dateExceptions: [DateException]? = nil,
        conflictPreferences: [ConflictPreference]? = nil,
        reminderSettings: ReminderSettings? = nil,
        sortOrder: Int32? = nil,
        showSaturday: Bool? = nil,
        showSunday: Bool? = nil,
        sundayFirst: Bool? = nil
    ) -> Timetable {
        Timetable(
            id: id,
            name: name ?? self.name,
            firstDayEpochDay: firstDayEpochDay ?? self.firstDayEpochDay,
            maxWeek: maxWeek ?? self.maxWeek,
            timeTable: timeTable ?? self.timeTable,
            courses: courses ?? self.courses,
            dateExceptions: dateExceptions ?? self.dateExceptions,
            conflictPreferences: conflictPreferences ?? self.conflictPreferences,
            reminderSettings: reminderSettings ?? self.reminderSettings,
            sortOrder: sortOrder ?? self.sortOrder,
            showSaturday: showSaturday ?? self.showSaturday,
            showSunday: showSunday ?? self.showSunday,
            sundayFirst: sundayFirst ?? self.sundayFirst
        )
    }
}

extension Course {
    func replacing(
        name: String? = nil,
        color: Int32? = nil,
        note: String? = nil,
        credit: Float? = nil,
        slots: [LogicalCourseSlot]? = nil
    ) -> Course {
        Course(
            id: id,
            name: name ?? self.name,
            color: color ?? self.color,
            note: note ?? self.note,
            credit: credit ?? self.credit,
            slots: slots ?? self.slots
        )
    }
}

extension RecurrenceSegment {
    func replacing(
        dayOfWeek: KotlinInt?,
        startNode: KotlinInt?,
        nodeCount: KotlinInt?,
        teacher: String?,
        room: String?,
        customTime: MinuteRange?
    ) -> RecurrenceSegment {
        RecurrenceSegment(
            id: id,
            startWeek: startWeek,
            endWeek: endWeek,
            weekPattern: weekPattern,
            dayOfWeek: dayOfWeek,
            startNode: startNode,
            nodeCount: nodeCount,
            teacher: teacher,
            room: room,
            customTime: customTime
        )
    }
}

extension LogicalCourseSlot {
    func replacing(
        dayOfWeek: Int32? = nil,
        startNode: Int32? = nil,
        nodeCount: Int32? = nil,
        teacher: String? = nil,
        room: String? = nil,
        customTime: MinuteRange? = nil,
        recurrenceSegments: [RecurrenceSegment]? = nil
    ) -> LogicalCourseSlot {
        LogicalCourseSlot(
            id: id,
            courseId: courseId,
            dayOfWeek: dayOfWeek ?? self.dayOfWeek,
            startNode: startNode ?? self.startNode,
            nodeCount: nodeCount ?? self.nodeCount,
            teacher: teacher ?? self.teacher,
            room: room ?? self.room,
            customTime: customTime ?? self.customTime,
            recurrenceSegments: recurrenceSegments ?? self.recurrenceSegments
        )
    }

    func replacingAndSyncingRecurrenceOverrides(
        dayOfWeek: Int32,
        startNode: Int32,
        nodeCount: Int32,
        teacher: String,
        room: String,
        customTime: MinuteRange?
    ) -> LogicalCourseSlot {
        let updatedSegments = recurrenceSegments.map { segment in
            segment.replacing(
                dayOfWeek: segment.dayOfWeek?.int32Value == self.dayOfWeek ? nil : segment.dayOfWeek,
                startNode: segment.startNode?.int32Value == self.startNode ? nil : segment.startNode,
                nodeCount: segment.nodeCount?.int32Value == self.nodeCount ? nil : segment.nodeCount,
                teacher: segment.teacher == self.teacher ? nil : segment.teacher,
                room: segment.room == self.room ? nil : segment.room,
                customTime: sameMinuteRange(segment.customTime, self.customTime) ? nil : segment.customTime
            )
        }
        return LogicalCourseSlot(
            id: id,
            courseId: courseId,
            dayOfWeek: dayOfWeek,
            startNode: startNode,
            nodeCount: nodeCount,
            teacher: teacher,
            room: room,
            customTime: customTime,
            recurrenceSegments: updatedSegments
        )
    }
}

private func sameMinuteRange(_ lhs: MinuteRange?, _ rhs: MinuteRange?) -> Bool {
    switch (lhs, rhs) {
    case (nil, nil): return true
    case let (left?, right?):
        return left.startMinuteOfDay == right.startMinuteOfDay &&
            left.endMinuteOfDay == right.endMinuteOfDay
    default: return false
    }
}

extension ReminderSettings {
    static var standard: ReminderSettings {
        ReminderSettings(
            startEnabled: false,
            endEnabled: false,
            startLeadMinutes: 10,
            endLeadMinutes: 5,
            content: ReminderContentSettings(
                includeCourseName: true,
                includeTeacher: true,
                includeRoom: true,
                includeNote: false
            ),
            vibrate: true,
            silent: false
        )
    }
}

extension Timetable {
    static func blank(
        name: String = AppLocalization.string("default.timetable.name"),
        schedule: ReusableTimeTable = .defaultDefinition(),
        id: String = UUID().uuidString,
        firstDayEpochDay: Int64 = TimetableDates.mondayEpochDay()
    ) -> Timetable {
        Timetable(
            id: id,
            name: name,
            firstDayEpochDay: firstDayEpochDay,
            maxWeek: 20,
            timeTable: schedule.domain,
            courses: [],
            dateExceptions: [],
            conflictPreferences: [],
            reminderSettings: .standard,
            sortOrder: 0,
            showSaturday: true,
            showSunday: true,
            sundayFirst: false
        )
    }
}

extension Int32 {
    var asKotlinInt: KotlinInt { KotlinInt(value: self) }
}

extension Int64 {
    var asKotlinLong: KotlinLong { KotlinLong(value: self) }
}

extension String {
    var trimmedOrNil: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }
}

enum SharedInterop {
    static func encodeBackup(_ timetable: Timetable) throws -> String {
        try BackupFormat.shared.encode(timetable: timetable)
    }

    static func decodeBackup(_ json: String) throws -> Timetable {
        try BackupFormat.shared.decode(json: json)
    }

    static func decodeBackupDocument(_ json: String) throws -> Timetable {
        try BackupFormat.shared.decodeDocument(json: json).timetable.toDomain()
    }

    static func apply(_ timetable: Timetable, command: TimetableCommand) throws -> Timetable {
        try TimetableCommands.shared.apply(timetable: timetable, command: command)
    }

    static func applyImport(
        current: Timetable?,
        imported: Timetable,
        mode: BackupImportMode
    ) throws -> BackupImportResult {
        try BackupImportPolicy.shared.apply(current: current, imported: imported, mode: mode)
    }

    static func exportICS(
        _ timetable: Timetable,
        range: EpochDayRange,
        options: IcsExportOptions
    ) throws -> String {
        try IcsCodec.shared.export(timetable: timetable, range: range, options: options)
    }

    static func importICS(
        _ text: String,
        timetableId: String,
        name: String,
        timeTable: ScheduleTimeTable
    ) throws -> Timetable {
        try IcsCodec.shared.toTimetable(
            text: text,
            timetableId: timetableId,
            name: name,
            firstDayEpochDay: nil,
            timeTable: timeTable
        )
    }

    static func importWakeUp(_ text: String, timetableId: String) throws -> Timetable {
        try WakeUpBackupImporter.shared.importToTimetable(text: text, timetableId: timetableId)
    }
}

struct IOSCourseMovePlan {
    let original: Timetable
    let occurrence: CourseOccurrence
    let targetEpochDay: Int64
    let targetStartNode: Int32
    let targetCustomTime: MinuteRange?
    let preview: Timetable

    init(timetable: Timetable, occurrence: CourseOccurrence, targetEpochDay: Int64, targetStartNode: Int32) throws {
        guard let sourceNode = timetable.timeTable.nodes.first(where: { $0.node == occurrence.startNode }),
              let targetNode = timetable.timeTable.nodes.first(where: { $0.node == targetStartNode }),
              timetable.timeTable.rangeForNodes(startNode: targetStartNode, nodeCount: occurrence.nodeCount) != nil else {
            throw IOSCourseEditingError.invalidSchedule
        }
        let delta = targetNode.startMinuteOfDay - sourceNode.startMinuteOfDay
        let custom = occurrence.usesCustomTime ? MinuteRange(startMinuteOfDay: occurrence.startMinuteOfDay + delta, endMinuteOfDay: occurrence.endMinuteOfDay + delta) : nil
        guard custom?.isValid != false else { throw IOSCourseEditingError.invalidSchedule }
        original = timetable
        self.occurrence = occurrence
        self.targetEpochDay = targetEpochDay
        self.targetStartNode = targetStartNode
        targetCustomTime = custom
        preview = try SharedInterop.apply(timetable, command: Self.single(occurrence, originalDay: occurrence.sourceEpochDay, targetDay: targetEpochDay, start: targetStartNode, custom: custom, timetable: timetable))
    }

    func result(allWeeks: Bool) throws -> Timetable {
        if !allWeeks { return preview }
        let sources = TimetableEngine.shared.recurringOccurrences(timetable: original).filter {
            $0.logicalSlotId == occurrence.logicalSlotId && $0.recurrenceSegmentId == occurrence.recurrenceSegmentId && $0.sourceEpochDay == occurrence.sourceEpochDay
        }
        guard sources.count == 1, let source = sources.first,
              let sourceNode = original.timeTable.nodes.first(where: { $0.node == source.startNode }),
              let targetNode = original.timeTable.nodes.first(where: { $0.node == targetStartNode }) else { throw IOSCourseEditingError.invalidSchedule }
        let targetDay = TimetableDates.dayOfWeek(forEpochDay: targetEpochDay, firstDayEpochDay: original.firstDayEpochDay)
        let command = TimetableCommandMoveLogicalSlot(
            logicalSlotId: occurrence.logicalSlotId,
            dayDelta: targetDay - source.dayOfWeek,
            startNodeDelta: targetStartNode - source.startNode,
            minuteDelta: targetCustomTime.map { $0.startMinuteOfDay - source.startMinuteOfDay } ?? (targetNode.startMinuteOfDay - sourceNode.startMinuteOfDay),
            targetDayOfWeek: nil, targetStartNode: nil, targetNodeCount: nil, targetCustomTime: nil
        )
        let moved = try SharedInterop.apply(original, command: command)
        if !occurrence.isRescheduled { return moved }
        let movedSources = TimetableEngine.shared.recurringOccurrences(timetable: moved).filter {
            $0.logicalSlotId == source.logicalSlotId && $0.recurrenceSegmentId == source.recurrenceSegmentId && $0.week == source.week
        }
        guard movedSources.count == 1, let movedSource = movedSources.first else { throw IOSCourseEditingError.invalidSchedule }
        return try SharedInterop.apply(moved, command: Self.single(occurrence, originalDay: movedSource.sourceEpochDay, targetDay: targetEpochDay, start: targetStartNode, custom: targetCustomTime, timetable: moved))
    }

    private static func single(_ item: CourseOccurrence, originalDay: Int64, targetDay: Int64, start: Int32, custom: MinuteRange?, timetable: Timetable) -> TimetableCommand {
        TimetableCommandRescheduleOccurrence(
            logicalSlotId: item.logicalSlotId, originalEpochDay: originalDay, targetEpochDay: targetDay,
            recurrenceSegmentId: item.recurrenceSegmentId,
            targetDayOfWeek: TimetableDates.dayOfWeek(forEpochDay: targetDay, firstDayEpochDay: timetable.firstDayEpochDay).asKotlinInt,
            targetStartNode: start.asKotlinInt, targetNodeCount: item.nodeCount.asKotlinInt,
            targetCustomTime: custom, targetTeacher: item.teacher, targetRoom: item.room
        )
    }
}

enum IOSCourseEditingError: LocalizedError {
    case invalidSchedule
    var errorDescription: String? { AppLocalization.string("error.schedule_range") }
}

enum IOSGridGeometry {
    static func span(_ item: CourseOccurrence, nodes: [TimeTableNode]) -> (top: Double, height: Double) {
        if !item.usesCustomTime { return (Double(max(item.startNode - 1, 0)), Double(max(item.nodeCount, 1))) }
        let start = position(item.startMinuteOfDay, nodes: nodes)
        let end = position(item.endMinuteOfDay, nodes: nodes)
        return (start, max(end - start, 0.35))
    }

    static func position(_ minute: Int32, nodes: [TimeTableNode]) -> Double {
        for (index, node) in nodes.enumerated() {
            if minute < node.startMinuteOfDay { return Double(index) }
            if minute <= node.endMinuteOfDay {
                return Double(index) + Double(minute - node.startMinuteOfDay) / Double(max(node.endMinuteOfDay - node.startMinuteOfDay, 1))
            }
        }
        return Double(nodes.count)
    }
}

struct IOSRecurrenceDraft: Identifiable {
    let original: RecurrenceSegment
    var weeks: Set<Int>
    var id: String { original.id }

    init(_ segment: RecurrenceSegment, maxWeek: Int) {
        original = segment
        weeks = Self.weeks(segment, maxWeek: maxWeek)
    }

    static func weeks(_ segment: RecurrenceSegment, maxWeek: Int) -> Set<Int> {
        let start = max(Int(segment.startWeek), 1)
        let end = min(Int(segment.endWeek), maxWeek)
        guard start <= end else { return [] }
        return Set((start...end).filter { week in
            segment.weekPattern == .all || (segment.weekPattern == .odd && week % 2 == 1) || (segment.weekPattern == .even && week % 2 == 0)
        })
    }

    func segments(maxWeek: Int) -> [RecurrenceSegment] {
        if weeks == Self.weeks(original, maxWeek: maxWeek) { return [original] }
        var runs: [[Int]] = []
        for week in weeks.sorted() {
            if let last = runs.last?.last, week == last + 1 { runs[runs.count - 1].append(week) }
            else { runs.append([week]) }
        }
        return runs.enumerated().map { index, run in
            RecurrenceSegment(id: index == 0 ? original.id : original.id + ":" + String(run[0]), startWeek: Int32(run[0]), endWeek: Int32(run[run.count - 1]), weekPattern: .all,
                dayOfWeek: original.dayOfWeek, startNode: original.startNode, nodeCount: original.nodeCount,
                teacher: original.teacher, room: original.room, customTime: original.customTime)
        }
    }
}

struct IOSSlotDraft: Identifiable {
    let original: LogicalCourseSlot
    var day: Int
    var start: Int
    var count: Int
    var teacher: String
    var room: String
    var custom: Bool
    var customStart: String
    var customEnd: String
    var recurrences: [IOSRecurrenceDraft]
    var id: String { original.id }

    init(_ slot: LogicalCourseSlot, maxWeek: Int) {
        original = slot
        day = Int(slot.dayOfWeek); start = Int(slot.startNode); count = Int(slot.nodeCount)
        teacher = slot.teacher; room = slot.room; custom = slot.customTime != nil
        customStart = slot.customTime.map { MinuteOfDay.shared.format(minuteOfDay: $0.startMinuteOfDay) } ?? "08:00"
        customEnd = slot.customTime.map { MinuteOfDay.shared.format(minuteOfDay: $0.endMinuteOfDay) } ?? "08:50"
        recurrences = slot.recurrenceSegments.map { IOSRecurrenceDraft($0, maxWeek: maxWeek) }
    }

    func slot(schedule: ScheduleTimeTable, maxWeek: Int) throws -> LogicalCourseSlot {
        guard (1...7).contains(day), schedule.rangeForNodes(startNode: Int32(start), nodeCount: Int32(count)) != nil,
              !recurrences.isEmpty, recurrences.allSatisfy({ !$0.weeks.isEmpty }) else { throw IOSCourseEditingError.invalidSchedule }
        var range: MinuteRange?
        if custom {
            guard let from = MinuteOfDay.shared.parse(value: customStart), let to = MinuteOfDay.shared.parse(value: customEnd) else { throw IOSCourseEditingError.invalidSchedule }
            range = MinuteRange(startMinuteOfDay: from.int32Value, endMinuteOfDay: to.int32Value)
            guard range?.isValid == true else { throw IOSCourseEditingError.invalidSchedule }
        }
        let updated = original.replacingAndSyncingRecurrenceOverrides(dayOfWeek: Int32(day), startNode: Int32(start), nodeCount: Int32(count), teacher: teacher, room: room, customTime: range)
        let segments = recurrences.flatMap { draft -> [RecurrenceSegment] in
            let synced = updated.recurrenceSegments.first { $0.id == draft.id } ?? draft.original
            var result = IOSRecurrenceDraft(synced, maxWeek: maxWeek)
            result.weeks = draft.weeks
            return result.segments(maxWeek: maxWeek)
        }
        return LogicalCourseSlot(id: id, courseId: original.courseId, dayOfWeek: Int32(day), startNode: Int32(start), nodeCount: Int32(count), teacher: teacher, room: room, customTime: range, recurrenceSegments: segments)
    }
}

enum IOSCourseEditing {
    static func replacingCourse(_ course: Course, in timetable: Timetable) throws -> Timetable {
        guard let previous = timetable.courses.first(where: { $0.id == course.id }), !course.slots.isEmpty else { throw IOSCourseEditingError.invalidSchedule }
        let updated = timetable.replacing(courses: timetable.courses.map { $0.id == course.id ? course : $0 })
        let oldSources = TimetableEngine.shared.recurringOccurrences(timetable: timetable)
        let newSources = TimetableEngine.shared.recurringOccurrences(timetable: updated)
        let slotIDs = Set(previous.slots.map(\.id))
        let exceptions: [DateException] = try timetable.dateExceptions.compactMap { exception in
            guard slotIDs.contains(exception.logicalSlotId) else { return exception }
            let originals = oldSources.filter {
                $0.logicalSlotId == exception.logicalSlotId && $0.sourceEpochDay == exception.originalEpochDay &&
                (exception.recurrenceSegmentId == nil || $0.recurrenceSegmentId == exception.recurrenceSegmentId)
            }
            guard originals.count == 1, let original = originals.first else { throw IOSCourseEditingError.invalidSchedule }
            let replacements = newSources.filter {
                $0.logicalSlotId == original.logicalSlotId && $0.week == original.week &&
                ($0.recurrenceSegmentId == original.recurrenceSegmentId || $0.recurrenceSegmentId.hasPrefix(original.recurrenceSegmentId + ":"))
            }
            // Removing an applicable week or time block also removes its one-date override.
            guard !replacements.isEmpty else { return nil }
            guard replacements.count == 1, let source = replacements.first else { throw IOSCourseEditingError.invalidSchedule }
            return DateException(id: exception.id, logicalSlotId: exception.logicalSlotId, originalEpochDay: source.sourceEpochDay, type: exception.type,
                recurrenceSegmentId: source.recurrenceSegmentId, targetEpochDay: exception.targetEpochDay,
                targetDayOfWeek: exception.targetDayOfWeek, targetStartNode: exception.targetStartNode, targetNodeCount: exception.targetNodeCount,
                targetCustomTime: exception.targetCustomTime, targetTeacher: exception.targetTeacher, targetRoom: exception.targetRoom)
        }
        return updated.replacing(dateExceptions: exceptions)
    }
}

extension ReusableTimeTable {
    func applyingBreak(minutes: Int, first: Int, last: Int) throws -> ReusableTimeTable {
        guard (0...240).contains(minutes), first >= 1, last <= nodes.count, first < last else { throw IOSCourseEditingError.invalidSchedule }
        var result = nodes
        for index in first..<last {
            let duration = nodes[index].endMinuteOfDay - nodes[index].startMinuteOfDay
            result[index].startMinuteOfDay = result[index - 1].endMinuteOfDay + Int32(minutes)
            result[index].endMinuteOfDay = result[index].startMinuteOfDay + duration
        }
        guard result.allSatisfy({ $0.startMinuteOfDay >= 0 && $0.endMinuteOfDay <= 1440 && $0.endMinuteOfDay > $0.startMinuteOfDay }),
              zip(result, result.dropFirst()).allSatisfy({ $0.endMinuteOfDay <= $1.startMinuteOfDay }) else { throw IOSCourseEditingError.invalidSchedule }
        return ReusableTimeTable(id: id, name: name, nodes: result)
    }
}
