import CoreData
import XCTest
import shared
@testable import iosApp

@MainActor
final class TimetableStoreTests: XCTestCase {
    func testManagementOrderAndSharedTimeTablePersist() throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("sleepdown-\(UUID().uuidString).sqlite")
        defer { try? FileManager.default.removeItem(at: url) }
        let store = TimetableStore(storeURL: url)
        let first = try XCTUnwrap(store.createTimetable(name: "First"))
        let second = try XCTUnwrap(store.createTimetable(name: "Second"))
        store.moveTimetable(from: IndexSet(integer: 1), to: 0)
        XCTAssertEqual(store.timetables.map(\.id), [second.id, first.id])
        let definition = try XCTUnwrap(store.createDefinition(name: "Custom", nodes: ReusableTimeTable.defaultDefinition().nodes))
        let reorderedFirst = try XCTUnwrap(store.timetables.first(where: { $0.id == first.id }))
        XCTAssertTrue(store.updateTimetable(reorderedFirst.replacing(timeTable: definition.domain)))
        XCTAssertFalse(store.deleteDefinition(definition.id))
        let changed = try definition.applyingBreak(minutes: 15, first: 1, last: 4)
        XCTAssertTrue(store.saveDefinition(changed))
        let reopened = TimetableStore(storeURL: url)
        XCTAssertEqual(reopened.timetables.map(\.id), [second.id, first.id])
        let saved = try XCTUnwrap(reopened.timetables.first(where: { $0.id == first.id }))
        XCTAssertEqual(saved.timeTable.nodes[1].startMinuteOfDay, 540)
        XCTAssertEqual(saved.timeTable.nodes[4].startMinuteOfDay, 720)
        XCTAssertEqual(reopened.timetables.first?.timeTable.id, ReusableTimeTable.defaultID)
    }

    func testTimetablePersistsAndReloadsAggregate() throws {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("sleepdown-\(UUID().uuidString).sqlite")
        defer { try? FileManager.default.removeItem(at: url) }

        let firstStore = TimetableStore(storeURL: url)
        let timetable = try XCTUnwrap(firstStore.createTimetable(name: "测试课表"))
        let courseID = UUID().uuidString
        let slotID = UUID().uuidString
        let segment = RecurrenceSegment(
            id: UUID().uuidString,
            startWeek: 1,
            endWeek: 16,
            weekPattern: .all,
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
            dayOfWeek: 1,
            startNode: 1,
            nodeCount: 2,
            teacher: "老师",
            room: "教室",
            customTime: nil,
            recurrenceSegments: [segment]
        )
        let course = Course(
            id: courseID,
            name: "持久化课程",
            color: Int32(bitPattern: 0xFF4F8EF7),
            note: "备注",
            credit: 2,
            slots: [slot]
        )
        XCTAssertTrue(firstStore.addCourse(course, to: timetable))

        let secondStore = TimetableStore(storeURL: url)
        let reloaded = try XCTUnwrap(secondStore.timetables.first(where: { $0.id == timetable.id }))
        XCTAssertEqual(reloaded.name, "测试课表")
        let reloadedCourse = try XCTUnwrap(reloaded.courses.first)
        XCTAssertEqual(reloadedCourse.name, "持久化课程")
        XCTAssertEqual(reloadedCourse.slots.first?.teacher, "老师")
        XCTAssertEqual(reloadedCourse.slots.first?.room, "教室")
    }

    func testSharedCommandProducesSingleOccurrenceException() throws {
        let schedule = ReusableTimeTable.defaultDefinition()
        let courseID = UUID().uuidString
        let slotID = UUID().uuidString
        let segmentID = UUID().uuidString
        let segment = RecurrenceSegment(
            id: segmentID,
            startWeek: 1,
            endWeek: 16,
            weekPattern: .all,
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
            dayOfWeek: 1,
            startNode: 1,
            nodeCount: 1,
            teacher: "",
            room: "",
            customTime: nil,
            recurrenceSegments: [segment]
        )
        let timetable = Timetable(
            id: UUID().uuidString,
            name: "共享命令测试",
            firstDayEpochDay: TimetableDates.mondayEpochDay(),
            maxWeek: 16,
            timeTable: schedule.domain,
            courses: [Course(
                id: courseID,
                name: "课程",
                color: Int32(bitPattern: 0xFF4F8EF7),
                note: "",
                credit: 0,
                slots: [slot]
            )],
            dateExceptions: [],
            conflictPreferences: [],
            reminderSettings: .standard,
            sortOrder: 0,
            showSaturday: true,
            showSunday: true,
            sundayFirst: false
        )
        let occurrences = TimetableEngine.shared.expandOccurrences(timetable: timetable)
        let occurrence = try XCTUnwrap(occurrences.first)
        let command = TimetableCommandRescheduleOccurrence(
            logicalSlotId: occurrence.logicalSlotId,
            originalEpochDay: occurrence.originalEpochDay,
            targetEpochDay: occurrence.epochDay + 1,
            recurrenceSegmentId: occurrence.recurrenceSegmentId,
            targetDayOfWeek: Int32(2).asKotlinInt,
            targetStartNode: Int32(2).asKotlinInt,
            targetNodeCount: Int32(1).asKotlinInt,
            targetCustomTime: nil,
            targetTeacher: nil,
            targetRoom: nil
        )

        let updated = try TimetableCommands.shared.apply(timetable: timetable, command: command)
        XCTAssertEqual(updated.dateExceptions.count, 1)
        XCTAssertEqual(updated.dateExceptions.first?.originalEpochDay, occurrence.originalEpochDay)
        XCTAssertEqual(updated.dateExceptions.first?.targetEpochDay?.int64Value, occurrence.epochDay + 1)
    }

    func testCustomTimeOccurrenceRescheduleKeepsSubmittedTargetTime() throws {
        let (timetable, _) = makeTimetable(customTime: MinuteRange(startMinuteOfDay: 505, endMinuteOfDay: 555))
        let occurrence = try XCTUnwrap(TimetableEngine.shared.expandOccurrences(timetable: timetable).first)
        let targetTime = MinuteRange(startMinuteOfDay: 650, endMinuteOfDay: 710)
        let command = TimetableCommandRescheduleOccurrence(
            logicalSlotId: occurrence.logicalSlotId,
            originalEpochDay: occurrence.originalEpochDay,
            targetEpochDay: occurrence.epochDay + 1,
            recurrenceSegmentId: occurrence.recurrenceSegmentId,
            targetDayOfWeek: Int32(2).asKotlinInt,
            targetStartNode: Int32(1).asKotlinInt,
            targetNodeCount: Int32(1).asKotlinInt,
            targetCustomTime: targetTime,
            targetTeacher: nil,
            targetRoom: nil
        )

        let updated = try TimetableCommands.shared.apply(timetable: timetable, command: command)
        let rescheduled = try XCTUnwrap(
            TimetableEngine.shared.expandOccurrences(timetable: updated).first(where: { $0.isRescheduled })
        )
        XCTAssertEqual(rescheduled.epochDay, occurrence.epochDay + 1)
        XCTAssertEqual(rescheduled.startMinuteOfDay, targetTime.startMinuteOfDay)
        XCTAssertEqual(rescheduled.endMinuteOfDay, targetTime.endMinuteOfDay)
        XCTAssertTrue(rescheduled.usesCustomTime)
    }

    func testICSExportUsesUTCDateAndMinuteForDTSTAMP() throws {
        let (timetable, _) = makeTimetable()
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(secondsFromGMT: 0)!
        let now = try XCTUnwrap(utc.date(from: DateComponents(year: 2024, month: 1, day: 2, hour: 1, minute: 3)))
        let data = try TimetableDocumentCodec.encodeICS(
            timetable,
            range: TimetableDates.range(for: timetable, week: 1),
            now: now
        )
        let text = try XCTUnwrap(String(data: data, encoding: .utf8))
        XCTAssertTrue(text.contains("DTSTAMP:20240102T010300Z"))
    }

    func testCorruptedPayloadIsVisibleAlongsideValidTimetableAndCanBeDeleted() throws {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("sleepdown-corrupt-\(UUID().uuidString).sqlite")
        defer { try? FileManager.default.removeItem(at: url) }

        let validID: String
        do {
            let store = TimetableStore(storeURL: url)
            validID = try XCTUnwrap(store.createTimetable(name: "有效课表")?.id)
        }
        try insertTimetableRecord(
            at: url,
            id: "corrupt-record",
            name: "损坏课表",
            payload: "not-json",
            sortOrder: 1
        )

        let store = TimetableStore(storeURL: url)
        XCTAssertTrue(store.timetables.contains(where: { $0.id == validID }))
        XCTAssertEqual(store.corruptedTimetables.map(\.id), ["corrupt-record"])
        let corruptionReason = try XCTUnwrap(store.corruptedTimetables.first?.reason)
        XCTAssertTrue(
            corruptionReason.hasPrefix(AppLocalization.string("error.corrupt_timetable", ""))
        )
        XCTAssertTrue(store.deleteTimetable("corrupt-record"))
        XCTAssertTrue(store.corruptedTimetables.isEmpty)
        XCTAssertTrue(store.timetables.contains(where: { $0.id == validID }))
    }

    func testBackgroundRefreshRequestHasBoundedEarliestDate() throws {
        if #available(iOS 15.0, *) {
            let now = Date(timeIntervalSince1970: 1_700_000_000)
            let request = ReminderScheduler.makeBackgroundRefreshRequest(now: now)
            XCTAssertEqual(request.identifier, ReminderScheduler.backgroundTaskIdentifier)
            XCTAssertEqual(
                request.earliestBeginDate,
                now.addingTimeInterval(ReminderScheduler.backgroundRefreshInterval)
            )
        }
    }

    func testWidgetPublishExposesEncodingFailure() {
        let invalidTimetable = Timetable.blank().replacing(maxWeek: 0)
        let result = WidgetSharedData.publishResult(
            timetables: [invalidTimetable],
            selectedTimetableID: invalidTimetable.id
        )
        guard case .failure(let error) = result else {
            return XCTFail("expected widget publish to fail")
        }
        if case .encodingFailed = error {
            return
        }
        XCTFail("expected an encoding failure, got \(error)")
    }

    func testNotificationSoundDependsOnlyOnSilentSetting() {
        XCTAssertNotNil(ReminderScheduler.notificationSound(for: makeReminderPlan(silent: false)))
        XCTAssertNil(ReminderScheduler.notificationSound(for: makeReminderPlan(silent: true)))
    }

    private func makeReminderPlan(silent: Bool) -> ReminderPlan {
        ReminderPlan(
            id: "reminder",
            occurrenceId: "occurrence",
            kind: .lessonStart,
            eventEpochDay: 20_000,
            eventMinuteOfDay: 600,
            triggerEpochDay: 20_000,
            triggerMinuteOfDay: 590,
            leadMinutes: 10,
            title: "课程开始",
            body: "课程",
            vibrate: false,
            silent: silent
        )
    }

    private func makeTimetable(customTime: MinuteRange? = nil) -> (Timetable, String) {
        let schedule = ReusableTimeTable.defaultDefinition()
        let courseID = UUID().uuidString
        let slotID = UUID().uuidString
        let segment = RecurrenceSegment(
            id: UUID().uuidString,
            startWeek: 1,
            endWeek: 16,
            weekPattern: .all,
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
            dayOfWeek: 1,
            startNode: 1,
            nodeCount: 1,
            teacher: "老师",
            room: "教室",
            customTime: customTime,
            recurrenceSegments: [segment]
        )
        let timetable = Timetable(
            id: UUID().uuidString,
            name: "测试课表",
            firstDayEpochDay: TimetableDates.mondayEpochDay(),
            maxWeek: 16,
            timeTable: schedule.domain,
            courses: [Course(
                id: courseID,
                name: "课程",
                color: Int32(bitPattern: 0xFF4F8EF7),
                note: "",
                credit: 0,
                slots: [slot]
            )],
            dateExceptions: [],
            conflictPreferences: [],
            reminderSettings: .standard,
            sortOrder: 0,
            showSaturday: true,
            showSunday: true,
            sundayFirst: false
        )
        return (timetable, courseID)
    }

    private func insertTimetableRecord(
        at url: URL,
        id: String,
        name: String,
        payload: String,
        sortOrder: Int32
    ) throws {
        let container = NSPersistentContainer(
            name: "SleepDownTests",
            managedObjectModel: TimetableStore.makeManagedObjectModel()
        )
        let storeDescription = NSPersistentStoreDescription(url: url)
        try container.persistentStoreCoordinator.addPersistentStore(
            ofType: NSSQLiteStoreType,
            configurationName: nil,
            at: storeDescription.url,
            options: nil
        )
        defer {
            if let persistentStore = container.persistentStoreCoordinator.persistentStores.first {
                try? container.persistentStoreCoordinator.remove(persistentStore)
            }
        }
        let record = NSEntityDescription.insertNewObject(
            forEntityName: "TimetableRecord",
            into: container.viewContext
        )
        record.setValue(id, forKey: "id")
        record.setValue(name, forKey: "name")
        record.setValue(sortOrder, forKey: "sortOrder")
        record.setValue(payload, forKey: "payload")
        record.setValue(Date(), forKey: "updatedAt")
        record.setValue(Int32(1), forKey: "schemaVersion")
        try container.viewContext.save()
    }
}

@MainActor
extension TimetableStoreTests {
    private func portTimetable() throws -> Timetable {
        let store = TimetableStore(inMemory: true)
        let table = try XCTUnwrap(store.createTimetable(name: "iOS parity"))
        let recurrence = RecurrenceSegment(id: "segment", startWeek: 1, endWeek: 8, weekPattern: .all, dayOfWeek: nil, startNode: nil, nodeCount: nil, teacher: nil, room: nil, customTime: nil)
        let slot = LogicalCourseSlot(id: "slot", courseId: "course", dayOfWeek: 1, startNode: 1, nodeCount: 2, teacher: "", room: "Room", customTime: nil, recurrenceSegments: [recurrence])
        let second = LogicalCourseSlot(id: "independent", courseId: "course", dayOfWeek: 4, startNode: 5, nodeCount: 1, teacher: "Other", room: "", customTime: nil, recurrenceSegments: [RecurrenceSegment(id: "second-segment", startWeek: 1, endWeek: 8, weekPattern: .odd, dayOfWeek: nil, startNode: nil, nodeCount: nil, teacher: nil, room: nil, customTime: nil)])
        return table.replacing(maxWeek: 8, courses: [Course(id: "course", name: "Course", color: Int32(bitPattern: 0xFF648FCC), note: "", credit: 0, slots: [slot, second])])
    }

    func testMovePreviewAndScopesPreserveIndependentSlots() throws {
        let table = try portTimetable()
        let source = try XCTUnwrap(TimetableEngine.shared.expandOccurrences(timetable: table).first { $0.logicalSlotId == "slot" && $0.week == 1 })
        let plan = try IOSCourseMovePlan(timetable: table, occurrence: source, targetEpochDay: source.epochDay + 1, targetStartNode: 3)
        XCTAssertTrue(table.dateExceptions.isEmpty)
        let one = try plan.result(allWeeks: false)
        XCTAssertEqual(one.dateExceptions.count, 1)
        XCTAssertEqual(TimetableEngine.shared.expandOccurrences(timetable: one).first { $0.logicalSlotId == "slot" && $0.week == 2 }?.dayOfWeek, 1)
        let all = try plan.result(allWeeks: true)
        let occurrences = TimetableEngine.shared.expandOccurrences(timetable: all)
        XCTAssertTrue(occurrences.filter { $0.logicalSlotId == "slot" }.allSatisfy { $0.dayOfWeek == 2 && $0.startNode == 3 && $0.teacher.isEmpty })
        XCTAssertTrue(occurrences.filter { $0.logicalSlotId == "independent" }.allSatisfy { $0.dayOfWeek == 4 && $0.startNode == 5 })
    }

    func testMovingRescheduledOccurrenceAllWeeksKeepsOneException() throws {
        let table = try portTimetable()
        let source = try XCTUnwrap(TimetableEngine.shared.expandOccurrences(timetable: table).first { $0.logicalSlotId == "slot" && $0.week == 1 })
        let first = try IOSCourseMovePlan(timetable: table, occurrence: source, targetEpochDay: source.epochDay + 1, targetStartNode: 3).result(allWeeks: false)
        let rescheduled = try XCTUnwrap(TimetableEngine.shared.expandOccurrences(timetable: first).first { $0.isRescheduled })
        let second = try IOSCourseMovePlan(timetable: first, occurrence: rescheduled, targetEpochDay: source.epochDay + 2, targetStartNode: 5).result(allWeeks: true)
        XCTAssertEqual(second.dateExceptions.count, 1)
        let courses = TimetableEngine.shared.expandOccurrences(timetable: second).filter { $0.logicalSlotId == "slot" }
        XCTAssertEqual(courses.count, 8)
        XCTAssertTrue(courses.allSatisfy { $0.dayOfWeek == 3 && $0.startNode == 5 })
    }

    func testArbitraryWeeksRemainEditableWithDateExceptions() throws {
        let table = try portTimetable()
        let source = try XCTUnwrap(TimetableEngine.shared.expandOccurrences(timetable: table).first { $0.logicalSlotId == "slot" && $0.week == 5 })
        let moved = try IOSCourseMovePlan(timetable: table, occurrence: source, targetEpochDay: source.epochDay + 1, targetStartNode: 3).result(allWeeks: false)
        let course = try XCTUnwrap(moved.courses.first)
        var draft = IOSSlotDraft(course.slots[0], maxWeek: 8)
        draft.recurrences[0].weeks = [1, 3, 5, 8]
        let slot = try draft.slot(schedule: moved.timeTable, maxWeek: 8)
        let updatedCourse = course.replacing(slots: [slot, course.slots[1]])
        let result = try IOSCourseEditing.replacingCourse(updatedCourse, in: moved)
        let occurrences = TimetableEngine.shared.expandOccurrences(timetable: result).filter { $0.logicalSlotId == "slot" }
        XCTAssertEqual(Set(occurrences.map { Int($0.week) }), [1, 3, 5, 8])
        XCTAssertEqual(occurrences.filter { $0.isRescheduled }.count, 1)
        XCTAssertEqual(occurrences.first { $0.isRescheduled }?.startNode, 3)
        XCTAssertEqual(result.dateExceptions.count, 1)
        XCTAssertEqual(Set(result.courses[0].slots[0].recurrenceSegments.map(\.id)).count, 4)
    }

    func testGridUsesPeriodRowsAndBreakRangePreservesOutsideTimes() throws {
        let table = try portTimetable()
        let source = try XCTUnwrap(TimetableEngine.shared.expandOccurrences(timetable: table).first { $0.logicalSlotId == "slot" })
        let span = IOSGridGeometry.span(source, nodes: table.timeTable.nodes)
        XCTAssertEqual(span.top, 0)
        XCTAssertEqual(span.height, 2)
        let original = ReusableTimeTable.defaultDefinition()
        let changed = try original.applyingBreak(minutes: 10, first: 1, last: 4)
        XCTAssertEqual(changed.nodes[0], original.nodes[0])
        XCTAssertEqual(changed.nodes[4], original.nodes[4])
        XCTAssertEqual(changed.nodes[1].startMinuteOfDay - changed.nodes[0].endMinuteOfDay, 10)
        XCTAssertThrowsError(try original.applyingBreak(minutes: 240, first: 1, last: 4))
        XCTAssertThrowsError(try original.applyingBreak(minutes: 10, first: 4, last: 1))
    }
}

@MainActor
extension TimetableStoreTests {
    private func widgetTimetable(start: Int32 = 480, end: Int32 = 540) -> Timetable {
        let (base, courseID) = makeTimetable()
        let slots = (1...7).map { day in
            LogicalCourseSlot(id: "widget-slot-\(day)", courseId: courseID, dayOfWeek: Int32(day),
                startNode: 1, nodeCount: 1, teacher: "Teacher", room: "Room",
                customTime: MinuteRange(startMinuteOfDay: start, endMinuteOfDay: end),
                recurrenceSegments: [RecurrenceSegment(id: "widget-segment-\(day)", startWeek: 1, endWeek: 16,
                    weekPattern: .all, dayOfWeek: nil, startNode: nil, nodeCount: nil, teacher: nil, room: nil, customTime: nil)])
        }
        return base.replacing(courses: [Course(id: courseID, name: "Widget Course", color: Int32(bitPattern: 0xFF3F51B5),
            note: "", credit: 0, slots: slots)])
    }

    func testWidgetRemainingCoursesKeepEndMinuteAndHidePastDays() throws {
        let table = widgetTimetable()
        let monday = table.firstDayEpochDay
        let current = WidgetPresentation.snapshot(timetable: table, kind: .today, nowEpochDay: monday, nowMinuteOfDay: 539)
        XCTAssertTrue(try XCTUnwrap(current.items.first).isCurrent)
        let atEnd = WidgetPresentation.snapshot(timetable: table, kind: .today, nowEpochDay: monday, nowMinuteOfDay: 540)
        XCTAssertEqual(atEnd.items.count, 1)
        XCTAssertFalse(try XCTUnwrap(atEnd.items.first).isCurrent)
        XCTAssertTrue(WidgetPresentation.snapshot(timetable: table, kind: .today,
            nowEpochDay: monday, nowMinuteOfDay: 541).items.isEmpty)
        let week = WidgetPresentation.snapshot(timetable: table, kind: .week, nowEpochDay: monday + 1, nowMinuteOfDay: 541)
        XCTAssertEqual(week.anchorEpochDay, monday)
        XCTAssertEqual(week.items.map(\.epochDay), Array((monday + 2)...(monday + 6)))
        XCTAssertTrue(WidgetPresentation.snapshot(timetable: table, kind: .week,
            nowEpochDay: monday + 6, nowMinuteOfDay: 541).items.isEmpty)
        // The single-upcoming widget retains its shared contract, unlike the remaining-course lists.
        let next = WidgetPresentation.snapshot(timetable: table, kind: .next, nowEpochDay: monday, nowMinuteOfDay: 540)
        XCTAssertEqual(next.items.first?.epochDay, monday + 1)
    }

    func testWidgetTwoDaysCrossIsoWeekAndNeverMarksTomorrowCurrent() throws {
        let table = widgetTimetable(start: 0, end: 30)
        let sunday = table.firstDayEpochDay + 6
        let snapshot = WidgetPresentation.snapshot(timetable: table, kind: .today,
            nowEpochDay: sunday, nowMinuteOfDay: 0, includesTomorrow: true)
        XCTAssertEqual(snapshot.items.map(\.epochDay), [sunday, sunday + 1])
        XCTAssertEqual(snapshot.items.map(\.isCurrent), [true, false])
        XCTAssertEqual(Set(snapshot.items.map(\.occurrenceId)).count, 2)
        let late = WidgetPresentation.snapshot(timetable: table, kind: .today,
            nowEpochDay: sunday, nowMinuteOfDay: 1439, includesTomorrow: true)
        XCTAssertEqual(late.items.map(\.epochDay), [sunday + 1])
        let rolled = WidgetPresentation.snapshot(timetable: table, kind: .today,
            nowEpochDay: sunday + 1, nowMinuteOfDay: 0, includesTomorrow: true)
        XCTAssertEqual(rolled.items.map(\.epochDay), [sunday + 1, sunday + 2])
    }

    func testWidgetSundayFirstKeepsIsoDatesAndHonorsHiddenWeekends() {
        let table = widgetTimetable().replacing(sundayFirst: true)
        let start = table.firstDayEpochDay
        let week = WidgetPresentation.snapshot(timetable: table, kind: .week, nowEpochDay: start, nowMinuteOfDay: 0)
        XCTAssertEqual(week.anchorEpochDay, start)
        XCTAssertEqual(week.items.map(\.epochDay), [start + 6] + Array(start...(start + 5)))
        let hidden = table.replacing(showSaturday: false, showSunday: false)
        let weekdays = WidgetPresentation.snapshot(timetable: hidden, kind: .week, nowEpochDay: start, nowMinuteOfDay: 0)
        XCTAssertEqual(weekdays.items.map(\.epochDay), Array(start...(start + 4)))
    }

    func testWidgetTimelineIncludesStartEndVisibilityAndMidnight() throws {
        let table = widgetTimetable()
        let start = table.firstDayEpochDay
        let now = try XCTUnwrap(WidgetDateSupport.date(epochDay: start, minuteOfDay: 479))
        let dates = WidgetPresentation.timelineDates(timetable: table, now: now)
        XCTAssertEqual(dates.first, now)
        XCTAssertEqual(dates.count, Set(dates).count)
        XCTAssertEqual(dates, dates.sorted())
        for minute in [Int32(480), 540, 541] {
            XCTAssertTrue(dates.contains(try XCTUnwrap(WidgetDateSupport.date(epochDay: start, minuteOfDay: minute))))
        }
        XCTAssertEqual(dates.last, WidgetDateSupport.date(epochDay: start + 1))
        for date in dates {
            let civil = WidgetDateSupport.nowComponents(for: date)
            let snapshot = WidgetPresentation.snapshot(timetable: table, kind: .today,
                nowEpochDay: civil.epochDay, nowMinuteOfDay: civil.minuteOfDay)
            XCTAssertFalse(snapshot.items.contains { $0.epochDay < civil.epochDay || $0.endMinuteOfDay < civil.minuteOfDay })
        }
    }

    func testWidgetMidnightUsesLocalCalendarAcrossDaylightSaving() throws {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = try XCTUnwrap(TimeZone(identifier: "America/Los_Angeles"))
        let utc = try XCTUnwrap(ISO8601DateFormatter().date(from: "2026-03-08T00:00:00Z"))
        let epochDay = Int64(utc.timeIntervalSince1970 / 86_400)
        let midnight = try XCTUnwrap(WidgetDateSupport.date(epochDay: epochDay, calendar: calendar))
        let dates = WidgetPresentation.timelineDates(timetable: widgetTimetable(), now: midnight, calendar: calendar)
        XCTAssertEqual(try XCTUnwrap(dates.last).timeIntervalSince(midnight), 23 * 3600, accuracy: 1)
        XCTAssertEqual(WidgetDateSupport.nowComponents(for: try XCTUnwrap(dates.last), calendar: calendar).epochDay, epochDay + 1)
    }

    func testWidgetTimelineHandlesRepeatedHourAndBatchesDenseDays() throws {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = try XCTUnwrap(TimeZone(identifier: "America/Los_Angeles"))
        let utc = try XCTUnwrap(ISO8601DateFormatter().date(from: "2026-11-01T00:00:00Z"))
        let day = Int64(utc.timeIntervalSince1970 / 86_400)
        let table = widgetTimetable(start: 80, end: 100).replacing(firstDayEpochDay: day - 6)
        let now = try XCTUnwrap(WidgetDateSupport.date(epochDay: day, calendar: calendar))
        let dates = WidgetPresentation.timelineDates(timetable: table, now: now, calendar: calendar)
        XCTAssertEqual(try XCTUnwrap(dates.last).timeIntervalSince(now), 25 * 3600, accuracy: 1)
        for policy in [Calendar.RepeatedTimePolicy.first, .last] {
            let start = try XCTUnwrap(WidgetDateSupport.date(epochDay: day, minuteOfDay: 80,
                calendar: calendar, repeatedTimePolicy: policy))
            XCTAssertTrue(dates.contains(start))
        }
        XCTAssertTrue(dates.contains(try XCTUnwrap(calendar.timeZone.nextDaylightSavingTimeTransition(after: now))))

        let base = widgetTimetable()
        let course = try XCTUnwrap(base.courses.first)
        let source = try XCTUnwrap(course.slots.first)
        let slots: [LogicalCourseSlot] = (0..<100).map { (index: Int) -> LogicalCourseSlot in
            let range = MinuteRange(startMinuteOfDay: Int32(index * 10 + 1), endMinuteOfDay: Int32(index * 10 + 5))
            return LogicalCourseSlot(id: "dense-\(index)", courseId: course.id, dayOfWeek: 1,
                startNode: 1, nodeCount: 1, teacher: "", room: "", customTime: range,
                recurrenceSegments: source.recurrenceSegments)
        }
        let dense = base.replacing(courses: [Course(id: course.id, name: course.name, color: course.color,
            note: "", credit: 0, slots: slots)])
        let midnight = try XCTUnwrap(WidgetDateSupport.date(epochDay: dense.firstDayEpochDay))
        let batch = WidgetPresentation.timelineDates(timetable: dense, now: midnight)
        XCTAssertEqual(batch.count, 64)
        XCTAssertEqual(batch.first, midnight)
        XCTAssertEqual(batch.count, Set(batch).count)
        let next = WidgetPresentation.timelineDates(timetable: dense, now: try XCTUnwrap(batch.last))
        XCTAssertGreaterThan(try XCTUnwrap(next.last), try XCTUnwrap(batch.last))
        XCTAssertEqual(WidgetPresentation.visibleItemLimit(height: 40, rowHeight: 84), 0)
        XCTAssertEqual(WidgetPresentation.visibleItemLimit(height: 100, rowHeight: 84, footerHeight: 48), 0)
    }

    func testWidgetSelectionDoesNotRebindDeletedConfiguredTable() {
        let a = WidgetSharedData.Record(id: "a", name: "A", sortOrder: 0, payload: "invalid A")
        let b = WidgetSharedData.Record(id: "b", name: "B", sortOrder: 1, payload: "B")
        let catalog = WidgetSharedData.Catalog(schemaVersion: 1, generatedAt: Date(), selectedTimetableID: "b", timetables: [a, b])
        XCTAssertEqual(WidgetPresentation.record(in: catalog, configuredID: nil)?.id, "b")
        XCTAssertEqual(WidgetPresentation.record(in: catalog, configuredID: "a")?.id, "a")
        XCTAssertNil(WidgetPresentation.record(in: catalog, configuredID: "deleted"))
        XCTAssertEqual(WidgetPresentation.record(in: catalog, configuredID: "b")?.id, "b")
    }

    func testWidgetDeepLinksRoundTripBindingAndRejectInvalidDates() throws {
        let url = WidgetDeepLink.weekURL(timetableID: "table / 中文 & #", epochDay: 20_000)
        let destination = try XCTUnwrap(WidgetDeepLink.destination(from: url))
        XCTAssertEqual(destination.timetableID, "table / 中文 & #")
        XCTAssertEqual(destination.epochDay, 20_000)
        XCTAssertNil(WidgetDeepLink.destination(from: URL(string: "https://week?epochDay=20000")!))
        XCTAssertNil(WidgetDeepLink.destination(from: URL(string: "sleepdown://week?epochDay=9223372036854775807")!))
        XCTAssertNil(WidgetDeepLink.destination(from: URL(string: "sleepdown://week?epochDay=bad")!))
        XCTAssertNil(try XCTUnwrap(WidgetDeepLink.destination(from: URL(string: "sleepdown://week?epochDay=20000")!)).timetableID)
        XCTAssertEqual(WidgetPresentation.visibleItemLimit(height: 100, rowHeight: 42), 1)
        XCTAssertEqual(WidgetPresentation.visibleItemLimit(height: 150, rowHeight: 42), 3)
        XCTAssertEqual(WidgetPresentation.visibleItemLimit(height: 150, rowHeight: 84), 1)
    }
}
