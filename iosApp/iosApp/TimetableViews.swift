import Foundation
import PhotosUI
import SwiftUI
import UIKit
import UniformTypeIdentifiers
import shared

struct OccurrenceSelection: Identifiable {
    let id: String
    let occurrence: CourseOccurrence
}

struct PendingTimetableImport: Identifiable {
    let id = UUID()
    let timetable: Timetable
    let sourceName: String
}

enum EmptyWeekViewMode: String, CaseIterable, Identifiable {
    case defaultView = "default"
    case hidden
    case userImage = "user-image"

    var id: String { rawValue }
}

enum EmptyWeekViewPreferences {
    static let modeKey = "emptyWeekViewMode"
    static let imageDataKey = "emptyWeekViewImageData"
    static let bottomPaddingKey = "emptyWeekViewBottomPadding"
    static let defaultBottomPadding = 24.0
}

struct WeekEmptyStateView: View {
    let mode: EmptyWeekViewMode
    let imageData: Data?
    var tint: Color = .primary

    var body: some View {
        Group {
            if mode == .userImage,
               let imageData,
               let image = UIImage(data: imageData) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 160, height: 160)
                    .opacity(0.72)
            } else {
                Image(systemName: "book")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 72, height: 72)
                    .foregroundColor(tint.opacity(0.24))
            }
        }
        .accessibilityHidden(true)
    }
}

struct EmptyImagePicker: UIViewControllerRepresentable {
    let onSelect: (Data) -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration(photoLibrary: .shared())
        configuration.filter = .images
        configuration.selectionLimit = 1
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        private let parent: EmptyImagePicker

        init(_ parent: EmptyImagePicker) {
            self.parent = parent
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard let provider = results.first?.itemProvider,
                  provider.canLoadObject(ofClass: UIImage.self) else {
                DispatchQueue.main.async {
                    picker.dismiss(animated: true)
                }
                return
            }
            provider.loadObject(ofClass: UIImage.self) { object, _ in
                let data = (object as? UIImage)?.jpegData(compressionQuality: 0.85)
                DispatchQueue.main.async {
                    if let data {
                        self.parent.onSelect(data)
                    }
                    picker.dismiss(animated: true)
                }
            }
        }
    }
}

enum TimetableExportKind {
    case json
    case ics

    var contentType: UTType {
        switch self {
        case .json: return .json
        case .ics: return .sleepDownICS
        }
    }

    var fileExtension: String {
        switch self {
        case .json: return "json"
        case .ics: return "ics"
        }
    }
}

struct TimetableRootView: View {
    @EnvironmentObject private var store: TimetableStore
    @EnvironmentObject private var reminders: ReminderScheduler
    @AppStorage("appearance") private var appearance = "system"
    @AppStorage(AppLanguage.storageKey) private var appLanguage = AppLanguage.system.rawValue
    @State private var widgetRequest: WidgetDeepLink.Request?

    var body: some View {
        NavigationView {
            WeekView(widgetRequest: $widgetRequest)
        }
        .navigationViewStyle(.stack)
        .safeAreaInset(edge: .bottom) {
            if widgetRequest != nil {
                HStack {
                    Text("widget.navigation.pending").font(.caption)
                    Button("widget.navigation.cancel") { widgetRequest = nil }
                }.padding().background(.regularMaterial)
            }
        }
        .preferredColorScheme(preferredColorScheme)
        .environment(\.locale, AppLanguage(rawValue: appLanguage)?.locale ?? AppLanguage.system.locale)
        .onAppear {
            AppLocalization.synchronizeWidgetLanguage(rawValue: appLanguage)
            reminders.refreshAuthorization()
            reminders.schedule(timetable: store.selectedTimetable)
        }
        .onReceive(store.$changeToken) { _ in
            reminders.schedule(timetable: store.selectedTimetable)
        }
        .onReceive(reminders.$authorizationState) { _ in
            reminders.schedule(timetable: store.selectedTimetable)
        }
        .onChange(of: appLanguage) { newValue in
            AppLocalization.synchronizeWidgetLanguage(rawValue: newValue)
            reminders.schedule(timetable: store.selectedTimetable)
        }
        .onOpenURL { url in
            guard let destination = WidgetDeepLink.destination(from: url) else { return }
            widgetRequest = WidgetDeepLink.Request(destination: destination)
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            reminders.supplementPendingRequests()
        }
        .alert(isPresented: Binding(
            get: { store.errorMessage != nil },
            set: { isPresented in
                if !isPresented { store.errorMessage = nil }
            }
        )) {
            Alert(
                title: Text("alert.title"),
                message: Text(verbatim: store.errorMessage ?? ""),
                dismissButton: .default(Text("action.ok")) { store.errorMessage = nil }
            )
        }
    }

    private var preferredColorScheme: ColorScheme? {
        switch appearance {
        case "light": return .light
        case "dark": return .dark
        default: return nil
        }
    }
}

struct WelcomeView: View {
    @EnvironmentObject private var store: TimetableStore
    @State private var showingCreate = false

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Image(systemName: "calendar.badge.plus")
                    .font(.system(size: 64))
                    .foregroundColor(.accentColor)
                Text("welcome.title")
                    .font(.title2.weight(.semibold))
                Text("welcome.subtitle")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
                Button("action.create_first_timetable") {
                    showingCreate = true
                }
                .buttonStyle(DefaultButtonStyle())
            }
            .padding()
            .navigationTitle("SleepDown")
        }
        .sheet(isPresented: $showingCreate) {
            NewTimetableView()
        }
    }
}

struct WeekView: View {
    @EnvironmentObject private var store: TimetableStore
    @Binding var widgetRequest: WidgetDeepLink.Request?
    @State private var requestedEpochDay: Int64?
    @State private var week: Int32 = 1
    @State private var didInitializeWeek = false
    @State private var showingNewTimetable = false
    @State private var newCourseSeed: GridCourseSeed?
    @State private var editingCourseID: String?
    @State private var selectedOccurrence: OccurrenceSelection?
    @State private var showingImporter = false
    @State private var showingExporter = false
    @State private var exportDocument = TimetableFileDocument()
    @State private var exportContentType: UTType = .json
    @State private var exportFilename = ""
    @State private var pendingImport: PendingTimetableImport?
    @State private var pendingImportDestination: TimetableImportDestination?
    @State private var showingImportChoices = false
    @State private var showingImportConfirmation = false
    @State private var showingWeeks = false
    @State private var pendingMove: IOSCourseMovePlan?
    @State private var showingMoveScope = false
    @State private var destination: SecondaryPage?

    private enum SecondaryPage { case management, timetable, courses, schedules, appearance, settings, widgets }

    init(widgetRequest: Binding<WidgetDeepLink.Request?> = .constant(nil)) {
        self._widgetRequest = widgetRequest
    }

    // External navigation waits for explicit completion of drafts, imports and secondary pages.
    private var canOpenWidgetRequest: Bool {
        destination == nil && !showingNewTimetable && newCourseSeed == nil && editingCourseID == nil
            && selectedOccurrence == nil && !showingImporter && !showingExporter
            && !showingImportChoices && !showingImportConfirmation && !showingWeeks && pendingMove == nil
    }

    private func openWidgetRequestIfReady() {
        guard canOpenWidgetRequest, let request = widgetRequest else { return }
        widgetRequest = nil
        if let tableID = request.destination.timetableID {
            guard store.timetables.contains(where: { $0.id == tableID }) else {
                store.errorMessage = AppLocalization.string("widget.no_timetable.found")
                return
            }
            store.selectTimetable(tableID)
        }
        let day = request.destination.epochDay ?? TimetableDates.epochDay(for: Date())
        if let table = store.selectedTimetable {
            requestedEpochDay = min(max(day, table.firstDayEpochDay), table.firstDayEpochDay + Int64(table.maxWeek) * 7 - 1)
        } else { requestedEpochDay = day }
        didInitializeWeek = false
        syncWeek()
    }

    var body: some View {
        Group {
            if let timetable = store.selectedTimetable {
                timetableContent(timetable)
            } else {
                VStack(spacing: 0) {
                    HStack {
                        Image(systemName: "calendar").font(.title2)
                        Text("SleepDown").font(.title3.bold())
                        Spacer()
                        Button { showingNewTimetable = true } label: { Image(systemName: "plus").frame(width: 44, height: 44) }
                        Button { showingImporter = true } label: { Image(systemName: "arrow.down.to.line").frame(width: 44, height: 44) }
                        Menu {
                            Button("navigation.timetable_management") { destination = .management }
                            Button("navigation.schedule") { destination = .schedules }
                            Button("tab.settings") { destination = .settings }
                            Button("navigation.widgets") { destination = .widgets }
                        } label: { Image(systemName: "ellipsis").frame(width: 44, height: 44) }
                        .accessibilityLabel("timetable.actions")
                    }.padding(.horizontal, 16).foregroundColor(.primary)
                    Spacer()
                    Button("action.create_first_timetable") { showingNewTimetable = true }
                        .buttonStyle(.borderedProminent)
                    Spacer()
                }
                .background(SchedulePalette.background.ignoresSafeArea())
            }
        }
        .background {
            NavigationLink(isActive: Binding(
                get: { destination != nil },
                set: { if !$0 { destination = nil } }
            )) {
                secondaryPage
            } label: { EmptyView() }
            .hidden()
        }
        .navigationBarHidden(true)
        .onAppear { syncWeek(); openWidgetRequestIfReady() }
        .onChange(of: widgetRequest?.id) { _ in openWidgetRequestIfReady() }
        .onChange(of: canOpenWidgetRequest) { _ in openWidgetRequestIfReady() }
        .onChange(of: store.selectedTimetableID) { _ in
            pendingMove = nil
            didInitializeWeek = false
            syncWeek()
        }
        .confirmationDialog("move.scope.title", isPresented: $showingMoveScope, titleVisibility: .visible, presenting: pendingMove) { plan in
            Button("move.scope.this_week") { commitMove(plan, allWeeks: false) }
            Button("move.scope.all_weeks") { commitMove(plan, allWeeks: true) }
            // Popover presentations hide cancel-role actions; preview rollback stays explicit.
            Button("action.cancel") { pendingMove = nil }
        }
        .onChange(of: showingMoveScope) { visible in
            if !visible { pendingMove = nil }
        }
        .sheet(isPresented: $showingWeeks) {
            if let timetable = store.selectedTimetable {
                WeekChooserView(maxWeek: Int(timetable.maxWeek), selected: Int(week), current: Int(currentWeek(for: timetable))) { selected in
                    week = Int32(selected)
                    showingWeeks = false
                }
            }
        }
        .onChange(of: requestedEpochDay) { _ in
            didInitializeWeek = false
            syncWeek()
        }
        .onReceive(store.$changeToken) { _ in syncWeek() }
        .sheet(isPresented: $showingNewTimetable) {
            NewTimetableView()
        }
        .sheet(item: $newCourseSeed) { seed in
            if let timetable = store.selectedTimetable {
                CourseDraftView(timetable: timetable, seed: seed) { course in
                    store.addCourse(course, to: timetable)
                }
            }
        }
        .sheet(isPresented: Binding(
            get: { editingCourseID != nil },
            set: { if !$0 { editingCourseID = nil } }
        )) {
            if let courseID = editingCourseID,
               let timetable = store.selectedTimetable,
               let course = timetable.courses.first(where: { $0.id == courseID }) {
                CourseDraftView(timetable: timetable, course: course) { updated in
                    store.updateCourse(updated, in: timetable)
                }
            }
        }
        .sheet(item: $selectedOccurrence) { selection in
            if let timetable = store.selectedTimetable {
                OccurrenceDetailView(
                    occurrence: selection.occurrence,
                    timetable: timetable,
                    onEditCourse: { courseID in
                        selectedOccurrence = nil
                        editingCourseID = courseID
                    },
                    onDismiss: { selectedOccurrence = nil }
                ).modifier(CourseDetailPresentation())
            }
        }
        .fileImporter(
            isPresented: $showingImporter,
            allowedContentTypes: TimetableFileDocument.readableContentTypes
        ) { result in
            handleImport(result)
        }
        .fileExporter(
            isPresented: $showingExporter,
            document: exportDocument,
            contentType: exportContentType,
            defaultFilename: exportFilename
        ) { result in
            if case .failure(let error) = result {
                store.errorMessage = AppLocalization.string(
                    "error.export_timetable",
                    error.localizedDescription
                )
            }
        }
        .actionSheet(isPresented: $showingImportChoices) {
            ActionSheet(
                title: Text("import.title"),
                message: Text(verbatim: pendingImport?.sourceName ?? ""),
                buttons: [
                    .default(Text("import.create")) { confirmImport(.create) },
                    .default(Text("import.merge")) { confirmImport(.merge) },
                    .destructive(Text("import.replace")) { confirmImport(.replace) },
                    .cancel()
                ]
            )
        }
.alert(isPresented: $showingImportConfirmation) {
            Alert(
                title: Text(verbatim: importConfirmationTitle),
                message: Text(verbatim: importConfirmationMessage),
                primaryButton: importConfirmationButton,
                secondaryButton: .cancel()
            )
        }
    }

    @ViewBuilder
    private func timetableContent(_ timetable: Timetable) -> some View {
        let style = IOSScheduleStyle.read(UserDefaults.standard.data(forKey: "schedule.\(timetable.id).style") ?? Data())
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Button { showingWeeks = true } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "calendar").font(.system(size: 23, weight: .semibold))
                        VStack(alignment: .leading, spacing: 7) {
                            Text(timetable.name).font(.system(size: 19, weight: .bold)).lineLimit(1)
                            Text(AppLocalization.string("week.number", String(week)) + " · " + weekTitle(for: timetable, week: week))
                                .font(.system(size: 11)).lineLimit(1)
                            GeometryReader { geometry in
                                Capsule().fill(Color.primary.opacity(0.15))
                                    .overlay(alignment: .leading) {
                                        Capsule().fill(SchedulePalette.accent)
                                            .frame(width: max(6, geometry.size.width * CGFloat(week) / CGFloat(max(timetable.maxWeek, 1))))
                                    }
                            }.frame(height: 3)
                        }
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }.accessibilityLabel("label.week_selector")
                Button {
                    newCourseSeed = GridCourseSeed(day: 1, start: 1, count: min(2, Int(timetable.timeTable.nodes.count)))
                } label: { Image(systemName: "plus").font(.system(size: 24)).frame(width: 44, height: 48) }
                    .accessibilityLabel("action.add_course")
                Button { showingImporter = true } label: {
                    Image(systemName: "arrow.down.to.line").font(.system(size: 22, weight: .bold)).frame(width: 44, height: 48)
                }.accessibilityLabel("file.import")
                Menu {
                    Button("export.json_backup") { prepareExport(.json, timetable: timetable) }
                    Button("export.ics_calendar") { prepareExport(.ics, timetable: timetable) }
                } label: {
                    Image(systemName: "square.and.arrow.up").font(.system(size: 20)).frame(width: 40, height: 48)
                }.accessibilityLabel("file.export")
                Menu {
                    Menu("label.timetable") {
                        ForEach(store.timetables, id: \.id) { item in
                            Button(item.name) { store.selectTimetable(item.id) }
                        }
                    }
                    Button("navigation.timetable_management") { destination = .management }
                    Button("navigation.timetable_settings") { destination = .timetable }
                    Button("navigation.course_management") { destination = .courses }
                    Button("navigation.schedule") { destination = .schedules }
                    Button("settings.appearance") { destination = .appearance }
                    Button("tab.settings") { destination = .settings }
                    Button("navigation.widgets") { destination = .widgets }
                    Menu("file.export") {
                        Button("export.json_backup") { prepareExport(.json, timetable: timetable) }
                        Button("export.ics_calendar") { prepareExport(.ics, timetable: timetable) }
                    }
                    Button("action.new_timetable") { showingNewTimetable = true }
                } label: { Image(systemName: "ellipsis").rotationEffect(.degrees(90)).font(.system(size: 23, weight: .bold)).frame(width: 36, height: 48) }
                    .accessibilityLabel("timetable.actions")
            }
            .foregroundColor(style.interfaceColor == 0 ? .primary : Color.sharedCourse(style.interfaceColor))
            .padding(.horizontal, 12).padding(.top, 8).padding(.bottom, 12)

            ScheduleWeekGrid(
                timetable: pendingMove?.preview ?? timetable,
                week: week,
                onSelect: { selectedOccurrence = OccurrenceSelection(id: $0.id, occurrence: $0) },
                onAdd: { newCourseSeed = $0 },
                onMove: { occurrence, day, node in moveOccurrence(occurrence, to: day, startNode: node, in: timetable) },
                onFlip: { direction in
                    if direction > 0 { nextWeek() } else { previousWeek() }
                }
            )
            .id(timetable.id)
        }
        .background(SchedulePalette.background.ignoresSafeArea())
    }

    @ViewBuilder
    private var secondaryPage: some View {
        Group {
            switch destination {
            case .management: TimetableManagementView()
            case .schedules: ReusableTimeTableView()
            case .settings: SettingsView()
            case .widgets: WidgetHelpView()
            case .timetable:
                if let table = store.selectedTimetable { TimetableEditView(timetable: table) }
            case .courses:
                if let table = store.selectedTimetable { CourseManagementView(tableID: table.id) }
            case .appearance:
                if let table = store.selectedTimetable { ScheduleAppearanceView(tableID: table.id) }
            case nil: EmptyView()
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarHidden(false)
    }

    private func syncWeek() {
        guard let timetable = store.selectedTimetable else { return }
        let maxWeek = max(timetable.maxWeek, 1)
        if !didInitializeWeek {
            let targetEpochDay = requestedEpochDay ?? TimetableDates.epochDay(for: Date())
            week = TimetableDates.week(
                forEpochDay: targetEpochDay,
                firstDayEpochDay: timetable.firstDayEpochDay
            )
            didInitializeWeek = true
        }
        week = min(max(week, 1), maxWeek)
    }

    private func currentWeek(for timetable: Timetable) -> Int32 {
        TimetableDates.week(
            forEpochDay: TimetableDates.epochDay(for: Date()),
            firstDayEpochDay: timetable.firstDayEpochDay
        )
    }

    private func goToCurrentWeek(_ timetable: Timetable) {
        week = min(max(currentWeek(for: timetable), 1), max(timetable.maxWeek, 1))
        didInitializeWeek = true
    }

    private func moveOccurrence(
        _ occurrence: CourseOccurrence,
        to targetEpochDay: Int64,
        startNode: Int32,
        in timetable: Timetable
    ) {
        guard targetEpochDay != occurrence.epochDay || startNode != occurrence.startNode else { return }
        do {
            pendingMove = try IOSCourseMovePlan(timetable: timetable, occurrence: occurrence, targetEpochDay: targetEpochDay, targetStartNode: startNode)
            showingMoveScope = true
        } catch {
            store.errorMessage = error.localizedDescription
        }
    }

    private func commitMove(_ plan: IOSCourseMovePlan, allWeeks: Bool) {
        do {
            let updated = try plan.result(allWeeks: allWeeks)
            _ = store.save(updated)
        } catch {
            store.errorMessage = error.localizedDescription
        }
        pendingMove = nil
    }

    private func previousWeek() {
        week = max(week - 1, 1)
    }

    private func nextWeek() {
        guard let maxWeek = store.selectedTimetable?.maxWeek else { return }
        week = min(week + 1, maxWeek)
    }

    private func weekTitle(for timetable: Timetable, week: Int32) -> String {
        let range = TimetableDates.range(for: timetable, week: week)
        let formatter = DateFormatter()
        formatter.locale = AppLocalization.currentLocale
        formatter.setLocalizedDateFormatFromTemplate("MMMd")
        return AppLocalization.string(
            "week.date_range",
            formatter.string(from: TimetableDates.displayDate(forEpochDay: range.startEpochDay)),
            formatter.string(from: TimetableDates.displayDate(forEpochDay: range.endEpochDay))
        )
    }

    private func prepareExport(_ kind: TimetableExportKind, timetable: Timetable) {
        let range = TimetableDates.range(for: timetable, week: week)
        do {
            switch kind {
            case .json:
                exportDocument = TimetableFileDocument(data: try TimetableDocumentCodec.encodeJSON(timetable))
                exportFilename = timetable.name.isEmpty
                    ? AppLocalization.string("file.timetable_json")
                    : "\(timetable.name).json"
            case .ics:
                exportDocument = TimetableFileDocument(data: try TimetableDocumentCodec.encodeICS(timetable, range: range))
                exportFilename = AppLocalization.string(
                    "file.week_ics",
                    timetable.name,
                    String(week)
                )
            }
            exportContentType = kind.contentType
            showingExporter = true
        } catch {
            store.errorMessage = AppLocalization.string("error.operation", error.localizedDescription)
        }
    }

    private func handleImport(_ result: Result<URL, Error>) {
        guard case .success(let url) = result else {
            if case .failure(let error) = result { store.errorMessage = error.localizedDescription }
            return
        }
        let definition = store.timeTableDefinitions.first(where: { $0.id == store.selectedTimetable?.timeTable.id })
            ?? store.timeTableDefinitions.first ?? ReusableTimeTable.defaultDefinition()

        let accessing = url.startAccessingSecurityScopedResource()
        defer {
            if accessing { url.stopAccessingSecurityScopedResource() }
        }
        do {
            let data = try Data(contentsOf: url)
            let imported = try TimetableDocumentCodec.decode(
                data: data,
                filename: url.lastPathComponent,
                defaultTimeTable: definition
            )
            pendingImport = PendingTimetableImport(
                timetable: imported,
                sourceName: url.lastPathComponent
            )
            if store.selectedTimetable == nil { confirmImport(.create) }
            else { showingImportChoices = true }
        } catch {
            store.errorMessage = AppLocalization.string("error.operation", error.localizedDescription)
        }
    }

    private func confirmImport(_ destination: TimetableImportDestination) {
        pendingImportDestination = destination
        showingImportConfirmation = true
    }

    private var importConfirmationTitle: String {
        switch pendingImportDestination ?? .create {
        case .create: return AppLocalization.string("import.confirm.create.title")
        case .merge: return AppLocalization.string("import.confirm.merge.title")
        case .replace: return AppLocalization.string("import.confirm.replace.title")
        }
    }

    private var importConfirmationMessage: String {
        switch pendingImportDestination ?? .create {
        case .create:
            return AppLocalization.string("import.confirm.create.message")
        case .merge:
            return AppLocalization.string("import.confirm.merge.message")
        case .replace:
            return AppLocalization.string("import.confirm.replace.message")
        }
    }

    private var importConfirmationButton: Alert.Button {
        switch pendingImportDestination ?? .create {
        case .create:
            return .default(Text("action.import_create")) { commitImport(.create) }
        case .merge:
            return .default(Text("action.import_merge")) { commitImport(.merge) }
        case .replace:
            return .destructive(Text("action.import_replace")) { commitImport(.replace) }
        }
    }

    private func commitImport(_ destination: TimetableImportDestination) {
        guard let pendingImport else { return }
        guard store.importTimetable(pendingImport.timetable, destination: destination) else { return }
        self.pendingImport = nil
        pendingImportDestination = nil
        showingImportConfirmation = false
    }
}

struct WeekCalendarView: View {
    let timetable: Timetable
    let week: Int32
    let onSelect: (CourseOccurrence) -> Void
    let onMove: (CourseOccurrence, Int64, Int32) -> Void

    @AppStorage(EmptyWeekViewPreferences.modeKey) private var emptyViewMode = EmptyWeekViewMode.defaultView.rawValue
    @AppStorage(EmptyWeekViewPreferences.bottomPaddingKey) private var emptyViewBottomPadding = EmptyWeekViewPreferences.defaultBottomPadding
    @State private var emptyViewImageData: Data?

    private let timeAxisWidth: CGFloat = 54
    private let dayWidth: CGFloat = 142

    var body: some View {
        let range = TimetableDates.range(for: timetable, week: week)
        let occurrences = TimetableEngine.shared.expandOccurrences(timetable: timetable, range: range)
        let placements = TimetableEngine.shared.gridPlacements(
            occurrences: occurrences,
            timeTable: timetable.timeTable,
            preferences: timetable.conflictPreferences
        )
        let conflictCounts = TimetableEngine.shared
            .conflictGroups(occurrences: occurrences, preferences: timetable.conflictPreferences)
            .reduce(into: [String: Int]()) { result, group in
                for occurrence in group.occurrences {
                    result[occurrence.id] = group.occurrences.count
                }
            }
        let days = visibleDays(start: range.startEpochDay)
        let gridHeight = CGFloat(
            max(timetable.timeTable.gridEndMinuteOfDay - timetable.timeTable.gridStartMinuteOfDay, 60)
        ) / 60 * 72

        return ScrollView([.horizontal, .vertical], showsIndicators: true) {
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    Color.clear.frame(width: timeAxisWidth, height: 36)
                    ForEach(days, id: \.self) { epochDay in
                        DayHeaderView(epochDay: epochDay)
                            .frame(width: dayWidth, height: 36)
                    }
                }
                HStack(alignment: .top, spacing: 0) {
                    TimeAxisView(timeTable: timetable.timeTable)
                        .frame(width: timeAxisWidth)
                    ZStack {
                        HStack(alignment: .top, spacing: 0) {
                            ForEach(Array(days.enumerated()), id: \.element) { dayIndex, epochDay in
                                DayColumnView(
                                    epochDay: epochDay,
                                    dayIndex: dayIndex,
                                    visibleDays: days,
                                    timetable: timetable,
                                    occurrences: occurrences.filter { $0.epochDay == epochDay },
                                    placements: placements,
                                    conflictCounts: conflictCounts,
                                    width: dayWidth,
                                    onSelect: onSelect,
                                    onMove: onMove
                                )
                            }
                        }
                        if occurrences.isEmpty,
                           EmptyWeekViewMode(rawValue: emptyViewMode) != .hidden {
                            WeekEmptyStateView(
                                mode: EmptyWeekViewMode(rawValue: emptyViewMode) ?? .defaultView,
                                imageData: emptyViewImageData
                            )
                            .frame(width: dayWidth * CGFloat(days.count), height: min(gridHeight, 240))
                            .allowsHitTesting(false)
                        }
                    }
                }
            }
            .padding(.bottom, CGFloat(max(emptyViewBottomPadding, 0)))
        }
        .background(Color(UIColor.systemGroupedBackground))
        .onAppear { refreshEmptyViewImage() }
        .onReceive(NotificationCenter.default.publisher(for: UserDefaults.didChangeNotification)) { _ in
            refreshEmptyViewImage()
        }
    }

    private func refreshEmptyViewImage() {
        emptyViewImageData = UserDefaults.standard.data(forKey: EmptyWeekViewPreferences.imageDataKey)
    }

    private func visibleDays(start: Int64) -> [Int64] {
        let allDays = (0..<7).map { start + Int64($0) }
        let orderedDays = timetable.sundayFirst
            ? [allDays[6]] + Array(allDays[0..<6])
            : allDays
        return orderedDays.filter { epochDay in
            let day = TimetableDates.dayOfWeek(forEpochDay: epochDay, firstDayEpochDay: start)
            if day == 6 { return timetable.showSaturday }
            if day == 7 { return timetable.showSunday }
            return true
        }
    }
}

struct DayHeaderView: View {
    let epochDay: Int64

    var body: some View {
        VStack(spacing: 1) {
            Text(weekdayName)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(dayNumber)
                .font(.subheadline.weight(.semibold))
        }
        .frame(maxWidth: .infinity)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .overlay(Rectangle().stroke(Color.gray.opacity(0.15), lineWidth: 0.5))
    }

    private var weekdayName: String {
        let formatter = DateFormatter()
        formatter.locale = AppLocalization.currentLocale
        return formatter.shortWeekdaySymbols[Calendar.current.component(.weekday, from: TimetableDates.displayDate(forEpochDay: epochDay)) - 1]
    }

    private var dayNumber: String {
        let formatter = DateFormatter()
        formatter.locale = AppLocalization.currentLocale
        formatter.setLocalizedDateFormatFromTemplate("Md")
        return formatter.string(from: TimetableDates.displayDate(forEpochDay: epochDay))
    }
}

private func weekdayName(for day: Int) -> String {
    switch day {
    case 1: return AppLocalization.string("weekday.monday")
    case 2: return AppLocalization.string("weekday.tuesday")
    case 3: return AppLocalization.string("weekday.wednesday")
    case 4: return AppLocalization.string("weekday.thursday")
    case 5: return AppLocalization.string("weekday.friday")
    case 6: return AppLocalization.string("weekday.saturday")
    case 7: return AppLocalization.string("weekday.sunday")
    default: return ""
    }
}

struct TimeAxisView: View {
    let timeTable: ScheduleTimeTable
    private let rowHeight: CGFloat = 72

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(timeTable.nodes.enumerated()), id: \.offset) { index, node in
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(node.node)")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Text(MinuteOfDay.shared.format(minuteOfDay: node.startMinuteOfDay))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, minHeight: rowHeight, alignment: .topTrailing)
                .padding(.trailing, 6)
                if index == timeTable.nodes.count - 1 {
                    Text(MinuteOfDay.shared.format(minuteOfDay: node.endMinuteOfDay))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                        .padding(.trailing, 6)
                }
            }
        }
        .frame(height: gridHeight)
    }

    private var gridHeight: CGFloat {
        CGFloat(max(timeTable.gridEndMinuteOfDay - timeTable.gridStartMinuteOfDay, 60)) / 60 * rowHeight
    }
}

struct DayColumnView: View {
    let epochDay: Int64
    let dayIndex: Int
    let visibleDays: [Int64]
    let timetable: Timetable
    let occurrences: [CourseOccurrence]
    let placements: [String: GridPlacement]
    let conflictCounts: [String: Int]
    let width: CGFloat
    let onSelect: (CourseOccurrence) -> Void
    let onMove: (CourseOccurrence, Int64, Int32) -> Void

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .topLeading) {
                Color(UIColor.systemBackground)
                gridLines
                ForEach(occurrences.sorted { $0.startMinuteOfDay < $1.startMinuteOfDay }, id: \.id) { occurrence in
                    CourseCardView(
                        occurrence: occurrence,
                        placement: placements[occurrence.id],
                        timetable: timetable,
                        conflictCount: conflictCounts[occurrence.id] ?? 1,
                        width: geometry.size.width,
                        height: gridHeight,
                        dayIndex: dayIndex,
                        visibleDays: visibleDays,
                        onSelect: onSelect,
                        onMove: onMove
                    )
                }
            }
        }
        .frame(width: width, height: gridHeight)
        .overlay(Rectangle().stroke(Color.gray.opacity(0.2), lineWidth: 0.5))
    }

    private var gridLines: some View {
        VStack(spacing: 0) {
            ForEach(0..<numberOfRows, id: \.self) { _ in
                Rectangle()
                    .fill(Color.gray.opacity(0.12))
                    .frame(height: 1)
                Spacer(minLength: rowHeight - 1)
            }
            Rectangle()
                .fill(Color.gray.opacity(0.12))
                .frame(height: 1)
        }
    }

    private var numberOfRows: Int {
        max(Int(ceil(Double(timetable.timeTable.gridEndMinuteOfDay - timetable.timeTable.gridStartMinuteOfDay) / 60)), 1)
    }

    private var rowHeight: CGFloat { 72 }

    private var gridHeight: CGFloat {
        CGFloat(max(timetable.timeTable.gridEndMinuteOfDay - timetable.timeTable.gridStartMinuteOfDay, 60)) / 60 * rowHeight
    }
}

struct CourseCardView: View {
    let occurrence: CourseOccurrence
    let placement: GridPlacement?
    let timetable: Timetable
    let conflictCount: Int
    let width: CGFloat
    let height: CGFloat
    let dayIndex: Int
    let visibleDays: [Int64]
    let onSelect: (CourseOccurrence) -> Void
    let onMove: (CourseOccurrence, Int64, Int32) -> Void

    @State private var dragOffset: CGSize = .zero
    @State private var isDragging = false
    private let rowHeight: CGFloat = 72

    var body: some View {
        let duration = max(timetable.timeTable.gridEndMinuteOfDay - timetable.timeTable.gridStartMinuteOfDay, 60)
        let fallbackTop = Double(occurrence.startMinuteOfDay - timetable.timeTable.gridStartMinuteOfDay) / Double(duration)
        let fallbackHeight = Double(max(occurrence.endMinuteOfDay - occurrence.startMinuteOfDay, 30)) / Double(duration)
        let top = CGFloat(placement?.topFraction ?? fallbackTop) * height
        let cardHeight = max(CGFloat(placement?.heightFraction ?? fallbackHeight) * height, 42)
        let cardWidth = max((placement?.widthFraction ?? 1) * width - 4, 58)
        let left = CGFloat(placement?.leftFraction ?? 0) * width + 2

        VStack(alignment: .leading, spacing: 2) {
            Text(occurrence.courseName)
                .font(.caption.weight(.semibold))
                .lineLimit(2)
            if !occurrence.room.isEmpty {
                Text(occurrence.room)
                    .font(.caption2)
                    .lineLimit(1)
            }
            if cardHeight > 64 {
                Text(verbatim: AppLocalization.string(
                    "time_range",
                    MinuteOfDay.shared.format(minuteOfDay: occurrence.startMinuteOfDay),
                    MinuteOfDay.shared.format(minuteOfDay: occurrence.endMinuteOfDay)
                ))
                    .font(.caption2)
                    .lineLimit(1)
            }
            if occurrence.isRescheduled {
                Text("course.rescheduled")
                    .font(.caption2.weight(.medium))
            }
            if conflictCount > 1 {
                Label(
                    AppLocalization.string("course.conflict", String(conflictCount)),
                    systemImage: "exclamationmark.triangle.fill"
                )
                    .font(.caption2.weight(.medium))
            }
        }
        .foregroundColor(.white)
        .padding(6)
        .frame(width: cardWidth, height: cardHeight - 4, alignment: .topLeading)
        .background(Color.sharedCourse(occurrence.color))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        .contentShape(Rectangle())
        .onTapGesture {
            if !isDragging { onSelect(occurrence) }
        }
        .simultaneousGesture(moveGesture)
        .accessibilityLabel(occurrence.courseName)
        .accessibilityHint(AppLocalization.string("course.drag_hint"))
        .accessibilityAddTraits(.isButton)
        .scaleEffect(isDragging ? 1.03 : 1)
        .zIndex(isDragging ? 1 : 0)
        .offset(
            x: left + (isDragging ? dragOffset.width : 0),
            y: top + 2 + (isDragging ? dragOffset.height : 0)
        )
        .animation(.easeOut(duration: 0.12), value: isDragging)
    }

    private var moveGesture: some Gesture {
        LongPressGesture(minimumDuration: 0.35, maximumDistance: 12)
            .sequenced(before: DragGesture(minimumDistance: 4))
            .onChanged { value in
                if case .second(_, let drag) = value {
                    isDragging = drag != nil
                    dragOffset = drag?.translation ?? .zero
                } else {
                    isDragging = true
                }
            }
            .onEnded { value in
                defer {
                    isDragging = false
                    dragOffset = .zero
                }
                guard case .second(true, let drag) = value, let drag,
                      !visibleDays.isEmpty else { return }

                let dayShift = Int((drag.translation.width / max(width, 1)).rounded())
                let targetIndex = min(
                    max(dayIndex + dayShift, 0),
                    visibleDays.count - 1
                )
                let lastNode = timetable.timeTable.nodes.map(\.node).max() ?? 1
                let nodeCount = min(max(occurrence.nodeCount, 1), lastNode)
                let rowShift = Int32((drag.translation.height / rowHeight).rounded())
                let maxStartNode = max(1, lastNode - nodeCount + 1)
                let targetStartNode = min(
                    max(occurrence.startNode + rowShift, 1),
                    maxStartNode
                )
                onMove(occurrence, visibleDays[targetIndex], targetStartNode)
            }
    }
}

struct OccurrenceDetailView: View {
    enum DeleteScope: String, CaseIterable, Identifiable {
        case occurrence
        case slot
        case fromWeek
        case course

        var id: String { rawValue }
        var title: String {
            switch self {
            case .occurrence: return AppLocalization.string("delete.scope.occurrence")
            case .slot: return AppLocalization.string("delete.scope.slot")
            case .fromWeek: return AppLocalization.string("delete.scope.from_week")
            case .course: return AppLocalization.string("delete.scope.course")
            }
        }
    }

    @EnvironmentObject private var store: TimetableStore
    @Environment(\.presentationMode) private var presentationMode
    let occurrence: CourseOccurrence
    let timetable: Timetable
    let onEditCourse: (String) -> Void
    let onDismiss: () -> Void

    @State private var selectedOccurrenceID: String
    @State private var deleteScope: DeleteScope = .occurrence
    @State private var showingDeleteConfirmation = false
    @State private var showingReschedule = false

    init(
        occurrence: CourseOccurrence,
        timetable: Timetable,
        onEditCourse: @escaping (String) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.occurrence = occurrence
        self.timetable = timetable
        self.onEditCourse = onEditCourse
        self.onDismiss = onDismiss
        _selectedOccurrenceID = State(initialValue: occurrence.id)
    }

    var body: some View {
        let currentOccurrence = selectedOccurrence
        let conflictOccurrences = allConflictOccurrences

        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                if conflictOccurrences.count > 1 {
                    Section(header: Text("section.conflict_occurrences")) {
                        Picker("label.conflict_occurrence", selection: $selectedOccurrenceID) {
                            ForEach(conflictOccurrences, id: \.id) { item in
                                Text(verbatim: occurrenceSummary(item)).tag(item.id)
                            }
                        }
                        Text(verbatim: AppLocalization.string(
                            "conflict.count",
                            String(conflictOccurrences.count)
                        ))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Text(currentOccurrence.courseName).font(.title2.bold())
                Text(occurrenceDateTitle(currentOccurrence)).font(.subheadline).foregroundColor(.secondary)
                Text(AppLocalization.string(
                    "slot.nodes",
                    AppLocalization.string("time_range", MinuteOfDay.shared.format(minuteOfDay: currentOccurrence.startMinuteOfDay), MinuteOfDay.shared.format(minuteOfDay: currentOccurrence.endMinuteOfDay)),
                    String(currentOccurrence.startNode), String(currentOccurrence.startNode + currentOccurrence.nodeCount - 1)
                ))
                if !currentOccurrence.teacher.isEmpty {
                    Text(AppLocalization.string("label.teacher") + ": " + currentOccurrence.teacher)
                }
                if !currentOccurrence.room.isEmpty {
                    Text(AppLocalization.string("label.room") + ": " + currentOccurrence.room)
                }
                if !currentOccurrence.note.isEmpty {
                    Text(AppLocalization.string("field.note") + ": " + currentOccurrence.note)
                }
                if currentOccurrence.isRescheduled {
                    Label("course.rescheduled.detail", systemImage: "arrow.right.arrow.left")
                        .font(.caption).foregroundColor(.secondary)
                }

                Divider()
                HStack(spacing: 8) {
                    Button { onEditCourse(currentOccurrence.courseId) } label: {
                        Text("action.edit").frame(maxWidth: .infinity).padding(.vertical, 12)
                            .foregroundColor(.white).background(SchedulePalette.accent).clipShape(Capsule())
                    }.accessibilityLabel("action.edit_course")
                    Button { showingReschedule = true } label: {
                        Text("action.move").frame(maxWidth: .infinity).padding(.vertical, 12)
                            .foregroundColor(SchedulePalette.accent).overlay(Capsule().stroke(Color.secondary))
                    }.accessibilityLabel("action.reschedule")
                    Button("action.delete", role: .destructive) { showingDeleteConfirmation = true }
                        .foregroundColor(.red).frame(maxWidth: .infinity)
                }.buttonStyle(.plain)
                if conflictOccurrences.count > 1 {
                    Section {
                        Button("action.prefer_conflict") { _ = store.setPreferredOccurrence(currentOccurrence, in: timetable) }
                    }
                }
                }.frame(maxWidth: .infinity, alignment: .leading).padding(20)
            }
            .navigationTitle("navigation.course_detail")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("action.done") { close() }
                }
            }
        }
        .sheet(isPresented: $showingReschedule) {
            RescheduleOccurrenceView(occurrence: currentOccurrence, timetable: timetable) {
                showingReschedule = false
                close()
            }
        }
        .sheet(isPresented: $showingDeleteConfirmation) {
            NavigationView {
                VStack(spacing: 16) {
                    ForEach(DeleteScope.allCases) { scope in
                        Button { deleteScope = scope } label: {
                            HStack {
                                Image(systemName: deleteScope == scope ? "largecircle.fill.circle" : "circle")
                                Text(scope.title).foregroundColor(.primary)
                                Spacer()
                            }.padding(12)
                        }
                    }
                    Button("action.delete", role: .destructive) {
                        if store.apply(deleteCommand(for: currentOccurrence), to: timetable) {
                            showingDeleteConfirmation = false
                            close()
                        }
                    }.buttonStyle(.borderedProminent).tint(.red)
                    Spacer()
                }.padding()
                .navigationTitle("alert.confirm_delete").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("action.cancel") { showingDeleteConfirmation = false } } }
            }.navigationViewStyle(.stack)
        }
    }

    private func occurrenceDateTitle(_ item: CourseOccurrence) -> String {
        let formatter = DateFormatter()
        formatter.locale = AppLocalization.currentLocale
        formatter.setLocalizedDateFormatFromTemplate("MMMd")
        return formatter.string(from: TimetableDates.displayDate(forEpochDay: item.epochDay)) + " · " + weekdayName(for: Int(item.dayOfWeek))
    }

    private var allConflictOccurrences: [CourseOccurrence] {
        let occurrences = TimetableEngine.shared.expandOccurrences(timetable: timetable)
        return TimetableEngine.shared
            .conflictGroups(occurrences: occurrences, preferences: timetable.conflictPreferences)
            .first(where: { group in
                group.occurrences.contains(where: { $0.id == occurrence.id })
            })?.occurrences ?? [occurrence]
    }

    private var selectedOccurrence: CourseOccurrence {
        allConflictOccurrences.first(where: { $0.id == selectedOccurrenceID }) ?? occurrence
    }

    private func occurrenceSummary(_ item: CourseOccurrence) -> String {
        let formatter = DateFormatter()
        formatter.locale = AppLocalization.currentLocale
        formatter.setLocalizedDateFormatFromTemplate("MMMd")
        return AppLocalization.string(
            "conflict.occurrence_summary",
            formatter.string(from: TimetableDates.displayDate(forEpochDay: item.epochDay)),
            AppLocalization.string(
                "time_range",
                MinuteOfDay.shared.format(minuteOfDay: item.startMinuteOfDay),
                MinuteOfDay.shared.format(minuteOfDay: item.endMinuteOfDay)
            )
        )
    }

    private func deleteCommand(for item: CourseOccurrence) -> TimetableCommand {
        switch deleteScope {
        case .occurrence:
            return TimetableCommandDeleteOccurrence(
                logicalSlotId: item.logicalSlotId,
                originalEpochDay: item.originalEpochDay,
                recurrenceSegmentId: item.recurrenceSegmentId
            )
        case .slot:
            return TimetableCommandDeleteLogicalSlot(logicalSlotId: item.logicalSlotId)
        case .fromWeek:
            let segmentID = item.recurrenceSegmentId
            if !segmentID.isEmpty {
                return TimetableCommandDeleteRecurrenceSegmentFromWeek(
                    logicalSlotId: item.logicalSlotId,
                    recurrenceSegmentId: segmentID,
                    fromWeek: item.week
                )
            }
            return TimetableCommandDeleteLogicalSlotFromWeek(
                logicalSlotId: item.logicalSlotId,
                fromWeek: item.week
            )
        case .course:
            return TimetableCommandDeleteCourse(courseId: item.courseId)
        }
    }

    private func close() {
        onDismiss()
        presentationMode.wrappedValue.dismiss()
    }
}

struct RescheduleOccurrenceView: View {
    @EnvironmentObject private var store: TimetableStore
    @Environment(\.presentationMode) private var presentationMode
    let occurrence: CourseOccurrence
    let timetable: Timetable
    let onSaved: () -> Void

    @State private var targetDate: Date
    @State private var targetStartNode: Int32
    @State private var targetCustomStart: String
    @State private var targetCustomEnd: String
    @State private var targetTeacher: String
    @State private var targetRoom: String
    @State private var errorMessage: String?

    init(occurrence: CourseOccurrence, timetable: Timetable, onSaved: @escaping () -> Void) {
        self.occurrence = occurrence
        self.timetable = timetable
        self.onSaved = onSaved
        let lastNode = timetable.timeTable.nodes.map(\.node).max() ?? 1
        let safeNodeCount = min(max(occurrence.nodeCount, 1), lastNode)
        let safeStartNode = min(max(occurrence.startNode, 1), max(1, lastNode - safeNodeCount + 1))
        _targetDate = State(initialValue: TimetableDates.displayDate(forEpochDay: occurrence.epochDay))
        _targetStartNode = State(initialValue: safeStartNode)
        _targetCustomStart = State(
            initialValue: occurrence.usesCustomTime
                ? MinuteOfDay.shared.format(minuteOfDay: occurrence.startMinuteOfDay)
                : ""
        )
        _targetCustomEnd = State(
            initialValue: occurrence.usesCustomTime
                ? MinuteOfDay.shared.format(minuteOfDay: occurrence.endMinuteOfDay)
                : ""
        )
        _targetTeacher = State(initialValue: occurrence.teacher)
        _targetRoom = State(initialValue: occurrence.room)
    }

    var body: some View {
        NavigationView {
            Form {
                DatePicker(
                    "label.reschedule_date",
                    selection: $targetDate,
                    in: TimetableDates.displayDate(forEpochDay: timetable.coverageRange.startEpochDay)...TimetableDates.displayDate(forEpochDay: timetable.coverageRange.endEpochDay),
                    displayedComponents: [.date]
                )
                if occurrence.usesCustomTime {
                    Section(header: Text("section.target_time")) {
                        TextField("placeholder.start_time", text: $targetCustomStart)
                            .keyboardType(.numbersAndPunctuation)
                        TextField("placeholder.end_time", text: $targetCustomEnd)
                            .keyboardType(.numbersAndPunctuation)
                    }
                } else {
                    Stepper(value: Binding(
                        get: { Int(targetStartNode) },
                        set: { targetStartNode = min(max(Int32($0), 1), Int32(maxStartNode)) }
                    ), in: 1...maxStartNode) {
                        FormValueRow(
                            label: AppLocalization.string("label.start_node"),
                            value: AppLocalization.string("value.start_node", String(targetStartNode))
                        )
                    }
                }
                Section(header: Text("section.single_occurrence_replacement")) {
                    Text("single_occurrence_replacement.help")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    TextField("label.teacher", text: $targetTeacher)
                    TextField("label.room", text: $targetRoom)
                }
                if let errorMessage {
                    Text(verbatim: errorMessage).foregroundColor(.red)
                }
                Section {
                    Button("action.save_reschedule") { save() }
                        .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("navigation.reschedule")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("action.cancel") { presentationMode.wrappedValue.dismiss() }
                }
            }
        }
    }

    private var lastNode: Int32 {
        timetable.timeTable.nodes.map(\.node).max() ?? 1
    }

    private var maxStartNode: Int {
        let nodeCount = max(1, occurrence.nodeCount)
        return Int(max(1, lastNode - nodeCount + 1))
    }

    private var commandNodeSpan: (startNode: Int32, nodeCount: Int32) {
        let safeNodeCount = min(max(occurrence.nodeCount, 1), lastNode)
        if occurrence.usesCustomTime {
            let safeStartNode = min(
                max(occurrence.startNode, 1),
                max(1, lastNode - safeNodeCount + 1)
            )
            return (safeStartNode, safeNodeCount)
        }
        return (targetStartNode, safeNodeCount)
    }

    private func save() {
        var targetCustomTime: MinuteRange?
        if occurrence.usesCustomTime {
            guard let start = MinuteOfDay.shared.parse(value: targetCustomStart),
                  let end = MinuteOfDay.shared.parse(value: targetCustomEnd) else {
                errorMessage = AppLocalization.string("error.invalid_time")
                return
            }
            let range = MinuteRange(startMinuteOfDay: start.int32Value, endMinuteOfDay: end.int32Value)
            guard range.isValid else {
                errorMessage = AppLocalization.string("error.end_before_start")
                return
            }
            targetCustomTime = range
        }

        let nodeSpan = commandNodeSpan
        guard timetable.timeTable.rangeForNodes(
            startNode: nodeSpan.startNode,
            nodeCount: nodeSpan.nodeCount
        ) != nil else {
            errorMessage = AppLocalization.string("error.no_schedule_range")
            return
        }
        let targetEpochDay = TimetableDates.epochDay(for: targetDate)
        let targetDay = TimetableDates.dayOfWeek(
            forEpochDay: targetEpochDay,
            firstDayEpochDay: timetable.firstDayEpochDay
        )
        let command = TimetableCommandRescheduleOccurrence(
            logicalSlotId: occurrence.logicalSlotId,
            originalEpochDay: occurrence.originalEpochDay,
            targetEpochDay: targetEpochDay,
            recurrenceSegmentId: occurrence.recurrenceSegmentId,
            targetDayOfWeek: targetDay.asKotlinInt,
            targetStartNode: nodeSpan.startNode.asKotlinInt,
            targetNodeCount: nodeSpan.nodeCount.asKotlinInt,
            targetCustomTime: targetCustomTime,
            targetTeacher: targetTeacher.trimmingCharacters(in: .whitespacesAndNewlines),
            targetRoom: targetRoom.trimmingCharacters(in: .whitespacesAndNewlines)
        )
        if store.apply(command, to: timetable) {
            onSaved()
            presentationMode.wrappedValue.dismiss()
        }
    }
}

struct CourseEditorView: View {
    @Environment(\.presentationMode) private var presentationMode
    let course: Course
    let schedule: ScheduleTimeTable
    let onSave: (Course) -> Void
    let onDelete: () -> Void

    @State private var name: String
    @State private var note: String
    @State private var credit: String
    @State private var color: Int32
    @State private var slots: [LogicalCourseSlot]
    @State private var selectedSlot: SlotSelection?
    @State private var showingDeleteConfirmation = false

    init(course: Course, schedule: ScheduleTimeTable, onSave: @escaping (Course) -> Void, onDelete: @escaping () -> Void) {
        self.course = course
        self.schedule = schedule
        self.onSave = onSave
        self.onDelete = onDelete
        _name = State(initialValue: course.name)
        _note = State(initialValue: course.note)
        _credit = State(initialValue: course.credit == 0 ? "" : String(course.credit))
        _color = State(initialValue: course.color)
        _slots = State(initialValue: course.slots)
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("section.basic_info")) {
                    TextField("field.course_name", text: $name)
                    TextField("field.note", text: $note)
                    TextField("field.credit", text: $credit)
                        .keyboardType(.decimalPad)
                    ColorPicker("label.course_color", selection: Binding(
                        get: { Color.sharedCourse(color) },
                        set: { color = $0.sharedColorValue }
                    ))
                }

                Section(header: Text("section.time_slots")) {
                    ForEach(slots, id: \.id) { slot in
                        Button {
                            selectedSlot = SlotSelection(id: slot.id, slot: slot)
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(slotSummary(slot))
                                        .foregroundColor(.primary)
                                    Text([slot.teacher, slot.room].filter { !$0.isEmpty }.joined(separator: " · "))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }

                Section {
                    Button("action.save_course") { save() }
                        .frame(maxWidth: .infinity)
                    Button("action.delete_full_course") {
                        showingDeleteConfirmation = true
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("navigation.edit_course")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("action.cancel") { presentationMode.wrappedValue.dismiss() }
                }
            }
        }
        .sheet(item: $selectedSlot) { selection in
            SlotEditorView(slot: selection.slot, schedule: schedule) { updatedSlot in
                slots = slots.map { $0.id == updatedSlot.id ? updatedSlot : $0 }
                selectedSlot = nil
            }
        }
        .alert(isPresented: $showingDeleteConfirmation) {
            Alert(
                title: Text("action.delete_course"),
                message: Text("alert.delete_course.message"),
                primaryButton: .destructive(Text("action.delete")) {
                    onDelete()
                    presentationMode.wrappedValue.dismiss()
                },
                secondaryButton: .cancel()
            )
        }
    }

    private func slotSummary(_ slot: LogicalCourseSlot) -> String {
        let dayNames = [
            "",
            AppLocalization.string("weekday.monday"),
            AppLocalization.string("weekday.tuesday"),
            AppLocalization.string("weekday.wednesday"),
            AppLocalization.string("weekday.thursday"),
            AppLocalization.string("weekday.friday"),
            AppLocalization.string("weekday.saturday"),
            AppLocalization.string("weekday.sunday")
        ]
        let day = dayNames[Int(slot.dayOfWeek)]
        if let customTime = slot.customTime {
            return AppLocalization.string(
                "slot.custom_time",
                day,
                MinuteOfDay.shared.format(minuteOfDay: customTime.startMinuteOfDay),
                MinuteOfDay.shared.format(minuteOfDay: customTime.endMinuteOfDay)
            )
        }
        return AppLocalization.string(
            "slot.nodes",
            day,
            String(slot.startNode),
            String(slot.endNode)
        )
    }

    private func save() {
        guard let numericCredit = Float(credit.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "0" : credit) else { return }
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else { return }
        onSave(course.replacing(name: trimmedName, color: color, note: note, credit: numericCredit, slots: slots))
        presentationMode.wrappedValue.dismiss()
    }
}

struct SlotSelection: Identifiable {
    let id: String
    let slot: LogicalCourseSlot
}

struct SlotEditorView: View {
    @Environment(\.presentationMode) private var presentationMode
    let slot: LogicalCourseSlot
    let schedule: ScheduleTimeTable
    let onSave: (LogicalCourseSlot) -> Void

    @State private var dayOfWeek: Int32
    @State private var startNode: Int32
    @State private var nodeCount: Int32
    @State private var teacher: String
    @State private var room: String
    @State private var usesCustomTime: Bool
    @State private var customStart: String
    @State private var customEnd: String
    @State private var errorMessage: String?

    init(slot: LogicalCourseSlot, schedule: ScheduleTimeTable, onSave: @escaping (LogicalCourseSlot) -> Void) {
        self.slot = slot
        self.schedule = schedule
        self.onSave = onSave
        let lastNode = schedule.nodes.map(\.node).max() ?? 1
        let safeStartNode = min(max(slot.startNode, 1), lastNode)
        let safeNodeCount = min(max(slot.nodeCount, 1), max(1, lastNode - safeStartNode + 1))
        _dayOfWeek = State(initialValue: slot.dayOfWeek)
        _startNode = State(initialValue: safeStartNode)
        _nodeCount = State(initialValue: safeNodeCount)
        _teacher = State(initialValue: slot.teacher)
        _room = State(initialValue: slot.room)
        _usesCustomTime = State(initialValue: slot.customTime != nil)
        _customStart = State(initialValue: slot.customTime.map { MinuteOfDay.shared.format(minuteOfDay: $0.startMinuteOfDay) } ?? "")
        _customEnd = State(initialValue: slot.customTime.map { MinuteOfDay.shared.format(minuteOfDay: $0.endMinuteOfDay) } ?? "")
    }

    var body: some View {
        NavigationView {
            Form {
                Picker("label.weekday", selection: Binding(
                    get: { Int(dayOfWeek) },
                    set: { dayOfWeek = Int32($0) }
                )) {
                    ForEach(1...7, id: \.self) { day in
                        Text(verbatim: weekdayName(for: day)).tag(day)
                    }
                }
                Stepper(value: Binding(
                    get: { Int(startNode) },
                    set: { startNode = min(max(Int32($0), 1), Int32(maxStartNode)) }
                ), in: 1...maxStartNode) {
                    FormValueRow(
                        label: AppLocalization.string("label.start_node"),
                        value: AppLocalization.string("value.start_node", String(startNode))
                    )
                }
                Stepper(value: Binding(
                    get: { Int(nodeCount) },
                    set: {
                        nodeCount = min(max(Int32($0), 1), Int32(maxNodeCount))
                        startNode = min(startNode, Int32(maxStartNode))
                    }
                ), in: 1...maxNodeCount) {
                    FormValueRow(
                        label: AppLocalization.string("label.continuous_nodes"),
                        value: AppLocalization.string("value.continuous_nodes", String(nodeCount))
                    )
                }
                TextField("field.teacher", text: $teacher)
                TextField("field.room", text: $room)
                Toggle("toggle.custom_time", isOn: $usesCustomTime)
                if usesCustomTime {
                    TextField("placeholder.start_time", text: $customStart)
                    TextField("placeholder.end_time", text: $customEnd)
                        .keyboardType(.numbersAndPunctuation)
                }
                if let errorMessage {
                    Text(verbatim: errorMessage).foregroundColor(.red)
                }
                Button("action.save_slot") { save() }
                    .frame(maxWidth: .infinity)
            }
            .navigationTitle("navigation.schedule")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("action.cancel") { presentationMode.wrappedValue.dismiss() }
                }
            }
        }
    }

    private var lastNode: Int32 {
        schedule.nodes.map(\.node).max() ?? 1
    }

    private var maxStartNode: Int {
        Int(max(1, lastNode - nodeCount + 1))
    }

    private var maxNodeCount: Int {
        Int(max(1, lastNode - startNode + 1))
    }

    private func save() {
        var customTime: MinuteRange?
        if usesCustomTime {
            guard let start = MinuteOfDay.shared.parse(value: customStart),
                  let end = MinuteOfDay.shared.parse(value: customEnd) else {
                errorMessage = AppLocalization.string("error.invalid_time")
                return
            }
            let range = MinuteRange(startMinuteOfDay: start.int32Value, endMinuteOfDay: end.int32Value)
            guard range.isValid else {
                errorMessage = AppLocalization.string("error.end_before_start")
                return
            }
            customTime = range
        }
        guard schedule.rangeForNodes(startNode: startNode, nodeCount: nodeCount) != nil else {
            errorMessage = AppLocalization.string("error.schedule_range")
            return
        }
        let updated = slot.replacingAndSyncingRecurrenceOverrides(
            dayOfWeek: dayOfWeek,
            startNode: startNode,
            nodeCount: nodeCount,
            teacher: teacher,
            room: room,
            customTime: customTime
        )
        onSave(updated)
        presentationMode.wrappedValue.dismiss()
    }
}

struct NewCourseView: View {
    @Environment(\.presentationMode) private var presentationMode
    let schedule: ScheduleTimeTable
    let onSave: (Course) -> Void

    @State private var name = ""
    @State private var teacher = ""
    @State private var room = ""
    @State private var note = ""
    @State private var credit = ""
    @State private var dayOfWeek: Int32 = 1
    @State private var startNode: Int32 = 1
    @State private var nodeCount: Int32 = 1
    @State private var startWeek: Int32 = 1
    @State private var endWeek: Int32 = 20
    @State private var patternIndex = 0
    @State private var errorMessage: String?

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("section.course")) {
                    TextField("field.course_name", text: $name)
                    TextField("field.teacher", text: $teacher)
                    TextField("field.room", text: $room)
                    TextField("field.note", text: $note)
                    TextField("field.credit", text: $credit)
                        .keyboardType(.decimalPad)
                }
                Section(header: Text("section.recurrence")) {
                    Picker("label.weekday", selection: Binding(
                        get: { Int(dayOfWeek) },
                        set: { dayOfWeek = Int32($0) }
                    )) {
                        ForEach(1...7, id: \.self) { day in
                            Text(verbatim: weekdayName(for: day)).tag(day)
                        }
                    }
                    Stepper(value: Binding(
                        get: { Int(startNode) },
                        set: { startNode = min(max(Int32($0), 1), Int32(maxStartNode)) }
                    ), in: 1...maxStartNode) {
                        FormValueRow(
                        label: AppLocalization.string("label.start_node"),
                        value: AppLocalization.string("value.start_node", String(startNode))
                    )
                    }
                    Stepper(value: Binding(
                        get: { Int(nodeCount) },
                        set: {
                            nodeCount = min(max(Int32($0), 1), Int32(maxNodeCount))
                            startNode = min(startNode, Int32(maxStartNode))
                        }
                    ), in: 1...maxNodeCount) {
                        FormValueRow(
                        label: AppLocalization.string("label.continuous_nodes"),
                        value: AppLocalization.string("value.continuous_nodes", String(nodeCount))
                    )
                    }
                    Stepper(value: Binding(
                        get: { Int(startWeek) },
                        set: { startWeek = Int32($0) }
                    ), in: 1...60) {
                        FormValueRow(
                            label: AppLocalization.string("label.week_number"),
                            value: AppLocalization.string("value.week", String(startWeek))
                        )
                    }
                    Stepper(value: Binding(
                        get: { Int(endWeek) },
                        set: { endWeek = Int32($0) }
                    ), in: 1...60) {
                        FormValueRow(
                            label: AppLocalization.string("label.week_number"),
                            value: AppLocalization.string("value.week", String(endWeek))
                        )
                    }
                    Picker("label.week_number", selection: $patternIndex) {
                        Text("pattern.weekly").tag(0)
                        Text("pattern.odd").tag(1)
                        Text("pattern.even").tag(2)
                    }
                }
                if let errorMessage {
                    Text(verbatim: errorMessage).foregroundColor(.red)
                }
                Button("action.add_course") { save() }
                    .frame(maxWidth: .infinity)
            }
            .navigationTitle("navigation.add_course")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("action.cancel") { presentationMode.wrappedValue.dismiss() }
                }
            }
        }
    }

    private var lastNode: Int32 {
        schedule.nodes.map(\.node).max() ?? 1
    }

    private var maxStartNode: Int {
        Int(max(1, lastNode - nodeCount + 1))
    }

    private var maxNodeCount: Int {
        Int(max(1, lastNode - startNode + 1))
    }

    private func save() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else {
            errorMessage = AppLocalization.string("error.enter_course_name")
            return
        }
        guard endWeek >= startWeek else {
            errorMessage = AppLocalization.string("error.end_week_before_start")
            return
        }
        guard schedule.rangeForNodes(startNode: startNode, nodeCount: nodeCount) != nil else {
            errorMessage = AppLocalization.string("error.schedule_range")
            return
        }
        let courseID = UUID().uuidString
        let slotID = UUID().uuidString
        let segment = RecurrenceSegment(
            id: UUID().uuidString,
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
            dayOfWeek: dayOfWeek,
            startNode: startNode,
            nodeCount: nodeCount,
            teacher: teacher,
            room: room,
            customTime: nil,
            recurrenceSegments: [segment]
        )
        let numericCredit = Float(credit.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "0" : credit) ?? 0
        let course = Course(
            id: courseID,
            name: trimmedName,
            color: Int32(bitPattern: 0xFF4F8EF7),
            note: note,
            credit: numericCredit,
            slots: [slot]
        )
        onSave(course)
        presentationMode.wrappedValue.dismiss()
    }

    private var pattern: WeekPattern {
        switch patternIndex {
        case 1: return .odd
        case 2: return .even
        default: return .all
        }
    }
}

private struct TimetableRowFrames: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]
    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, frame in frame })
    }
}

private struct TimetableTitleFrames: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]
    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, frame in frame })
    }
}

// A native long press fails as soon as scrolling begins, before claiming the touch.
private struct TimetableReorderTouchCapture: UIViewRepresentable {
    let globalOrigin: CGPoint
    let accepts: (CGPoint) -> Bool
    let event: (UIGestureRecognizer.State, CGPoint) -> Void

    final class TouchView: UIView, UIGestureRecognizerDelegate {
        var globalOrigin = CGPoint.zero
        var accepts: (CGPoint) -> Bool = { _ in false }
        var event: (UIGestureRecognizer.State, CGPoint) -> Void = { _, _ in }
        private weak var attachedWindow: UIWindow?
        private weak var scrollingView: UIScrollView?
        private var scrollWasEnabled = false
        private lazy var press: UILongPressGestureRecognizer = {
            let gesture = UILongPressGestureRecognizer(target: self, action: #selector(handlePress(_:)))
            gesture.minimumPressDuration = 0.35
            gesture.allowableMovement = 12
            gesture.delegate = self
            return gesture
        }()

        override func didMoveToWindow() {
            super.didMoveToWindow()
            detach()
            attachedWindow = window
            window?.addGestureRecognizer(press)
        }

        func detach() {
            restoreScrolling()
            attachedWindow?.removeGestureRecognizer(press)
            attachedWindow = nil
        }

        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
            let point = touch.location(in: self)
            return accepts(CGPoint(x: point.x + globalOrigin.x, y: point.y + globalOrigin.y))
        }

        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer,
                               shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool { true }

        private func restoreScrolling() {
            scrollingView?.isScrollEnabled = scrollWasEnabled
            scrollingView = nil
        }

        @objc private func handlePress(_ gesture: UILongPressGestureRecognizer) {
            if gesture.state == .began {
                var hit = attachedWindow?.hitTest(gesture.location(in: attachedWindow), with: nil)
                while let view = hit {
                    if let scroll = view as? UIScrollView {
                        scrollingView = scroll
                        scrollWasEnabled = scroll.isScrollEnabled
                        scroll.isScrollEnabled = false
                        break
                    }
                    hit = view.superview
                }
            }
            let point = gesture.location(in: self)
            event(gesture.state, CGPoint(x: point.x + globalOrigin.x, y: point.y + globalOrigin.y))
            if gesture.state == .ended || gesture.state == .cancelled || gesture.state == .failed {
                restoreScrolling()
            }
        }
    }

    func makeUIView(context: Context) -> TouchView {
        let view = TouchView()
        view.isUserInteractionEnabled = false
        view.globalOrigin = globalOrigin
        view.accepts = accepts
        view.event = event
        return view
    }

    func updateUIView(_ view: TouchView, context: Context) {
        view.globalOrigin = globalOrigin
        view.accepts = accepts
        view.event = event
    }

    static func dismantleUIView(_ view: TouchView, coordinator: ()) { view.detach() }
}

struct TimetableManagementView: View {
    @EnvironmentObject private var store: TimetableStore
    @State private var showingNew = false
    @State private var deletingID: String?
    @State private var deletingCorruptedID: String?
    @State private var draggedID: String?
    @State private var rowFrames: [String: CGRect] = [:]
    @State private var dragTranslation: CGFloat = 0
    @State private var dragStart = CGPoint.zero
    @State private var titleFrames: [String: CGRect] = [:]
    @State private var acceptsReorder = false

    private func reorderTouch(_ state: UIGestureRecognizer.State, at point: CGPoint) {
        switch state {
        case .began:
            draggedID = titleFrames.first(where: { $0.value.contains(point) })?.key
            dragStart = point
        case .changed:
            dragTranslation = point.y - dragStart.y
        case .ended:
            if let id = draggedID,
               let targetID = rowFrames.first(where: { $0.key != id && $0.value.contains(point) })?.key,
               let source = store.timetables.firstIndex(where: { $0.id == id }),
               let target = store.timetables.firstIndex(where: { $0.id == targetID }) {
                store.moveTimetable(from: IndexSet(integer: source), to: target > source ? target + 1 : target)
            }
            draggedID = nil
            dragTranslation = 0
        case .cancelled, .failed:
            draggedID = nil
            dragTranslation = 0
        default: break
        }
    }

    var body: some View {
        List {
            Section(header: Text("section.my_timetables")) {
                ForEach(store.timetables, id: \.id) { timetable in
                    VStack(spacing: 10) {
                        HStack(spacing: 12) {
                            Button { if draggedID == nil { store.selectTimetable(timetable.id) } } label: {
                                HStack {
                                    Image(systemName: "calendar").foregroundColor(SchedulePalette.accent)
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(timetable.name).font(.system(size: 17, weight: .semibold)).foregroundColor(.primary)
                                        Text(AppLocalization.string("timetable.course_count", String(timetable.courses.count), timetable.timeTable.name)).font(.caption).foregroundColor(.secondary)
                                    }
                                    Spacer()
                                    if store.selectedTimetableID == timetable.id { Image(systemName: "checkmark").foregroundColor(SchedulePalette.accent) }
                                }
                            }
                            .accessibilityIdentifier("table.select.\(timetable.id)")
                            .background(GeometryReader { geometry in
                                Color.clear.preference(key: TimetableTitleFrames.self, value: [timetable.id: geometry.frame(in: .global)])
                            })
                            Button(role: .destructive) { deletingID = timetable.id } label: { Image(systemName: "trash").frame(width: 44, height: 44) }
                                .accessibilityLabel("action.delete")
                        }
                        Divider()
                        HStack(spacing: 0) {
                            NavigationLink("section.course", destination: CourseManagementView(tableID: timetable.id))
                                .frame(maxWidth: .infinity)
                            NavigationLink("tab.settings", destination: TimetableEditView(timetable: timetable)).frame(maxWidth: .infinity)
                            NavigationLink("settings.appearance", destination: ScheduleAppearanceView(tableID: timetable.id)).frame(maxWidth: .infinity)
                            Button("action.copy") {
                                if store.importTimetable(timetable, destination: .create), let copyID = store.selectedTimetableID {
                                    for key in ["rowHeight", "textSize", "style", "background"] {
                                        if let value = UserDefaults.standard.object(forKey: "schedule.\(timetable.id).\(key)") {
                                            UserDefaults.standard.set(value, forKey: "schedule.\(copyID).\(key)")
                                        }
                                    }
                                }
                            }.frame(maxWidth: .infinity)
                        }.font(.system(size: 13)).padding(.vertical, 8)
                    }.buttonStyle(.borderless).padding(.vertical, 6)
                    .background(GeometryReader { geometry in
                        Color.clear.preference(key: TimetableRowFrames.self, value: [timetable.id: geometry.frame(in: .global)])
                    })
                    .offset(y: draggedID == timetable.id ? dragTranslation : 0)
                    .zIndex(draggedID == timetable.id ? 1 : 0)
                }
                .onDelete { offsets in
                    deletingID = offsets.compactMap { store.timetables[$0].id }.first
                }
            }
            if !store.corruptedTimetables.isEmpty {
                Section(header: Text("section.needs_attention")) {
                    ForEach(store.corruptedTimetables) { record in
                        HStack(spacing: 12) {
                            Image(systemName: "exclamationmark.triangle")
                                .foregroundColor(.orange)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(record.name)
                                Text(record.reason)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .onDelete { offsets in
                        deletingCorruptedID = offsets
                            .compactMap { store.corruptedTimetables[$0].id }
                            .first
                    }
                }
            }
            Section {
                Button("action.new_timetable") { showingNew = true }
            }
        }
        .background(GeometryReader { geometry in
            TimetableReorderTouchCapture(globalOrigin: geometry.frame(in: .global).origin, accepts: { point in
                acceptsReorder && !showingNew && deletingID == nil && deletingCorruptedID == nil
                    && geometry.frame(in: .global).contains(point)
                    && titleFrames.values.contains(where: { $0.contains(point) })
            }, event: { state, point in reorderTouch(state, at: point) })
        })
        .onPreferenceChange(TimetableRowFrames.self) { rowFrames = $0 }
        .onPreferenceChange(TimetableTitleFrames.self) { titleFrames = $0 }
        .onAppear { acceptsReorder = true }
        .onDisappear { acceptsReorder = false; draggedID = nil; dragTranslation = 0 }
        .navigationTitle("navigation.timetable_management")
        .navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) { Button { showingNew = true } label: { Image(systemName: "plus") }.accessibilityLabel("action.new_timetable") }
        }
        .sheet(isPresented: $showingNew) { NewTimetableView() }
        .alert(isPresented: Binding(
            get: { deletingID != nil || deletingCorruptedID != nil },
            set: {
                if !$0 {
                    deletingID = nil
                    deletingCorruptedID = nil
                }
            }
        )) {
            Alert(
                title: Text(verbatim: AppLocalization.string(
                    deletingCorruptedID == nil
                        ? "navigation.timetable"
                        : "alert.delete_corrupted_timetable.title"
                )),
                message: Text(verbatim: AppLocalization.string(
                    deletingCorruptedID == nil
                        ? "alert.delete_timetable.message"
                        : "alert.delete_corrupted_timetable.message"
                )),
                primaryButton: .destructive(Text("action.delete")) {
                    if let deletingCorruptedID {
                        _ = store.deleteTimetable(deletingCorruptedID)
                    } else if let deletingID {
                        _ = store.deleteTimetable(deletingID)
                    }
                    deletingID = nil
                    deletingCorruptedID = nil
                },
                secondaryButton: .cancel()
            )
        }
    }
}

struct NewTimetableView: View {
    @EnvironmentObject private var store: TimetableStore
    @Environment(\.presentationMode) private var presentationMode
    @State private var name = ""
    @State private var definitionID = ReusableTimeTable.defaultID

    var body: some View {
        NavigationView {
            Form {
                TextField("field.timetable_name", text: $name)
                Picker("label.schedule", selection: $definitionID) {
                    ForEach(store.timeTableDefinitions, id: \.id) { definition in
                        Text(definition.name).tag(definition.id)
                    }
                }
                Button("action.create") {
                    let definition = store.timeTableDefinitions.first(where: { $0.id == definitionID })
                    if store.createTimetable(name: name, using: definition) != nil {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .navigationTitle("navigation.new_timetable")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("action.cancel") { presentationMode.wrappedValue.dismiss() }
                }
            }
        }
        .onAppear {
            if definitionID.isEmpty { definitionID = store.timeTableDefinitions.first?.id ?? ReusableTimeTable.defaultID }
        }
    }
}

struct TimetableEditView: View {
    @EnvironmentObject private var store: TimetableStore
    @Environment(\.presentationMode) private var presentationMode
    let timetable: Timetable

    @State private var name: String
    @State private var firstDay: Date
    @State private var definitionID: String
    @State private var maxWeek: Int32
    @State private var showSaturday: Bool
    @State private var showSunday: Bool
    @State private var sundayFirst: Bool

    init(timetable: Timetable) {
        self.timetable = timetable
        _name = State(initialValue: timetable.name)
        _firstDay = State(initialValue: TimetableDates.displayDate(
            forEpochDay: TimetableDates.mondayEpochDay(
                containing: TimetableDates.displayDate(forEpochDay: timetable.firstDayEpochDay)
            )
        ))
        _definitionID = State(initialValue: timetable.timeTable.id)
        _maxWeek = State(initialValue: timetable.maxWeek)
        _showSaturday = State(initialValue: timetable.showSaturday)
        _showSunday = State(initialValue: timetable.showSunday)
        _sundayFirst = State(initialValue: timetable.sundayFirst)
    }

    var body: some View {
        Form {
            TextField("field.timetable_name", text: $name)
            Picker("label.schedule", selection: $definitionID) {
                ForEach(store.timeTableDefinitions, id: \.id) { definition in
                    Text(definition.name).tag(definition.id)
                }
            }
            DatePicker(
                "label.first_week_date",
                selection: Binding(
                    get: { firstDay },
                    set: { firstDay = TimetableDates.displayDate(
                        forEpochDay: TimetableDates.mondayEpochDay(containing: $0)
                    ) }
                ),
                displayedComponents: [.date]
            )
            Stepper(value: Binding(
                get: { Int(maxWeek) },
                set: { maxWeek = Int32($0) }
            ), in: 1...60) {
                FormValueRow(
                    label: AppLocalization.string("label.term_weeks"),
                    value: AppLocalization.string("value.term_weeks", String(maxWeek))
                )
            }
            Toggle("toggle.show_saturday", isOn: $showSaturday)
            Toggle("toggle.show_sunday", isOn: $showSunday)
            Toggle("toggle.sunday_first", isOn: $sundayFirst)
            Section("section.actions") {
                NavigationLink("settings.appearance") { ScheduleAppearanceView(tableID: timetable.id) }
                NavigationLink("navigation.course_management") { CourseManagementView(tableID: timetable.id) }
                NavigationLink("navigation.schedule") { ReusableTimeTableView(tableID: timetable.id) }
                NavigationLink("navigation.widgets") { WidgetHelpView() }
            }
            Button("action.save") {
                let selectedSchedule = store.timeTableDefinitions
                    .first(where: { $0.id == definitionID })?.domain
                let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !trimmedName.isEmpty else {
                    store.errorMessage = AppLocalization.string("error.enter_timetable_name")
                    return
                }
                let current = store.timetables.first { $0.id == timetable.id } ?? timetable
                let updated = current.replacing(
                    name: trimmedName,
                    firstDayEpochDay: TimetableDates.mondayEpochDay(containing: firstDay),
                    maxWeek: maxWeek,
                    timeTable: selectedSchedule,
                    showSaturday: showSaturday,
                    showSunday: showSunday,
                    sundayFirst: sundayFirst
                )
                if store.updateTimetable(updated) {
                    presentationMode.wrappedValue.dismiss()
                }
            }
            .frame(maxWidth: .infinity)
        }
        .navigationTitle("navigation.timetable_settings")
        .navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
        .onChange(of: store.changeToken) { _ in
            // Child pages save the bound schedule and display options independently.
            guard let current = store.timetables.first(where: { $0.id == timetable.id }) else { return }
            definitionID = current.timeTable.id
            showSaturday = current.showSaturday
            showSunday = current.showSunday
            sundayFirst = current.sundayFirst
        }
        .onAppear {
            firstDay = TimetableDates.displayDate(
                forEpochDay: TimetableDates.mondayEpochDay(containing: firstDay)
            )
        }
    }
}

struct SettingsView: View {
    @EnvironmentObject private var store: TimetableStore
    @EnvironmentObject private var reminders: ReminderScheduler
    @AppStorage("appearance") private var appearance = "system"
    @AppStorage(AppLanguage.storageKey) private var appLanguage = AppLanguage.system.rawValue
    @AppStorage(EmptyWeekViewPreferences.modeKey) private var emptyViewMode = EmptyWeekViewMode.defaultView.rawValue
    @AppStorage(EmptyWeekViewPreferences.bottomPaddingKey) private var emptyViewBottomPadding = EmptyWeekViewPreferences.defaultBottomPadding
    @State private var emptyViewImageData: Data?
    @State private var showingEmptyImagePicker = false

    var body: some View {
        Form {
            Section(header: Text("section.data")) {
                NavigationLink("settings.reusable_schedules") { ReusableTimeTableView() }
                Text("settings.data.help")
                    .font(.caption)
                    .foregroundColor(.secondary)
                if let error = store.widgetPublishError {
                    Label("settings.widget_publish_failed", systemImage: "exclamationmark.triangle")
                        .foregroundColor(.orange)
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Button("action.retry_publish") {
                        _ = store.retryWidgetPublish()
                    }
                }
            }
            Section(header: Text("section.reminders")) {
                HStack {
                    Text("reminder.authorization")
                    Spacer()
                    Text(reminders.authorizationState.title)
                        .foregroundColor(reminders.authorizationState.isUsable ? .green : .secondary)
                }
                HStack {
                    Text("reminder.scheduled")
                    Spacer()
                    Text("\(reminders.scheduledCount)")
                        .foregroundColor(.secondary)
                }
                HStack {
                    Text("reminder.remaining")
                    Spacer()
                    Text("\(reminders.remainingCount)")
                        .foregroundColor(.secondary)
                }
                if reminders.authorizationState.isUsable && reminders.remainingCount > 0 {
                    Button("action.supplement_reminders") { reminders.supplementPendingRequests() }
                }
                if reminders.authorizationState == .denied {
                    Button("settings.open_system") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                    }
                } else if !reminders.authorizationState.isUsable {
                    Button("action.request_authorization") { reminders.requestAuthorization() }
                }
                Button("action.refresh_authorization") { reminders.refreshAuthorization() }
                if let error = reminders.lastError {
                    Text(error).font(.caption).foregroundColor(.red)
                }
                if store.selectedTimetable != nil {
                    NavigationLink("reminder.settings.link") { ReminderSettingsView() }
                }
            }
            Section(header: Text("section.appearance")) {
                Picker("settings.theme.title", selection: $appearance) {
                    Text("settings.theme.system").tag("system")
                    Text("settings.theme.light").tag("light")
                    Text("settings.theme.dark").tag("dark")
                }
            }
            Section(header: Text("section.empty_week")) {
                Picker("label.empty_week_style", selection: $emptyViewMode) {
                    Text("empty_week.mode.default").tag(EmptyWeekViewMode.defaultView.rawValue)
                    Text("empty_week.mode.hidden").tag(EmptyWeekViewMode.hidden.rawValue)
                    Text("empty_week.mode.user_image").tag(EmptyWeekViewMode.userImage.rawValue)
                }
                if emptyViewMode == EmptyWeekViewMode.userImage.rawValue {
                    Button("action.choose_empty_image") {
                        showingEmptyImagePicker = true
                    }
                    if emptyViewImageData != nil {
                        Button("action.remove_empty_image", role: .destructive) {
                            emptyViewImageData = nil
                            UserDefaults.standard.removeObject(forKey: EmptyWeekViewPreferences.imageDataKey)
                            emptyViewMode = EmptyWeekViewMode.defaultView.rawValue
                        }
                    } else {
                        Text("empty_week.no_image")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                Stepper(value: $emptyViewBottomPadding, in: 0...240, step: 4) {
                    FormValueRow(
                        label: AppLocalization.string("label.empty_week_bottom_padding"),
                        value: AppLocalization.string("value.points", String(Int(emptyViewBottomPadding)))
                    )
                }
            }
            Section(header: Text("section.language")) {
                Picker("settings.language.title", selection: $appLanguage) {
                    Text("settings.language.system").tag(AppLanguage.system.rawValue)
                    Text("settings.language.simplified_chinese").tag(AppLanguage.simplifiedChinese.rawValue)
                    Text("settings.language.english").tag(AppLanguage.english.rawValue)
                }
            }
        }
        .navigationTitle("navigation.settings")
        .navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
        .onAppear {
            emptyViewImageData = UserDefaults.standard.data(forKey: EmptyWeekViewPreferences.imageDataKey)
        }
        .sheet(isPresented: $showingEmptyImagePicker) {
            EmptyImagePicker { data in
                emptyViewImageData = data
                UserDefaults.standard.set(data, forKey: EmptyWeekViewPreferences.imageDataKey)
                emptyViewMode = EmptyWeekViewMode.userImage.rawValue
            }
        }
    }
}

struct ReminderSettingsView: View {
    @EnvironmentObject private var store: TimetableStore
    @Environment(\.presentationMode) private var presentationMode

    @State private var startEnabled = false
    @State private var endEnabled = false
    @State private var startLead = 10
    @State private var endLead = 5
    @State private var includeCourseName = true
    @State private var includeTeacher = true
    @State private var includeRoom = true
    @State private var includeNote = false
    @State private var silent = false

    var body: some View {
        Form {
            Section(header: Text("section.reminder_time")) {
                Toggle("reminder.start_toggle", isOn: $startEnabled)
                Stepper(value: $startLead, in: 0...120, step: 5) {
                    FormValueRow(
                        label: AppLocalization.string("reminder.advance"),
                        value: AppLocalization.string("reminder.minutes", String(startLead))
                    )
                }
                Toggle("reminder.end_toggle", isOn: $endEnabled)
                Stepper(value: $endLead, in: 0...120, step: 5) {
                    FormValueRow(
                        label: AppLocalization.string("reminder.advance"),
                        value: AppLocalization.string("reminder.minutes", String(endLead))
                    )
                }
            }
            Section(header: Text("section.notification_content")) {
                Toggle("reminder.content.course", isOn: $includeCourseName)
                Toggle("reminder.content.teacher", isOn: $includeTeacher)
                Toggle("reminder.content.room", isOn: $includeRoom)
                Toggle("reminder.content.note", isOn: $includeNote)
                Toggle("reminder.content.mute", isOn: $silent)
                Text("reminder.vibration_note")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Button("action.save_reminder") { save() }
                .frame(maxWidth: .infinity)
        }
        .navigationTitle("navigation.reminder_settings")
        .navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
        .toolbar { ToolbarItem(placement: .navigationBarTrailing) { Button("action.save_reminder") { save() } } }
        .onAppear(perform: load)
    }

    private func load() {
        guard let settings = store.selectedTimetable?.reminderSettings else { return }
        startEnabled = settings.startEnabled
        endEnabled = settings.endEnabled
        startLead = Int(settings.startLeadMinutes)
        endLead = Int(settings.endLeadMinutes)
        includeCourseName = settings.content.includeCourseName
        includeTeacher = settings.content.includeTeacher
        includeRoom = settings.content.includeRoom
        includeNote = settings.content.includeNote
        silent = settings.silent
    }

    private func save() {
        guard let timetable = store.selectedTimetable else { return }
        let settings = ReminderSettings(
            startEnabled: startEnabled,
            endEnabled: endEnabled,
            startLeadMinutes: Int32(startLead),
            endLeadMinutes: Int32(endLead),
            content: ReminderContentSettings(
                includeCourseName: includeCourseName,
                includeTeacher: includeTeacher,
                includeRoom: includeRoom,
                includeNote: includeNote
            ),
            vibrate: timetable.reminderSettings.vibrate,
            silent: silent
        )
        if store.updateTimetable(timetable.replacing(reminderSettings: settings)) {
            presentationMode.wrappedValue.dismiss()
        }
    }
}

struct ReusableTimeTableView: View {
    @EnvironmentObject private var store: TimetableStore
    var tableID: String? = nil
    private var table: Timetable? {
        store.timetables.first { $0.id == (tableID ?? store.selectedTimetableID) }
    }
    @State private var showingNew = false
    @State private var deletingID: String?

    var body: some View {
        List {
            ForEach(store.timeTableDefinitions, id: \.id) { definition in
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text(definition.name).font(.headline)
                        Spacer()
                        if table?.timeTable.id == definition.id { Image(systemName: "checkmark").foregroundColor(SchedulePalette.accent) }
                    }
                    Text(AppLocalization.string("value.node_count", String(definition.nodes.count))).font(.caption).foregroundColor(.secondary)
                    HStack {
                        NavigationLink("action.edit") { TimeTableEditorView(definition: definition) }
                            .accessibilityIdentifier("schedule.edit.\(definition.id)")
                        Spacer()
                        Button("action.copy") { _ = store.copyDefinition(definition) }
                        Spacer()
                        Button("action.delete", role: .destructive) { deletingID = definition.id }
                            .disabled(definition.id == ReusableTimeTable.defaultID)
                        Spacer()
                        Button("schedule.use") {
                            if let table { _ = store.updateTimetable(table.replacing(timeTable: definition.domain)) }
                        }.disabled(table == nil || table?.timeTable.id == definition.id)
                    }.font(.subheadline)
                }.buttonStyle(.borderless).padding(.vertical, 6)
            }
            .onDelete { offsets in
                deletingID = offsets.compactMap { store.timeTableDefinitions[$0].id }.first
            }
        }
        .navigationTitle("navigation.reusable_schedules")
        .navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button { showingNew = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel("action.new_schedule")
            }
        }
        .sheet(isPresented: $showingNew) {
            NavigationView {
                TimeTableEditorView(definition: ReusableTimeTable(id: UUID().uuidString, name: "", nodes: []), isNew: true)
                    .toolbar { ToolbarItem(placement: .navigationBarLeading) { Button("action.cancel") { showingNew = false } } }
            }.navigationViewStyle(.stack)
        }
        .alert(isPresented: Binding(
            get: { deletingID != nil },
            set: { if !$0 { deletingID = nil } }
        )) {
            Alert(
                title: Text("action.delete_schedule"),
                message: Text("alert.delete_schedule.message"),
                primaryButton: .destructive(Text("action.delete")) {
                    if let deletingID { _ = store.deleteDefinition(deletingID) }
                    deletingID = nil
                },
                secondaryButton: .cancel()
            )
        }
    }
}

struct TimeTableEditorView: View {
    @EnvironmentObject private var store: TimetableStore
    @Environment(\.presentationMode) private var presentationMode
    let definition: ReusableTimeTable
    let isNew: Bool

    @State private var name: String
    @State private var nodes: [TimeTableNodeValue]
    @State private var uniformDuration: Int
    @State private var breakMinutes = 10
    @State private var breakFirst = 1
    @State private var breakLast = 4
    @State private var errorMessage: String?

    init(definition: ReusableTimeTable, isNew: Bool = false) {
        self.definition = definition
        self.isNew = isNew
        _name = State(initialValue: definition.name)
        _nodes = State(initialValue: definition.nodes)
        _uniformDuration = State(initialValue: Int(
            definition.nodes.first.map { $0.endMinuteOfDay - $0.startMinuteOfDay } ?? 45
        ))
    }

    var body: some View {
        Form {
            Section(header: Text("section.basic_info")) {
                TextField("field.schedule_name", text: $name)
            }
            Section(header: Text("section.uniform_duration")) {
                Stepper(value: $uniformDuration, in: 1...240) {
                    FormValueRow(
                        label: AppLocalization.string("label.uniform_duration"),
                        value: AppLocalization.string("value.duration_minutes", String(uniformDuration))
                    )
                }
                Button("action.apply_uniform_duration") {
                    applyUniformDuration()
                }
            }
            Section(header: Text("section.breaks")) {
                Stepper(AppLocalization.string("value.duration_minutes", String(breakMinutes)), value: $breakMinutes, in: 0...240)
                    .accessibilityIdentifier("schedule.break.minutes")
                Stepper(AppLocalization.string("break.first", String(breakFirst)), value: $breakFirst, in: 1...max(1, nodes.count - 1))
                Stepper(AppLocalization.string("break.last", String(breakLast)), value: $breakLast, in: 2...max(2, nodes.count))
                Button("break.apply") {
                    do {
                        nodes = try ReusableTimeTable(id: definition.id, name: name, nodes: nodes).applyingBreak(minutes: breakMinutes, first: breakFirst, last: breakLast).nodes
                        errorMessage = nil
                    } catch { errorMessage = error.localizedDescription }
                }
            }
            Section(header: Text("section.nodes")) {
                ForEach(nodes.indices, id: \.self) { index in
                    VStack {
                        TimeTableNodeEditor(node: Binding(
                            get: { nodes[index] },
                            set: { nodes[index] = $0 }
                        ))
                        Button("schedule.delete_node", role: .destructive) {
                            nodes.remove(at: index)
                            for position in nodes.indices { nodes[position].node = Int32(position + 1) }
                            breakLast = min(breakLast, max(2, nodes.count))
                            breakFirst = min(breakFirst, max(1, nodes.count - 1))
                        }.disabled(nodes.count <= 1)
                    }
                }
                if nodes.count < 60 {
                    Button("action.add_node") {
                        let node = Int32(nodes.count + 1)
                        let start = nodes.last?.endMinuteOfDay ?? 480
                        nodes.append(TimeTableNodeValue(node: node, startMinuteOfDay: start, endMinuteOfDay: start + 45))
                    }
                }
            }
            if let errorMessage {
                Text(verbatim: errorMessage).foregroundColor(.red)
            }
            Button("action.save_schedule") { save() }
                .frame(maxWidth: .infinity)
        }
        .navigationTitle(Text(verbatim: AppLocalization.string(
            isNew ? "navigation.new_schedule" : "navigation.schedule_edit"
        )))
        .navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) { Button("action.save_schedule") { save() } }
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("action.done") { UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil) }
            }
        }
        .onAppear {
            if isNew && nodes.isEmpty {
                nodes = ReusableTimeTable.defaultDefinition().nodes
                uniformDuration = Int(
                    nodes.first.map { $0.endMinuteOfDay - $0.startMinuteOfDay } ?? 45
                )
            }
        }
    }

    private func applyUniformDuration() {
        nodes = nodes.map { node in
            var updated = node
            updated.endMinuteOfDay = node.startMinuteOfDay + Int32(uniformDuration)
            return updated
        }
        errorMessage = nil
    }

    private func save() {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else {
            errorMessage = AppLocalization.string("error.enter_schedule_name")
            return
        }
        let updated = ReusableTimeTable(id: definition.id, name: trimmedName, nodes: nodes)
        if isNew {
            if store.createDefinition(name: updated.name, nodes: updated.nodes) != nil {
                presentationMode.wrappedValue.dismiss()
            } else { errorMessage = store.errorMessage }
        } else if store.saveDefinition(updated) {
            presentationMode.wrappedValue.dismiss()
        } else { errorMessage = store.errorMessage }
    }
}

struct TimeTableNodeEditor: View {
    @Binding var node: TimeTableNodeValue

    private func clock(_ key: WritableKeyPath<TimeTableNodeValue, Int32>) -> Binding<Date> {
        Binding(get: {
            Calendar.current.startOfDay(for: Date()).addingTimeInterval(Double(node[keyPath: key]) * 60)
        }, set: { date in
            let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
            node[keyPath: key] = Int32((parts.hour ?? 0) * 60 + (parts.minute ?? 0))
        })
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack {
                Text(verbatim: AppLocalization.string("value.node", String(node.node)))
                    .font(.subheadline.weight(.semibold))
                Spacer()
                Text(verbatim: AppLocalization.string(
                    "value.time_range",
                    MinuteOfDay.shared.format(minuteOfDay: node.startMinuteOfDay),
                    MinuteOfDay.shared.format(minuteOfDay: node.endMinuteOfDay)
                ))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            HStack {
                DatePicker("form.start_time", selection: clock(\.startMinuteOfDay), displayedComponents: .hourAndMinute)
                    .accessibilityIdentifier("schedule.node.\(node.node).start")
                DatePicker("form.end_time", selection: clock(\.endMinuteOfDay), displayedComponents: .hourAndMinute)
                    .accessibilityIdentifier("schedule.node.\(node.node).end")
            }
        }
    }
}

private struct FormValueRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
            Spacer()
            Text(value)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.trailing)
        }
    }
}

private extension Color {
    static func sharedCourse(_ value: Int32) -> Color {
        let color = UInt32(bitPattern: value)
        return Color(
            red: Double((color >> 16) & 0xFF) / 255,
            green: Double((color >> 8) & 0xFF) / 255,
            blue: Double(color & 0xFF) / 255
        )
    }

    var sharedColorValue: Int32 {
        #if os(iOS)
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        UIColor(self).getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return Int32(bitPattern: 0xFF000000 | (UInt32(red * 255) << 16) | (UInt32(green * 255) << 8) | UInt32(blue * 255))
        #else
        return Int32(bitPattern: 0xFF4F8EF7)
        #endif
    }
}

// The week canvas uses timetable nodes, not wall-clock hours; breaks do not create extra rows.
private enum SchedulePalette {
    static let accent = Color(red: 1, green: 0.18, blue: 0.38)
    static let background = LinearGradient(colors: [Color(red: 0.91, green: 0.91, blue: 0.957), Color(red: 0.745, green: 0.808, blue: 0.898)], startPoint: .top, endPoint: .bottom)
    static let colors: [Int32] = [0xFFE991AF, 0xFF648FCC, 0xFF64D7C8, 0xFFB59AE8, 0xFFEA7698, 0xFFE88770].map { Int32(bitPattern: $0) }
}

private struct CourseDetailPresentation: ViewModifier {
    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDetents([.medium, .large]).presentationDragIndicator(.visible)
        } else {
            content
        }
    }
}

struct GridCourseSeed: Equatable, Identifiable {
    var id: String { "\(day)-\(start)-\(count)" }
    var day: Int
    var start: Int
    var count: Int
}

private struct WeekChooserView: View {
    let maxWeek: Int
    let selected: Int
    let current: Int
    let onSelect: (Int) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            ScrollView {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 5), spacing: 12) {
                    ForEach(1...max(maxWeek, 1), id: \.self) { value in
                        Button { onSelect(value) } label: {
                            Text("\(value)").font(.headline).frame(maxWidth: .infinity).frame(height: 44)
                                .foregroundColor(value == selected ? .white : .primary)
                                .background(value == selected ? SchedulePalette.accent : Color.secondary.opacity(0.1))
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                }.padding()
                Button("action.current_week") { onSelect(min(max(current, 1), max(maxWeek, 1))) }
                    .buttonStyle(.bordered)
            }
            .navigationTitle("label.week_selector").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("action.cancel") { dismiss() } } }
        }.navigationViewStyle(.stack)
    }
}

private struct ScheduleWeekGrid: View {
    let timetable: Timetable
    let week: Int32
    let onSelect: (CourseOccurrence) -> Void
    let onAdd: (GridCourseSeed) -> Void
    let onMove: (CourseOccurrence, Int64, Int32) -> Void
    let onFlip: (Int) -> Void
    @State private var selection: GridCourseSeed?
    @State private var draggingID: String?
    @State private var dragOffset = CGSize.zero
    @State private var handleStart: GridCourseSeed?
    @AppStorage(EmptyWeekViewPreferences.modeKey) private var emptyMode = EmptyWeekViewMode.defaultView.rawValue
    @AppStorage(EmptyWeekViewPreferences.imageDataKey) private var emptyImage = Data()
    @AppStorage("schedule.rowHeight") private var rowHeight = 66.0
    @AppStorage("schedule.textSize") private var textSize = 12.0
    @AppStorage private var styleData: Data
    @AppStorage private var imageData: Data
    private var style: IOSScheduleStyle { IOSScheduleStyle.read(styleData) }
    private var axis: CGFloat { style.showTimeBar ? 48 : 0 }

    init(timetable: Timetable, week: Int32, onSelect: @escaping (CourseOccurrence) -> Void, onAdd: @escaping (GridCourseSeed) -> Void, onMove: @escaping (CourseOccurrence, Int64, Int32) -> Void, onFlip: @escaping (Int) -> Void) {
        self.timetable = timetable; self.week = week; self.onSelect = onSelect
        self.onAdd = onAdd; self.onMove = onMove; self.onFlip = onFlip
        _rowHeight = AppStorage(wrappedValue: 66, "schedule.\(timetable.id).rowHeight")
        _textSize = AppStorage(wrappedValue: 12, "schedule.\(timetable.id).textSize")
        _styleData = AppStorage(wrappedValue: Data(), "schedule.\(timetable.id).style")
        _imageData = AppStorage(wrappedValue: Data(), "schedule.\(timetable.id).background")
    }

    private var days: [Int64] {
        let start = TimetableDates.range(for: timetable, week: week).startEpochDay
        let weekdays: [Int] = [1, 2, 3, 4, 5] + (timetable.showSaturday ? [6] : []) + (timetable.showSunday ? [7] : [])
        let ordered: [Int] = timetable.sundayFirst && timetable.showSunday ? [7] + weekdays.filter { $0 != 7 } : weekdays
        return ordered.map { start + Int64($0 - 1) }
    }

    var body: some View {
        let occurrences = TimetableEngine.shared.expandOccurrences(timetable: timetable, range: TimetableDates.range(for: timetable, week: week))
        let nodes = timetable.timeTable.nodes.sorted { $0.node < $1.node }
        GeometryReader { geometry in
            let width = max((geometry.size.width - axis) / CGFloat(max(days.count, 1)), 1)
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    if style.showTimeBar { Text(monthTitle).font(.system(size: style.headerSize, weight: .bold)).frame(width: axis) }
                    ForEach(days, id: \.self) { day in
                        let today = day == TimetableDates.epochDay(for: Date())
                        VStack(spacing: 2) {
                            Text(weekdayName(for: Int(TimetableDates.dayOfWeek(forEpochDay: day, firstDayEpochDay: timetable.firstDayEpochDay))))
                            Text(dateTitle(day))
                        }
                        .font(.system(size: style.headerSize, weight: today ? .bold : .regular))
                        .foregroundColor((style.interfaceColor == 0 ? Color.primary : Color.sharedCourse(style.interfaceColor)))
                        .frame(width: width, height: 40)
                    }
                }
                ScrollView(.vertical, showsIndicators: false) {
                    ZStack(alignment: .topLeading) {
                        VStack(spacing: 0) {
                            ForEach(nodes, id: \.node) { node in
                                HStack(spacing: 0) {
                                    if style.showTimeBar {
                                        VStack(spacing: 3) {
                                            Text("\(node.node)").font(.system(size: 12, weight: .bold))
                                            Text(MinuteOfDay.shared.format(minuteOfDay: node.startMinuteOfDay))
                                            Text(MinuteOfDay.shared.format(minuteOfDay: node.endMinuteOfDay))
                                        }.font(.system(size: 9)).foregroundColor((style.interfaceColor == 0 ? Color.primary : Color.sharedCourse(style.interfaceColor))).frame(width: axis, height: rowHeight)
                                    }
                                    ForEach(days, id: \.self) { day in
                                        Color.clear.frame(width: width, height: rowHeight)
                                            .overlay(Rectangle().stroke((style.interfaceColor == 0 ? Color.primary : Color.sharedCourse(style.interfaceColor)).opacity(style.showGrid ? 0.15 : 0), lineWidth: 0.5))
                                            .contentShape(Rectangle())
                                            .onTapGesture {
                                                selection = GridCourseSeed(day: Int(TimetableDates.dayOfWeek(forEpochDay: day, firstDayEpochDay: timetable.firstDayEpochDay)), start: Int(node.node), count: 1)
                                            }
                                            .accessibilityLabel(weekdayName(for: Int(TimetableDates.dayOfWeek(forEpochDay: day, firstDayEpochDay: timetable.firstDayEpochDay))) + " " + String(node.node))
                                            .accessibilityAddTraits(.isButton)
                                            .accessibilityIdentifier("grid.cell.\(TimetableDates.dayOfWeek(forEpochDay: day, firstDayEpochDay: timetable.firstDayEpochDay)).\(node.node)")
                                    }
                                }
                            }
                        }
                        if style.showOtherWeeks {
                            ForEach(otherWeekCourses(current: occurrences), id: \.logicalSlotId) { item in
                                if let dayIndex = days.firstIndex(where: {
                                    TimetableDates.dayOfWeek(forEpochDay: $0, firstDayEpochDay: timetable.firstDayEpochDay) == TimetableDates.dayOfWeek(forEpochDay: item.epochDay, firstDayEpochDay: timetable.firstDayEpochDay)
                                }) {
                                    courseCard(item, dayIndex: dayIndex, width: width, nodes: nodes, occurrences: [], ghost: true)
                                }
                            }
                        }
                        ForEach(occurrences, id: \.id) { item in
                            if let dayIndex = days.firstIndex(of: item.epochDay) {
                                courseCard(item, dayIndex: dayIndex, width: width, nodes: nodes, occurrences: occurrences)
                            }
                        }
                        if let selected = selection,
                           let dayIndex = days.firstIndex(where: { Int(TimetableDates.dayOfWeek(forEpochDay: $0, firstDayEpochDay: timetable.firstDayEpochDay)) == selected.day }) {
                            Button { onAdd(selected); selection = nil } label: {
                                Image(systemName: "plus").font(.system(size: 23))
                                    .frame(width: max(width - 4, 1), height: rowHeight * Double(selected.count) - 4)
                                    .background(SchedulePalette.accent.opacity(0.15))
                                    .overlay(RoundedRectangle(cornerRadius: 6).stroke(SchedulePalette.accent, lineWidth: 2))
                            }
                            .foregroundColor(SchedulePalette.accent)
                            .offset(x: axis + CGFloat(dayIndex) * width + 2, y: Double(selected.start - 1) * rowHeight + 2)
                            .accessibilityLabel("action.add_course")
                            .accessibilityIdentifier("grid.selection.add")
                            selectionHandle(selected, upper: true, width: width, dayIndex: dayIndex, maxNode: nodes.count)
                            selectionHandle(selected, upper: false, width: width, dayIndex: dayIndex, maxNode: nodes.count)
                        }
                    }
                    .frame(height: Double(nodes.count) * rowHeight + 32)
                }
                .simultaneousGesture(DragGesture(minimumDistance: 30).onEnded { value in
                    guard draggingID == nil, handleStart == nil,
                          abs(value.translation.width) > 60,
                          abs(value.translation.width) > abs(value.translation.height) * 1.6 else { return }
                    selection = nil
                    onFlip(value.translation.width < 0 ? 1 : -1)
                })
            }
        }
        .overlay {
            if occurrences.isEmpty && selection == nil && emptyMode != EmptyWeekViewMode.hidden.rawValue {
                WeekEmptyStateView(
                    mode: EmptyWeekViewMode(rawValue: emptyMode) ?? .defaultView,
                    imageData: emptyImage,
                    tint: style.interfaceColor == 0 ? .primary : .sharedCourse(style.interfaceColor)
                )
                .allowsHitTesting(false)
            }
        }
        .background {
            if let image = UIImage(data: imageData) {
                GeometryReader { proxy in
                    Image(uiImage: image).resizable().scaledToFill().frame(width: proxy.size.width, height: proxy.size.height).clipped()
                }.allowsHitTesting(false)
            }
        }
        .onChange(of: week) { _ in selection = nil }
    }

    private func otherWeekCourses(current: [CourseOccurrence]) -> [CourseOccurrence] {
        let recurring = TimetableEngine.shared.recurringOccurrences(timetable: timetable)
        let range = TimetableDates.range(for: timetable, week: week)
        // A cancellation or move does not turn this week's recurring slot into an other-week course.
        let scheduledThisWeek = recurring.filter { $0.epochDay >= range.startEpochDay && $0.epochDay <= range.endEpochDay }
        var seen = Set((current + scheduledThisWeek).map(\.logicalSlotId))
        return recurring.filter { item in
            guard !seen.contains(item.logicalSlotId) else { return false }
            seen.insert(item.logicalSlotId)
            return true
        }
    }

    private func courseCard(_ item: CourseOccurrence, dayIndex: Int, width: CGFloat, nodes: [TimeTableNode], occurrences: [CourseOccurrence], ghost: Bool = false) -> some View {
        let span = IOSGridGeometry.span(item, nodes: nodes)
        let height = max(24, span.height * rowHeight - 4)
        let conflict = occurrences.filter { $0.epochDay == item.epochDay && $0.startMinuteOfDay < item.endMinuteOfDay && $0.endMinuteOfDay > item.startMinuteOfDay }.count
        let dragging = draggingID == item.id
        let content = VStack(alignment: style.centerHorizontal ? .center : .leading, spacing: 5) {
            if style.centerVertical { Spacer(minLength: 0) }
            Text(item.courseName).font(.system(size: textSize, weight: .bold)).lineLimit(height > 100 ? 3 : 2)
            if style.showTeacher && !item.teacher.isEmpty { Text(item.teacher).font(.system(size: max(textSize - 1, 9))).lineLimit(1) }
            if style.showRoom && !item.room.isEmpty { Text((style.roomPrefix ? "@" : "") + item.room).font(.system(size: max(textSize - 1, 9))).lineLimit(1) }
            if style.showTime { Text(MinuteOfDay.shared.format(minuteOfDay: item.startMinuteOfDay) + "–" + MinuteOfDay.shared.format(minuteOfDay: item.endMinuteOfDay)).font(.system(size: max(textSize - 2, 8))).lineLimit(2) }
            if ghost { Text("appearance.not_this_week").font(.system(size: 9)) }
            if item.isRescheduled && height > 145 { Text("course.rescheduled").font(.system(size: 9)) }
            Spacer(minLength: 0)
        }
        .multilineTextAlignment(style.centerHorizontal ? .center : .leading)
        .foregroundColor(Color.sharedCourse(style.courseColor))
        .blendMode(style.mixText ? .overlay : .normal).padding(5)
        .frame(width: max(width - 4, 1), height: height, alignment: style.centerHorizontal ? .top : .topLeading)
        .background(Color.sharedCourse(item.color).opacity(ghost ? style.otherOpacity : style.opacity))
        .clipShape(RoundedRectangle(cornerRadius: style.radius))
        .overlay(RoundedRectangle(cornerRadius: style.radius).stroke(Color.sharedCourse(style.borderUsesCourse ? item.color : style.borderColor), style: StrokeStyle(lineWidth: conflict > 1 ? 2 : 1, dash: style.dotted ? [3, 3] : [])))
        .overlay(alignment: .topTrailing) {
            if conflict > 1 { Text("\(conflict)").font(.system(size: 9, weight: .bold)).padding(2).background(SchedulePalette.accent).foregroundColor(.white) }
        }
        let x: CGFloat = axis + CGFloat(dayIndex) * width + 2 + (dragging ? dragOffset.width : 0)
        let y: CGFloat = CGFloat(span.top * rowHeight) + 2 + (dragging ? dragOffset.height : 0)
        return Button {
            guard draggingID == nil else { return }
            selection = nil
            onSelect(item)
        } label: {
            content.contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .simultaneousGesture(courseDrag(item, dayIndex: dayIndex, width: width, nodeCount: nodes.count))
        .offset(x: x, y: y)
        .zIndex(ghost ? 0 : dragging ? 10 : 1)
        .allowsHitTesting(!ghost)
        .accessibilityHidden(ghost)
        .accessibilityLabel(item.courseName)
        .accessibilityAddTraits(.isButton)
    }

    private func courseDrag(_ item: CourseOccurrence, dayIndex: Int, width: CGFloat, nodeCount: Int) -> some Gesture {
        LongPressGesture(minimumDuration: 0.35, maximumDistance: 12)
            .sequenced(before: DragGesture(minimumDistance: 0, coordinateSpace: .global))
            .onChanged { value in
                if case .second(true, let drag) = value {
                    selection = nil
                    draggingID = item.id
                    dragOffset = drag?.translation ?? .zero
                }
            }
            .onEnded { value in
                if case .second(true, let drag) = value, let drag {
                    let dayDelta = Int((drag.translation.width / width).rounded())
                    let day = min(max(dayIndex + dayDelta, 0), days.count - 1)
                    let rowDelta = Int((drag.translation.height / CGFloat(rowHeight)).rounded())
                    let maxStart = max(1, nodeCount - Int(item.nodeCount) + 1)
                    let start = min(max(Int(item.startNode) + rowDelta, 1), maxStart)
                    onMove(item, days[day], Int32(start))
                }
                DispatchQueue.main.async { draggingID = nil; dragOffset = .zero }
            }
    }

    private func selectionHandle(_ selected: GridCourseSeed, upper: Bool, width: CGFloat, dayIndex: Int, maxNode: Int) -> some View {
        let x = axis + CGFloat(dayIndex + 1) * width - 14
        let y = Double(upper ? selected.start - 1 : selected.start + selected.count - 1) * rowHeight - 18
        return Image(systemName: upper ? "chevron.up" : "chevron.down")
            .font(.system(size: 12, weight: .bold)).foregroundColor(.white)
            .frame(width: 28, height: 36).background(SchedulePalette.accent).clipShape(Capsule())
            .contentShape(Rectangle()).offset(x: min(x, axis + width * CGFloat(days.count) - 30), y: max(y, 0))
            .highPriorityGesture(DragGesture(minimumDistance: 1, coordinateSpace: .global).onChanged { value in
                if handleStart == nil { handleStart = selected }
                guard let original = handleStart else { return }
                let delta = Int((value.translation.height / rowHeight).rounded())
                let end = original.start + original.count - 1
                if upper {
                    let start = min(max(original.start + delta, 1), end)
                    selection = GridCourseSeed(day: original.day, start: start, count: end - start + 1)
                } else {
                    let newEnd = min(max(end + delta, original.start), maxNode)
                    selection = GridCourseSeed(day: original.day, start: original.start, count: newEnd - original.start + 1)
                }
            }.onEnded { _ in DispatchQueue.main.async { handleStart = nil } })
            .accessibilityLabel(upper ? "selection.start" : "selection.end")
    }

    private var monthTitle: String {
        let date = TimetableDates.displayDate(forEpochDay: TimetableDates.range(for: timetable, week: week).startEpochDay)
        let formatter = DateFormatter(); formatter.locale = AppLocalization.currentLocale; formatter.dateFormat = "MMM"
        return formatter.string(from: date)
    }
    private func dateTitle(_ day: Int64) -> String {
        let components = TimetableDates.civilDateComponents(forEpochDay: day)
        return "\(components.month ?? 1)/\(components.day ?? 1)"
    }
}

private struct CourseDraftView: View {
    @Environment(\.dismiss) private var dismiss
    let timetable: Timetable
    let original: Course?
    let courseID: String
    let onSave: (Course) -> Bool
    @State private var name: String
    @State private var note: String
    @State private var credit: String
    @State private var color: Int32
    @State private var slots: [IOSSlotDraft]
    @State private var error: String?

    init(timetable: Timetable, course: Course? = nil, seed: GridCourseSeed = GridCourseSeed(day: 1, start: 1, count: 2), onSave: @escaping (Course) -> Bool) {
        self.timetable = timetable; original = course; self.onSave = onSave
        let id = course?.id ?? UUID().uuidString
        courseID = id
        _name = State(initialValue: course?.name ?? "")
        _note = State(initialValue: course?.note ?? "")
        _credit = State(initialValue: course.map { $0.credit == 0 ? "" : String($0.credit) } ?? "")
        _color = State(initialValue: course?.color ?? SchedulePalette.colors[0])
        let source = course?.slots ?? [Self.emptySlot(courseID: id, seed: seed, maxWeek: timetable.maxWeek)]
        _slots = State(initialValue: source.map { IOSSlotDraft($0, maxWeek: Int(timetable.maxWeek)) })
    }

    private static func emptySlot(courseID: String, seed: GridCourseSeed, maxWeek: Int32) -> LogicalCourseSlot {
        LogicalCourseSlot(id: UUID().uuidString, courseId: courseID, dayOfWeek: Int32(seed.day), startNode: Int32(seed.start), nodeCount: Int32(max(seed.count, 1)), teacher: "", room: "", customTime: nil,
            recurrenceSegments: [RecurrenceSegment(id: UUID().uuidString, startWeek: 1, endWeek: maxWeek, weekPattern: .all, dayOfWeek: nil, startNode: nil, nodeCount: nil, teacher: nil, room: nil, customTime: nil)])
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(spacing: 12) {
                        draftField("field.course_name", text: $name)
                        HStack(spacing: 12) {
                            ForEach(SchedulePalette.colors, id: \.self) { value in
                                Button { color = value } label: {
                                    Circle().fill(Color.sharedCourse(value)).frame(width: 32, height: 32)
                                        .overlay { if color == value { Image(systemName: "checkmark").foregroundColor(.white) } }
                                }.accessibilityLabel("label.course_color")
                            }
                            ColorPicker("label.course_color", selection: Binding(get: { Color.sharedCourse(color) }, set: { color = $0.sharedColorValue })).labelsHidden()
                        }
                        draftField("field.note", text: $note)
                        draftField("field.credit", text: $credit).keyboardType(.decimalPad)
                    }.padding(16).background(Color(UIColor.secondarySystemGroupedBackground)).clipShape(RoundedRectangle(cornerRadius: 12))
                    ForEach($slots) { $slot in
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("section.time_slots").font(.headline)
                                Spacer()
                                if slots.count > 1 {
                                    Button(role: .destructive) { slots.removeAll { $0.id == slot.id } } label: { Image(systemName: "trash").frame(width: 44, height: 44) }
                                }
                            }
                            HStack {
                                Picker("label.weekday", selection: $slot.day) {
                                    ForEach(1...7, id: \.self) { Text(weekdayName(for: $0)).tag($0) }
                                }
                                Spacer()
                                Text(AppLocalization.string("slot.nodes", "", String(slot.start), String(slot.start + slot.count - 1))).font(.subheadline)
                            }
                            Stepper(AppLocalization.string("value.start_node", String(slot.start)), value: $slot.start, in: 1...max(1, timetable.timeTable.nodes.count - slot.count + 1))
                            Stepper(AppLocalization.string("value.continuous_nodes", String(slot.count)), value: $slot.count, in: 1...max(1, timetable.timeTable.nodes.count - slot.start + 1))
                            draftField("field.teacher", text: $slot.teacher)
                            draftField("field.room", text: $slot.room)
                            ForEach($slot.recurrences) { $recurrence in
                                WeekSetPicker(weeks: $recurrence.weeks, maxWeek: Int(timetable.maxWeek))
                            }
                            Toggle("toggle.custom_time", isOn: $slot.custom)
                            if slot.custom {
                                HStack {
                                    draftField("placeholder.start_time", text: $slot.customStart)
                                    Text("–")
                                    draftField("placeholder.end_time", text: $slot.customEnd)
                                }.keyboardType(.numbersAndPunctuation)
                            }
                        }.padding(16).background(Color(UIColor.secondarySystemGroupedBackground)).clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    Button {
                        let slot = Self.emptySlot(courseID: courseID, seed: GridCourseSeed(day: 1, start: 1, count: min(2, timetable.timeTable.nodes.count)), maxWeek: timetable.maxWeek)
                        slots.append(IOSSlotDraft(slot, maxWeek: Int(timetable.maxWeek)))
                    } label: { Label("action.add_slot", systemImage: "plus").frame(maxWidth: .infinity).frame(height: 44) }
                    .buttonStyle(.bordered)
                    if let error { Text(error).foregroundColor(.red) }
                }.padding(16)
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle(original == nil ? "navigation.add_course" : "navigation.edit_course")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("action.cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("action.save_course") { save() }.font(.body.weight(.semibold)) }
            }
        }.navigationViewStyle(.stack).tint(SchedulePalette.accent)
    }

    private func draftField(_ key: LocalizedStringKey, text: Binding<String>) -> some View {
        TextField(key, text: text).padding(12).overlay(RoundedRectangle(cornerRadius: 7).stroke(Color.secondary.opacity(0.3)))
    }

    private func save() {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { error = AppLocalization.string("error.enter_course_name"); return }
        guard let numeric = Float(credit.isEmpty ? "0" : credit), numeric.isFinite else { error = AppLocalization.string("error.invalid_credit"); return }
        do {
            let result = try slots.map { try $0.slot(schedule: timetable.timeTable, maxWeek: Int(timetable.maxWeek)) }
            let updated = Course(id: courseID, name: name.trimmingCharacters(in: .whitespacesAndNewlines), color: color, note: note, credit: numeric, slots: result)
            if onSave(updated) { dismiss() }
            else { error = AppLocalization.string("error.save_course") }
        } catch { self.error = error.localizedDescription }
    }
}

private struct WeekSetPicker: View {
    @Binding var weeks: Set<Int>
    let maxWeek: Int
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("label.week_number").font(.subheadline.bold())
                Spacer()
                Button("pattern.weekly") { weeks = Set(1...max(maxWeek, 1)) }
                Button("pattern.odd") { weeks = Set((1...max(maxWeek, 1)).filter { $0 % 2 == 1 }) }
                Button("pattern.even") { weeks = Set((1...max(maxWeek, 1)).filter { $0 % 2 == 0 }) }
                Button("action.clear") { weeks = [] }
            }.font(.caption)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 5), spacing: 6) {
                ForEach(1...max(maxWeek, 1), id: \.self) { week in
                    Button {
                        if weeks.contains(week) { weeks.remove(week) } else { weeks.insert(week) }
                    } label: {
                        Text("\(week)").font(.system(size: 13, weight: .medium)).frame(maxWidth: .infinity).frame(height: 44)
                            .foregroundColor(weeks.contains(week) ? .white : .primary)
                            .background(weeks.contains(week) ? SchedulePalette.accent : Color.secondary.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }.accessibilityLabel(AppLocalization.string("week.number", String(week)))
                        .accessibilityAddTraits(weeks.contains(week) ? [.isSelected] : [])
                }
            }
        }
    }
}

private struct CourseManagementView: View {
    @EnvironmentObject private var store: TimetableStore
    let tableID: String
    @State private var editing: String?
    @State private var adding = false
    @State private var deleting: String?
    private var table: Timetable? { store.timetables.first { $0.id == tableID } }
    var body: some View {
        List {
            if let table {
                if table.courses.isEmpty { Text("course.management.empty").foregroundColor(.secondary) }
                ForEach(table.courses, id: \.id) { course in
                    Button { editing = course.id } label: {
                        HStack {
                            Circle().fill(Color.sharedCourse(course.color)).frame(width: 12, height: 12)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(course.name).foregroundColor(.primary)
                                Text(AppLocalization.string("course.slot_count", String(course.slots.count)))
                                    .font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary)
                        }.padding(.vertical, 8)
                    }
                    .swipeActions {
                        Button("action.delete", role: .destructive) { deleting = course.id }
                    }
                }
                Button("action.add_course") { adding = true }
            }
        }
        .navigationTitle("navigation.course_management").navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
        .sheet(isPresented: Binding(get: { editing != nil }, set: { if !$0 { editing = nil } })) {
            if let table, let course = table.courses.first(where: { $0.id == editing }) {
                CourseDraftView(timetable: table, course: course) { store.updateCourse($0, in: table) }
            }
        }
        .toolbar { Button { adding = true } label: { Image(systemName: "plus") }.accessibilityLabel("action.add_course") }
        .sheet(isPresented: $adding) {
            if let table { CourseDraftView(timetable: table) { store.addCourse($0, to: table) } }
        }
        .alert("alert.confirm_delete", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } })) {
            Button("action.delete_full_course", role: .destructive) {
                if let deleting, let table { _ = store.deleteCourse(deleting, from: table) }
                deleting = nil
            }
            Button("action.cancel", role: .cancel) { deleting = nil }
        } message: { Text("alert.delete_course.message") }
    }
}

private struct IOSScheduleStyle: Codable {
    var showGrid = false
    var showTimeBar = true
    var headerSize = 11.0
    var interfaceColor: Int32 = 0 // Follow the system text color until explicitly customized.
    var courseColor: Int32 = Int32(bitPattern: 0xFFFFFFFF)
    var borderColor: Int32 = Int32(bitPattern: 0x66FFFFFF)
    var mixText = false
    var borderUsesCourse = false
    var dotted = false
    var radius = 5.0
    var opacity = 0.9
    var otherOpacity = 0.3
    var showOtherWeeks = false
    var centerHorizontal = false
    var centerVertical = false
    var showTime = false
    var showRoom = true
    var roomPrefix = false
    var showTeacher = true

    static func read(_ data: Data) -> IOSScheduleStyle {
        // Appearance preferences are optional; old installations use the original defaults.
        (try? JSONDecoder().decode(Self.self, from: data)) ?? Self()
    }
}

private struct ScheduleAppearanceView: View {
    @EnvironmentObject private var store: TimetableStore
    @AppStorage private var rowHeight: Double
    @AppStorage private var textSize: Double
    @AppStorage private var styleData: Data
    @AppStorage private var imageData: Data
    @State private var choosingImage = false
    let tableID: String
    init(tableID: String) {
        self.tableID = tableID
        _rowHeight = AppStorage(wrappedValue: 66, "schedule.\(tableID).rowHeight")
        _textSize = AppStorage(wrappedValue: 12, "schedule.\(tableID).textSize")
        _styleData = AppStorage(wrappedValue: Data(), "schedule.\(tableID).style")
        _imageData = AppStorage(wrappedValue: Data(), "schedule.\(tableID).background")
    }
    private var style: IOSScheduleStyle { IOSScheduleStyle.read(styleData) }
    private func preference<T>(_ key: WritableKeyPath<IOSScheduleStyle, T>) -> Binding<T> {
        Binding(get: { style[keyPath: key] }, set: { value in
            var updated = style
            updated[keyPath: key] = value
            do { styleData = try JSONEncoder().encode(updated) }
            catch { store.errorMessage = error.localizedDescription }
        })
    }
    private func color(_ key: WritableKeyPath<IOSScheduleStyle, Int32>) -> Binding<Color> {
        Binding(get: { key == \.interfaceColor && style.interfaceColor == 0 ? Color.primary : Color.sharedCourse(style[keyPath: key]) }, set: { preference(key).wrappedValue = $0.sharedColorValue })
    }
    private func weekend(_ key: String) -> Binding<Bool> {
        Binding(get: {
            guard let table = store.timetables.first(where: { $0.id == tableID }) else { return false }
            return key == "sat" ? table.showSaturday : key == "sun" ? table.showSunday : table.sundayFirst
        }, set: { value in
            guard let table = store.timetables.first(where: { $0.id == tableID }) else { return }
            _ = store.updateTimetable(table.replacing(
                showSaturday: key == "sat" ? value : table.showSaturday,
                showSunday: key == "sun" ? value : table.showSunday,
                sundayFirst: key == "first" ? value : table.sundayFirst
            ))
        })
    }
    var body: some View {
        Form {
            Section("appearance.preview") {
                Text("appearance.preview_course")
                    .font(.system(size: textSize, weight: .bold))
                    .foregroundColor(Color.sharedCourse(style.courseColor))
                    .frame(maxWidth: .infinity, minHeight: rowHeight * 2)
                    .background(SchedulePalette.accent.opacity(style.opacity))
                    .clipShape(RoundedRectangle(cornerRadius: style.radius))
                    .overlay(RoundedRectangle(cornerRadius: style.radius).stroke(Color.sharedCourse(style.borderColor), style: StrokeStyle(lineWidth: 1, dash: style.dotted ? [3, 3] : [])))
            }
            Section("appearance.overall") {
                Button("appearance.choose_background") { choosingImage = true }
                if !imageData.isEmpty {
                    if let image = UIImage(data: imageData) { Image(uiImage: image).resizable().scaledToFit().frame(maxHeight: 120) }
                    Button("appearance.clear_background", role: .destructive) { imageData = Data() }
                }
                Toggle("appearance.grid", isOn: preference(\.showGrid))
                ColorPicker("appearance.interface_color", selection: color(\.interfaceColor), supportsOpacity: false)
                Stepper("\(AppLocalization.string("appearance.header_size")) · \(Int(style.headerSize)) pt", value: preference(\.headerSize), in: 8...32)
                Toggle("appearance.time_bar", isOn: preference(\.showTimeBar))
                Toggle("toggle.show_saturday", isOn: weekend("sat"))
                Toggle("toggle.show_sunday", isOn: weekend("sun"))
                Toggle("toggle.sunday_first", isOn: weekend("first"))
                Toggle("appearance.other_weeks", isOn: preference(\.showOtherWeeks))
            }
            Section("appearance.cells") {
                ColorPicker("appearance.text_color", selection: color(\.courseColor))
                Toggle("appearance.mix_text", isOn: preference(\.mixText))
                ColorPicker("appearance.border_color", selection: color(\.borderColor))
                Toggle("appearance.border_course", isOn: preference(\.borderUsesCourse))
                Toggle("appearance.dotted", isOn: preference(\.dotted))
                Stepper("\(AppLocalization.string("appearance.period_height")) · \(Int(rowHeight)) pt", value: $rowHeight, in: 34...130, step: 2)
                Stepper("\(AppLocalization.string("appearance.course_text")) · \(Int(textSize)) pt", value: $textSize, in: 8...32)
                Stepper("\(AppLocalization.string("appearance.radius")) · \(Int(style.radius)) pt", value: preference(\.radius), in: 0...32)
                Text("appearance.opacity")
                Slider(value: preference(\.opacity), in: 0...1, step: 0.05).accessibilityLabel("appearance.opacity")
                Text("appearance.other_opacity")
                Slider(value: preference(\.otherOpacity), in: 0...1, step: 0.05).accessibilityLabel("appearance.other_opacity")
                Toggle("appearance.center_horizontal", isOn: preference(\.centerHorizontal))
                Toggle("appearance.center_vertical", isOn: preference(\.centerVertical))
                Toggle("appearance.show_time", isOn: preference(\.showTime))
                Toggle("appearance.show_room", isOn: preference(\.showRoom))
                Toggle("appearance.room_prefix", isOn: preference(\.roomPrefix))
                Toggle("appearance.show_teacher", isOn: preference(\.showTeacher))
            }
        }
        .navigationTitle("settings.appearance").navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
        .sheet(isPresented: $choosingImage) { EmptyImagePicker { imageData = $0 } }
    }
}

private struct WidgetHelpView: View {
    @EnvironmentObject private var store: TimetableStore
    @State private var published = false
    var body: some View {
        List {
            Section("widget.help.add_title") {
                Text("widget.help.add")
                Text("widget.help.configure")
            }
            Section("widget.help.data_title") {
                Text("widget.help.data")
                Button("widget.help.refresh") { published = store.retryWidgetPublish() }
                if published { Label("widget.help.refreshed", systemImage: "checkmark") }
                if let error = store.widgetPublishError { Text(error).foregroundColor(.red) }
            }
        }
        .navigationTitle("navigation.widgets").navigationBarTitleDisplayMode(.inline).navigationBarHidden(false)
    }
}
