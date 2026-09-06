import Foundation
import SwiftUI
import UniformTypeIdentifiers
import shared

extension UTType {
    static let sleepDownBackup = UTType(exportedAs: "com.letr.sleepdown.backup")
    static var sleepDownICS: UTType { UTType(filenameExtension: "ics") ?? .data }
}

struct TimetableFileDocument: FileDocument {
    static var readableContentTypes: [UTType] = [
        .json,
        .sleepDownICS,
        .commaSeparatedText,
        .data
    ]

    let data: Data

    init(data: Data = Data()) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

enum TimetableDocumentError: LocalizedError {
    case unreadableFile
    case unsupportedFormat
    case emptyCSV
    case invalidCSV(String)

    var errorDescription: String? {
        switch self {
        case .unreadableFile:
            return AppLocalization.string("document.file.unreadable")
        case .unsupportedFormat:
            return AppLocalization.string("document.file.unsupported")
        case .emptyCSV:
            return AppLocalization.string("document.csv.empty")
        case .invalidCSV(let message):
            return AppLocalization.string("document.csv.invalid", message)
        }
    }
}

enum TimetableDocumentCodec {
    static func encodeJSON(_ timetable: Timetable) throws -> Data {
        try SharedInterop.encodeBackup(timetable).data(using: .utf8) ?? Data()
    }

    static func encodeICS(
        _ timetable: Timetable,
        range: EpochDayRange,
        now: Date = Date()
    ) throws -> Data {
        let timestamp = TimetableDates.utcDayAndMinute(for: now)
        let options = IcsExportOptions(
            dtStampEpochDay: timestamp.epochDay,
            dtStampMinuteOfDay: timestamp.minuteOfDay
        )
        let text = try SharedInterop.exportICS(
            timetable,
            range: range,
            options: options
        )
        return text.data(using: .utf8) ?? Data()
    }

    static func decode(
        data: Data,
        filename: String,
        defaultTimeTable: ReusableTimeTable
    ) throws -> Timetable {
        guard let text = String(data: data, encoding: .utf8) else {
            throw TimetableDocumentError.unreadableFile
        }

        let fileExtension = URL(fileURLWithPath: filename).pathExtension.lowercased()
        if fileExtension == "csv" || fileExtension == "tsv" {
            return try CSVTimetableImporter.importText(text, schedule: defaultTimeTable)
        }
        if fileExtension == "ics" || fileExtension == "ical" {
            return try SharedInterop.importICS(
                text,
                timetableId: UUID().uuidString,
                name: URL(fileURLWithPath: filename).deletingPathExtension().lastPathComponent,
                timeTable: defaultTimeTable.domain
            )
        }

        let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        if object?.keys.contains("courseBases") == true || object?.keys.contains("courseDetails") == true {
            return try SharedInterop.importWakeUp(text, timetableId: UUID().uuidString)
        }
        if object?.keys.contains("formatVersion") == true || object?.keys.contains("timetable") == true {
            return try SharedInterop.decodeBackupDocument(text)
        }
        if fileExtension == "wakeup" || fileExtension == "backup" {
            return try SharedInterop.importWakeUp(text, timetableId: UUID().uuidString)
        }
        if object != nil {
            return try SharedInterop.decodeBackup(text)
        }
        throw TimetableDocumentError.unsupportedFormat
    }
}

private enum CSVTimetableImporter {
    static func importText(_ text: String, schedule: ReusableTimeTable) throws -> Timetable {
        let lines = text
            .components(separatedBy: .newlines)
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        guard !lines.isEmpty else { throw TimetableDocumentError.emptyCSV }

        let firstRow = parseRow(lines[0])
        let hasHeader = firstRow.contains { value in
            let normalized = normalize(value)
            return normalized.contains("课程") || normalized == "name" || normalized == "course"
        }
        let headers = hasHeader ? firstRow.map(normalize) : []
        let dataLines = hasHeader ? Array(lines.dropFirst()) : lines
        guard !dataLines.isEmpty else { throw TimetableDocumentError.emptyCSV }

        var courses: [Course] = []
        var maximumWeek: Int32 = 20
        for (rowIndex, line) in dataLines.enumerated() {
            let values = parseRow(line)
            guard !values.allSatisfy({ $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) else { continue }
            let value = FieldReader(values: values, headers: headers)
            let name = value.first([
                "课程名称", "课程名", "课程", "name", "course", "coursename"
            ], fallbackIndex: 0)?.trimmedOrNil
            guard let name else {
                throw TimetableDocumentError.invalidCSV(
                    AppLocalization.string("document.csv.missing_course_name", String(rowIndex + 1))
                )
            }

            let day = parseDay(value.first(["星期", "周几", "day", "weekday"], fallbackIndex: 3) ?? "") ?? 1
            let startNode = parseNumber(value.first([
                "开始节", "开始节次", "起始节", "startnode", "start"
            ], fallbackIndex: 4) ?? "") ?? 1
            let endNode = parseNumber(value.first([
                "结束节", "结束节次", "终止节", "endnode", "end"
            ], fallbackIndex: 5) ?? "")
            let nodeCount = max(1, endNode.map { $0 - startNode + 1 } ?? parseNumber(value.first([
                "节数", "课时", "nodecount", "step"
            ], fallbackIndex: 5) ?? "") ?? 1)
            let startWeek = max(1, parseNumber(value.first([
                "开始周", "起始周", "startweek", "fromweek"
            ], fallbackIndex: 6) ?? "") ?? 1)
            let endWeek = max(startWeek, parseNumber(value.first([
                "结束周", "终止周", "endweek", "toweek"
            ], fallbackIndex: 7) ?? "") ?? 20)
            maximumWeek = max(maximumWeek, endWeek)

            let teacher = value.first(["教师", "老师", "teacher"], fallbackIndex: 1)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let room = value.first(["教室", "地点", "room", "location"], fallbackIndex: 2)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let customTime = parseCustomTime(
                start: value.first(["开始时间", "上课时间", "starttime"], fallbackIndex: 9),
                end: value.first(["结束时间", "下课时间", "endtime"], fallbackIndex: 10)
            )
            let pattern = parsePattern(value.first(["单双周", "周次类型", "weektype", "pattern"], fallbackIndex: 8))
            let courseID = UUID().uuidString
            let slotID = UUID().uuidString
            let segmentID = UUID().uuidString
            let segment = RecurrenceSegment(
                id: segmentID,
                startWeek: startWeek,
                endWeek: endWeek,
                weekPattern: pattern,
                dayOfWeek: nil,
                startNode: nil,
                nodeCount: nil,
                teacher: nil,
                room: nil,
                customTime: nil
            )
            let slot = LogicalCourseSlot(
                id: slotID,
                courseId: courseID,
                dayOfWeek: day,
                startNode: startNode,
                nodeCount: nodeCount,
                teacher: teacher,
                room: room,
                customTime: customTime,
                recurrenceSegments: [segment]
            )
            courses.append(
                Course(
                    id: courseID,
                    name: name,
                    color: color(for: courses.count),
                    note: value.first(["备注", "note"], fallbackIndex: 11)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
                    credit: Float(value.first(["学分", "credit"], fallbackIndex: 12) ?? "") ?? 0,
                    slots: [slot]
                )
            )
        }

        guard !courses.isEmpty else { throw TimetableDocumentError.emptyCSV }
        return Timetable(
            id: UUID().uuidString,
            name: AppLocalization.string("import.default_timetable_name"),
            firstDayEpochDay: TimetableDates.mondayEpochDay(),
            maxWeek: maximumWeek,
            timeTable: schedule.domain,
            courses: courses,
            dateExceptions: [],
            conflictPreferences: [],
            reminderSettings: .standard,
            sortOrder: 0,
            showSaturday: true,
            showSunday: true,
            sundayFirst: false
        )
    }

    private static func parseRow(_ line: String) -> [String] {
        var values: [String] = []
        var current = ""
        var quoted = false
        var index = line.startIndex
        while index < line.endIndex {
            let character = line[index]
            if character == "\"" {
                if quoted && index < line.index(before: line.endIndex) {
                    let next = line[line.index(after: index)]
                    if next == "\"" {
                        current.append("\"")
                        index = line.index(after: index)
                    } else {
                        quoted.toggle()
                    }
                } else {
                    quoted.toggle()
                }
            } else if (character == "," || character == "\t") && !quoted {
                values.append(current)
                current = ""
            } else {
                current.append(character)
            }
            index = line.index(after: index)
        }
        values.append(current)
        return values
    }

    private static func normalize(_ value: String) -> String {
        value
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "_", with: "")
    }

    private static func parseNumber(_ value: String) -> Int32? {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        let digits = trimmed.unicodeScalars.filter { CharacterSet.decimalDigits.contains($0) }
        guard !digits.isEmpty else { return nil }
        return Int32(String(String.UnicodeScalarView(digits)))
    }

    private static func parseDay(_ value: String) -> Int32? {
        if let number = parseNumber(value) { return min(max(number, 1), 7) }
        let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines)
        let names: [String: Int32] = [
            "一": 1, "二": 2, "三": 3, "四": 4, "五": 5, "六": 6, "日": 7, "天": 7,
            "mon": 1, "monday": 1, "tue": 2, "tuesday": 2, "wed": 3, "wednesday": 3,
            "thu": 4, "thursday": 4, "fri": 5, "friday": 5, "sat": 6, "saturday": 6,
            "sun": 7, "sunday": 7
        ]
        for (name, day) in names where normalized.lowercased().contains(name) {
            return day
        }
        return nil
    }

    private static func parsePattern(_ value: String?) -> WeekPattern {
        let normalized = value?.lowercased() ?? ""
        if normalized.contains("单") || normalized.contains("odd") { return .odd }
        if normalized.contains("双") || normalized.contains("even") { return .even }
        return .all
    }

    private static func parseCustomTime(start: String?, end: String?) -> MinuteRange? {
        guard let start, let end,
              let startMinute = MinuteOfDay.shared.parse(value: start),
              let endMinute = MinuteOfDay.shared.parse(value: end) else { return nil }
        let range = MinuteRange(
            startMinuteOfDay: startMinute.int32Value,
            endMinuteOfDay: endMinute.int32Value
        )
        return range.isValid ? range : nil
    }

    private static func color(for index: Int) -> Int32 {
        let colors: [UInt32] = [
            0xFF4F8EF7, 0xFF5DBB63, 0xFFE28743, 0xFF9B6BDF,
            0xFFE05B83, 0xFF31A9A7, 0xFFAA8F39, 0xFF7C8B9A
        ]
        return Int32(bitPattern: colors[index % colors.count])
    }

    private struct FieldReader {
        let values: [String]
        let headers: [String]

        func first(_ names: [String], fallbackIndex: Int? = nil) -> String? {
            if !headers.isEmpty {
                let normalizedNames = Set(names.map { CSVTimetableImporter.normalize($0) })
                if let index = headers.firstIndex(where: { normalizedNames.contains($0) }) {
                    return index < values.count ? values[index] : nil
                }
            }
            if let fallbackIndex, fallbackIndex < values.count {
                return values[fallbackIndex]
            }
            return nil
        }
    }
}
