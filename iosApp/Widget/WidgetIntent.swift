import AppIntents
import Foundation

@available(iOS 17.0, *)
struct TimetableEntity: AppEntity, Identifiable, Hashable {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "widget.configuration.timetable")
    static let defaultQuery = TimetableEntityQuery()

    let id: String
    let name: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: LocalizedStringResource(stringLiteral: name))
    }
}

@available(iOS 17.0, *)
struct TimetableEntityQuery: EntityQuery {
    func entities(for identifiers: [TimetableEntity.ID]) async throws -> [TimetableEntity] {
        let records = WidgetSharedData.loadCatalog()?.timetables ?? []
        return identifiers.compactMap { id in
            guard let record = records.first(where: { $0.id == id }) else { return nil }
            return TimetableEntity(id: record.id, name: record.name)
        }
    }

    func suggestedEntities() async throws -> [TimetableEntity] {
        let records = WidgetSharedData.loadCatalog()?.timetables ?? []
        return records
            .sorted { $0.sortOrder == $1.sortOrder ? $0.name < $1.name : $0.sortOrder < $1.sortOrder }
            .map { TimetableEntity(id: $0.id, name: $0.name) }
    }
}

@available(iOS 17.0, *)
struct TimetableSelectionIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "widget.configuration.choose"
    static let description = IntentDescription("widget.configuration.description")

    @Parameter(title: "widget.configuration.timetable")
    var timetable: TimetableEntity?

    init() {}
}
