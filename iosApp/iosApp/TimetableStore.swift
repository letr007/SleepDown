import Combine
import CoreData
import Foundation
import shared

struct CorruptedTimetableRecord: Identifiable, Equatable {
    let id: String
    let name: String
    let reason: String
}

@MainActor
final class TimetableStore: ObservableObject {
    @Published private(set) var timetables: [Timetable] = []
    @Published private(set) var corruptedTimetables: [CorruptedTimetableRecord] = []
    @Published private(set) var timeTableDefinitions: [ReusableTimeTable] = []
    @Published private(set) var selectedTimetableID: String?
    @Published private(set) var changeToken = UUID()
    @Published var errorMessage: String?
    @Published private(set) var widgetPublishError: String?

    private let container: NSPersistentContainer
    private let context: NSManagedObjectContext
    private let defaults = UserDefaults.standard
    private let selectedTimetableKey = "selectedTimetableID"
    private var lastValidTimetables: [String: Timetable] = [:]

    init(storeURL: URL? = nil, inMemory: Bool = false) {
        let model = Self.makeManagedObjectModel()
        container = NSPersistentContainer(name: "SleepDown", managedObjectModel: model)

        let description: NSPersistentStoreDescription
        if inMemory {
            description = NSPersistentStoreDescription()
            description.type = NSInMemoryStoreType
        } else if let storeURL {
            description = NSPersistentStoreDescription(url: storeURL)
        } else {
            let applicationSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
                .appendingPathComponent("SleepDown", isDirectory: true)
            try? FileManager.default.createDirectory(at: applicationSupport, withIntermediateDirectories: true)
            description = NSPersistentStoreDescription(
                url: applicationSupport.appendingPathComponent("SleepDown.sqlite")
            )
        }
        description.shouldMigrateStoreAutomatically = true
        description.shouldInferMappingModelAutomatically = true
        container.persistentStoreDescriptions = [description]

        var loadError: Error?
        do {
            try container.persistentStoreCoordinator.addPersistentStore(
                ofType: description.type,
                configurationName: description.configuration,
                at: description.url,
                options: description.options
            )
        } catch {
            loadError = error
        }
        context = container.viewContext
        context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        context.undoManager = nil

        if let loadError {
            errorMessage = AppLocalization.string("error.open_local_timetable", loadError.localizedDescription)
            return
        }

        do {
            try ensureDefaultTimeTableDefinition()
            reload()
        } catch {
            errorMessage = AppLocalization.string("error.read_local_timetable", error.localizedDescription)
        }
    }

    var selectedTimetable: Timetable? {
        guard let selectedTimetableID else { return timetables.first }
        return timetables.first(where: { $0.id == selectedTimetableID }) ?? timetables.first
    }

    @discardableResult
    func retryWidgetPublish() -> Bool {
        publishWidgetData()
    }

    func selectTimetable(_ id: String) {
        guard timetables.contains(where: { $0.id == id }) else { return }
        selectedTimetableID = id
        defaults.set(id, forKey: selectedTimetableKey)
        _ = publishWidgetData()
        changeToken = UUID()
    }

    @discardableResult
    func createTimetable(name: String, using definition: ReusableTimeTable? = nil) -> Timetable? {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else {
            errorMessage = AppLocalization.string("error.enter_timetable_name")
            return nil
        }
        let schedule = definition ?? timeTableDefinitions.first ?? .defaultDefinition()
        let timetable = Timetable.blank(
            name: trimmedName,
            schedule: schedule,
            id: UUID().uuidString,
            firstDayEpochDay: TimetableDates.mondayEpochDay()
        ).replacing(sortOrder: Int32(timetables.count))
        guard save(timetable) else { return nil }
        selectTimetable(timetable.id)
        return timetable
    }

    @discardableResult
    func save(_ timetable: Timetable) -> Bool {
        guard validateCourseSlots(in: timetable) else { return false }

        let request = NSFetchRequest<NSManagedObject>(entityName: "TimetableRecord")
        request.fetchLimit = 1
        request.predicate = NSPredicate(format: "id == %@", timetable.id)

        do {
            let record = try context.fetch(request).first ?? NSEntityDescription.insertNewObject(
                forEntityName: "TimetableRecord",
                into: context
            )
            record.setValue(timetable.id, forKey: "id")
            record.setValue(timetable.name, forKey: "name")
            record.setValue(timetable.sortOrder, forKey: "sortOrder")
            record.setValue(try SharedInterop.encodeBackup(timetable), forKey: "payload")
            record.setValue(Date(), forKey: "updatedAt")
            record.setValue(Int32(1), forKey: "schemaVersion")
            try context.save()
            reload()
            changeToken = UUID()
            return true
        } catch {
            errorMessage = AppLocalization.string("error.save_timetable", error.localizedDescription)
            return false
        }
    }

    @discardableResult
    func deleteTimetable(_ id: String) -> Bool {
        let wasSelected = selectedTimetableID == id
        let request = NSFetchRequest<NSManagedObject>(entityName: "TimetableRecord")
        request.fetchLimit = 1
        request.predicate = NSPredicate(format: "id == %@", id)
        do {
            let record: NSManagedObject?
            if let fetched = try context.fetch(request).first {
                record = fetched
            } else if let objectIDURL = URL(string: id),
                      let objectID = context.persistentStoreCoordinator?.managedObjectID(
                          forURIRepresentation: objectIDURL
                      ) {
                record = try? context.existingObject(with: objectID)
            } else {
                record = nil
            }
            if let record {
                context.delete(record)
                try context.save()
            }
            lastValidTimetables.removeValue(forKey: id)
            reload()
            if wasSelected {
                selectedTimetableID = timetables.first?.id
                if let selectedTimetableID {
                    defaults.set(selectedTimetableID, forKey: selectedTimetableKey)
                } else {
                    defaults.removeObject(forKey: selectedTimetableKey)
                }
            }
            _ = publishWidgetData()
            changeToken = UUID()
            return true
        } catch {
            errorMessage = AppLocalization.string("error.delete_timetable", error.localizedDescription)
            return false
        }
    }

    func moveTimetable(from offsets: IndexSet, to destination: Int) {
        var ordered = timetables
        ordered.move(fromOffsets: offsets, toOffset: destination)
        for (index, timetable) in ordered.enumerated() {
            let updated = timetable.replacing(sortOrder: Int32(index))
            guard persistWithoutReload(updated) else { context.rollback(); return }
        }
        do {
            try context.save()
            reload()
            changeToken = UUID()
        } catch {
            context.rollback()
            errorMessage = AppLocalization.string("error.reorder_timetable", error.localizedDescription)
        }
    }

    @discardableResult
    func updateTimetable(_ timetable: Timetable) -> Bool {
        save(timetable)
    }

    @discardableResult
    func updateCourse(_ course: Course, in timetable: Timetable? = nil) -> Bool {
        guard let current = timetable ?? selectedTimetable else { return false }
        let courses = current.courses.map { $0.id == course.id ? course : $0 }
        guard courses.contains(where: { $0.id == course.id }) else {
            errorMessage = AppLocalization.string("error.course_not_found")
            return false
        }
        do {
            return save(try IOSCourseEditing.replacingCourse(course, in: current))
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    @discardableResult
    func addCourse(_ course: Course, to timetable: Timetable? = nil) -> Bool {
        guard let current = timetable ?? selectedTimetable else { return false }
        guard !current.courses.contains(where: { $0.id == course.id }) else {
            errorMessage = AppLocalization.string("error.course_id_exists")
            return false
        }
        return save(current.replacing(courses: current.courses + [course]))
    }

    @discardableResult
    func apply(_ command: TimetableCommand, to timetable: Timetable? = nil) -> Bool {
        guard let current = timetable ?? selectedTimetable else { return false }
        do {
            let updated = try SharedInterop.apply(current, command: command)
            return save(updated)
        } catch {
            errorMessage = AppLocalization.string("error.operation", error.localizedDescription)
            return false
        }
    }

    @discardableResult
    func deleteCourse(_ courseID: String, from timetable: Timetable? = nil) -> Bool {
        apply(TimetableCommandDeleteCourse(courseId: courseID), to: timetable)
    }

    @discardableResult
    func setPreferredOccurrence(_ occurrence: CourseOccurrence, in timetable: Timetable? = nil) -> Bool {
        guard let current = timetable ?? selectedTimetable else { return false }
        let remaining = current.conflictPreferences.filter {
            !($0.epochDay?.int64Value == occurrence.epochDay && $0.logicalSlotId == occurrence.logicalSlotId)
        }
        let preference = ConflictPreference(
            courseId: occurrence.courseId,
            priority: 0,
            logicalSlotId: occurrence.logicalSlotId,
            occurrenceId: occurrence.id,
            epochDay: occurrence.epochDay.asKotlinLong
        )
        return save(current.replacing(conflictPreferences: remaining + [preference]))
    }

    @discardableResult
    func saveDefinition(_ definition: ReusableTimeTable) -> Bool {
        let validation = TimeTableValidator.shared.validate(timeTable: definition.domain)
        guard validation.isValid else {
            errorMessage = AppLocalization.string(
                "error.validation",
                validation.issues.map(\.message).joined(separator: "\n")
            )
            return false
        }

        do {
            let affectedUpdates = timetables
                .filter { $0.timeTable.id == definition.id }
                .map { $0.replacing(timeTable: definition.domain) }
            guard affectedUpdates.allSatisfy({ validateCourseSlots(in: $0) }) else {
                return false
            }

            let request = NSFetchRequest<NSManagedObject>(entityName: "TimeTableDefinitionRecord")
            request.fetchLimit = 1
            request.predicate = NSPredicate(format: "id == %@", definition.id)
            let record = try context.fetch(request).first ?? NSEntityDescription.insertNewObject(
                forEntityName: "TimeTableDefinitionRecord",
                into: context
            )
            record.setValue(definition.id, forKey: "id")
            record.setValue(definition.name, forKey: "name")
            record.setValue(Int32(timeTableDefinitions.firstIndex(where: { $0.id == definition.id }) ?? timeTableDefinitions.count), forKey: "sortOrder")
            record.setValue(try JSONEncoder().encode(definition.nodes), forKey: "nodesPayload")
            record.setValue(Date(), forKey: "updatedAt")
            record.setValue(Int32(1), forKey: "schemaVersion")

            for updated in affectedUpdates {
                guard persistWithoutReload(updated) else { return false }
            }
            try context.save()
            reload()
            changeToken = UUID()
            return true
        } catch {
            errorMessage = AppLocalization.string("error.save_schedule", error.localizedDescription)
            return false
        }
    }

    @discardableResult
    func createDefinition(name: String, nodes: [TimeTableNodeValue]) -> ReusableTimeTable? {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else {
            errorMessage = AppLocalization.string("error.enter_schedule_name")
            return nil
        }
        let definition = ReusableTimeTable(id: UUID().uuidString, name: trimmedName, nodes: nodes)
        return saveDefinition(definition) ? definition : nil
    }

    @discardableResult
    func copyDefinition(_ definition: ReusableTimeTable) -> ReusableTimeTable? {
        let copy = ReusableTimeTable(
            id: UUID().uuidString,
            name: uniqueDefinitionName(definition.name),
            nodes: definition.nodes
        )
        return saveDefinition(copy) ? copy : nil
    }

    @discardableResult
    func deleteDefinition(_ id: String) -> Bool {
        guard id != ReusableTimeTable.defaultID else {
            errorMessage = AppLocalization.string("error.default_schedule_delete")
            return false
        }
        guard !timetables.contains(where: { $0.timeTable.id == id }) else {
            errorMessage = AppLocalization.string("error.schedule_in_use")
            return false
        }
        let request = NSFetchRequest<NSManagedObject>(entityName: "TimeTableDefinitionRecord")
        request.fetchLimit = 1
        request.predicate = NSPredicate(format: "id == %@", id)
        do {
            if let record = try context.fetch(request).first {
                context.delete(record)
                try context.save()
            }
            reload()
            changeToken = UUID()
            return true
        } catch {
            errorMessage = AppLocalization.string("error.delete_schedule", error.localizedDescription)
            return false
        }
    }

    @discardableResult
    func importTimetable(_ imported: Timetable, destination: TimetableImportDestination) -> Bool {
        do {
            switch destination {
            case .create:
                let copy = imported.copying(
                    id: UUID().uuidString,
                    name: uniqueTimetableName(imported.name),
                    sortOrder: Int32(timetables.count)
                )
                return save(copy)
            case .merge:
                guard let current = selectedTimetable else {
                    return importTimetable(imported, destination: .create)
                }
                let result = try SharedInterop.applyImport(
                    current: current,
                    imported: imported,
                    mode: BackupImportMode.merge
                )
                return save(result.timetable)
            case .replace:
                guard let current = selectedTimetable else {
                    return importTimetable(imported, destination: .create)
                }
                let result = try SharedInterop.applyImport(
                    current: current,
                    imported: imported,
                    mode: BackupImportMode.replace
                )
                return save(
                    result.timetable.copying(
                        id: current.id,
                        name: result.timetable.name,
                        sortOrder: current.sortOrder
                    )
                )
            }
        } catch {
            errorMessage = AppLocalization.string("error.import_timetable", error.localizedDescription)
            return false
        }
    }

    func reload() {
        let timetableRequest = NSFetchRequest<NSManagedObject>(entityName: "TimetableRecord")
        timetableRequest.sortDescriptors = [
            NSSortDescriptor(key: "sortOrder", ascending: true),
            NSSortDescriptor(key: "name", ascending: true)
        ]
        let definitionRequest = NSFetchRequest<NSManagedObject>(entityName: "TimeTableDefinitionRecord")
        definitionRequest.sortDescriptors = [
            NSSortDescriptor(key: "sortOrder", ascending: true),
            NSSortDescriptor(key: "name", ascending: true)
        ]

        do {
            let records = try context.fetch(timetableRequest)
            let recordIDs = Set(records.compactMap { record in
                (record.value(forKey: "id") as? String)?.trimmedOrNil
            })
            lastValidTimetables = lastValidTimetables.filter { recordIDs.contains($0.key) }

            var loadedTimetables: [Timetable] = []
            var loadedCorruptedRecords: [CorruptedTimetableRecord] = []
            for record in records {
                let persistedID = (record.value(forKey: "id") as? String)?.trimmedOrNil
                    ?? record.objectID.uriRepresentation().absoluteString
                let name = (record.value(forKey: "name") as? String)?.trimmedOrNil
                    ?? AppLocalization.string("timetable.corrupted")
                guard let payload = record.value(forKey: "payload") as? String,
                      !payload.isEmpty else {
                    loadedCorruptedRecords.append(
                        CorruptedTimetableRecord(
                            id: persistedID,
                            name: name,
                            reason: AppLocalization.string("error.empty_timetable_payload")
                        )
                    )
                    if let fallback = lastValidTimetables[persistedID] {
                        loadedTimetables.append(fallback)
                    }
                    continue
                }

                do {
                    let timetable = try SharedInterop.decodeBackup(payload)
                    lastValidTimetables[persistedID] = timetable
                    loadedTimetables.append(timetable)
                } catch {
                    loadedCorruptedRecords.append(
                        CorruptedTimetableRecord(
                            id: persistedID,
                            name: name,
                            reason: AppLocalization.string("error.corrupt_timetable", error.localizedDescription)
                        )
                    )
                    if let fallback = lastValidTimetables[persistedID] {
                        loadedTimetables.append(fallback)
                    }
                }
            }

            let definitions = try context.fetch(definitionRequest)
            let loadedDefinitions: [ReusableTimeTable] = definitions.compactMap { record in
                guard
                    let id = record.value(forKey: "id") as? String,
                    let name = record.value(forKey: "name") as? String,
                    let payload = record.value(forKey: "nodesPayload") as? Data,
                    let nodes = try? JSONDecoder().decode([TimeTableNodeValue].self, from: payload)
                else { return nil }
                return ReusableTimeTable(id: id, name: name, nodes: nodes)
            }

            timetables = loadedTimetables
            corruptedTimetables = loadedCorruptedRecords
            timeTableDefinitions = loadedDefinitions.isEmpty ? [.defaultDefinition()] : loadedDefinitions
            let persistedSelection = defaults.string(forKey: selectedTimetableKey)
            selectedTimetableID = timetables.contains(where: { $0.id == persistedSelection })
                ? persistedSelection
                : timetables.first?.id
            _ = publishWidgetData()
        } catch {
            errorMessage = AppLocalization.string("error.read_timetables", error.localizedDescription)
        }
    }

    private func ensureDefaultTimeTableDefinition() throws {
        let request = NSFetchRequest<NSManagedObject>(entityName: "TimeTableDefinitionRecord")
        request.fetchLimit = 1
        request.predicate = NSPredicate(format: "id == %@", ReusableTimeTable.defaultID)
        guard try context.fetch(request).isEmpty else { return }

        let definition = ReusableTimeTable.defaultDefinition()
        let record = NSEntityDescription.insertNewObject(
            forEntityName: "TimeTableDefinitionRecord",
            into: context
        )
        record.setValue(definition.id, forKey: "id")
        record.setValue(definition.name, forKey: "name")
        record.setValue(Int32(0), forKey: "sortOrder")
        record.setValue(try JSONEncoder().encode(definition.nodes), forKey: "nodesPayload")
        record.setValue(Date(), forKey: "updatedAt")
        record.setValue(Int32(1), forKey: "schemaVersion")
        try context.save()
    }

    private func persistWithoutReload(_ timetable: Timetable) -> Bool {
        guard validateCourseSlots(in: timetable) else { return false }

        let request = NSFetchRequest<NSManagedObject>(entityName: "TimetableRecord")
        request.fetchLimit = 1
        request.predicate = NSPredicate(format: "id == %@", timetable.id)
        do {
            let record = try context.fetch(request).first ?? NSEntityDescription.insertNewObject(
                forEntityName: "TimetableRecord",
                into: context
            )
            record.setValue(timetable.id, forKey: "id")
            record.setValue(timetable.name, forKey: "name")
            record.setValue(timetable.sortOrder, forKey: "sortOrder")
            record.setValue(try SharedInterop.encodeBackup(timetable), forKey: "payload")
            record.setValue(Date(), forKey: "updatedAt")
            record.setValue(Int32(1), forKey: "schemaVersion")
            return true
        } catch {
            errorMessage = AppLocalization.string("error.prepare_save_timetable", error.localizedDescription)
            return false
        }
    }

    @discardableResult
    private func publishWidgetData() -> Bool {
        switch WidgetSharedData.publishResult(
            timetables: timetables,
            selectedTimetableID: selectedTimetableID
        ) {
        case .success:
            widgetPublishError = nil
            return true
        case .failure(let error):
            widgetPublishError = error.localizedDescription
            return false
        }
    }

    private func uniqueTimetableName(_ name: String) -> String {
        var candidate = name
        var suffix = 2
        while timetables.contains(where: { $0.name == candidate }) {
            candidate = "\(name) \(suffix)"
            suffix += 1
        }
        return candidate
    }

    private func uniqueDefinitionName(_ name: String) -> String {
        var candidate = name
        var suffix = 2
        while timeTableDefinitions.contains(where: { $0.name == candidate }) {
            candidate = "\(name) \(suffix)"
            suffix += 1
        }
        return candidate
    }

    private func validateCourseSlots(in timetable: Timetable) -> Bool {
        let schedule = timetable.timeTable
        for course in timetable.courses {
            for slot in course.slots {
                guard (1...7).contains(Int(slot.dayOfWeek)),
                      slot.startNode > 0,
                      slot.nodeCount > 0,
                      schedule.rangeForNodes(startNode: slot.startNode, nodeCount: slot.nodeCount) != nil else {
                    errorMessage = AppLocalization.string(
                        "error.course_slot_out_of_range",
                        course.name
                    )
                    return false
                }

                for segment in slot.recurrenceSegments {
                    let dayOfWeek = segment.dayOfWeek?.int32Value ?? slot.dayOfWeek
                    let startNode = segment.startNode?.int32Value ?? slot.startNode
                    let nodeCount = segment.nodeCount?.int32Value ?? slot.nodeCount
                    guard (1...7).contains(Int(dayOfWeek)),
                          startNode > 0,
                          nodeCount > 0,
                          schedule.rangeForNodes(startNode: startNode, nodeCount: nodeCount) != nil else {
                        errorMessage = AppLocalization.string(
                            "error.course_slot_out_of_range",
                            course.name
                        )
                        return false
                    }
                }
            }
        }
        return true
    }

    static func makeManagedObjectModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()

        let timetableEntity = NSEntityDescription()
        timetableEntity.name = "TimetableRecord"
        timetableEntity.managedObjectClassName = NSStringFromClass(NSManagedObject.self)
        timetableEntity.properties = [
            attribute("id", type: .stringAttributeType),
            attribute("name", type: .stringAttributeType),
            attribute("sortOrder", type: .integer32AttributeType),
            attribute("payload", type: .stringAttributeType),
            attribute("updatedAt", type: .dateAttributeType),
            attribute("schemaVersion", type: .integer32AttributeType)
        ]

        timetableEntity.uniquenessConstraints = [["id"]]

        let definitionEntity = NSEntityDescription()
        definitionEntity.name = "TimeTableDefinitionRecord"
        definitionEntity.managedObjectClassName = NSStringFromClass(NSManagedObject.self)
        definitionEntity.properties = [
            attribute("id", type: .stringAttributeType),
            attribute("name", type: .stringAttributeType),
            attribute("sortOrder", type: .integer32AttributeType),
            attribute("nodesPayload", type: .binaryDataAttributeType),
            attribute("updatedAt", type: .dateAttributeType),
            attribute("schemaVersion", type: .integer32AttributeType)
        ]

        definitionEntity.uniquenessConstraints = [["id"]]

        model.entities = [timetableEntity, definitionEntity]
        return model
    }

    private static func attribute(_ name: String, type: NSAttributeType) -> NSAttributeDescription {
        let description = NSAttributeDescription()
        description.name = name
        description.attributeType = type
        description.isOptional = false
        return description
    }
}

enum TimetableImportDestination: Equatable {
    case create
    case merge
    case replace
}

private extension Timetable {
    func copying(id: String, name: String, sortOrder: Int32) -> Timetable {
        Timetable(
            id: id,
            name: name,
            firstDayEpochDay: firstDayEpochDay,
            maxWeek: maxWeek,
            timeTable: timeTable,
            courses: courses,
            dateExceptions: dateExceptions,
            conflictPreferences: conflictPreferences,
            reminderSettings: reminderSettings,
            sortOrder: sortOrder,
            showSaturday: showSaturday,
            showSunday: showSunday,
            sundayFirst: sundayFirst
        )
    }
}
