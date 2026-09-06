import XCTest

final class TimetableFlowTests: XCTestCase {
    private func capture(_ name: String, app: XCUIApplication) {
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)
        let hierarchy = XCTAttachment(string: app.debugDescription)
        hierarchy.name = name + "-hierarchy"
        hierarchy.lifetime = .keepAlways
        add(hierarchy)
    }

    private func chooseWeek(_ week: Int, app: XCUIApplication) {
        app.buttons["选择周次"].tap()
        let choice = app.buttons[String(week)]
        XCTAssertTrue(choice.waitForExistence(timeout: 5))
        choice.tap()
        XCTAssertTrue(app.buttons["线性代数"].waitForExistence(timeout: 5))
    }

    private func openMenu(_ title: String, app: XCUIApplication) {
        let menu = app.buttons["课表操作"]
        XCTAssertTrue(menu.waitForExistence(timeout: 8))
        menu.tap()
        let item = app.buttons[title]
        XCTAssertTrue(item.waitForExistence(timeout: 5))
        item.tap()
    }

    private func back(app: XCUIApplication) {
        let button = app.navigationBars.buttons.firstMatch
        XCTAssertTrue(button.waitForExistence(timeout: 5))
        button.tap()
    }

    func testWidgetGalleryShowsTwoDayConfiguration() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        openMenu("桌面小组件", app: app)
        app.buttons["刷新小组件数据"].tap()
        XCTAssertTrue(app.staticTexts["共享数据已更新"].waitForExistence(timeout: 5))
        XCUIDevice.shared.press(.home)
        let home = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        XCUIDevice.shared.press(.home)
        capture("widget-home", app: home)
        let homeIcon = try XCTUnwrap(home.icons.allElementsBoundByIndex.first {
            $0.frame.width > 40 && $0.frame.width < 100 && $0.isHittable
        })
        homeIcon.press(forDuration: 1.5)
        let editHome = home.buttons["编辑主屏幕"]
        XCTAssertTrue(editHome.waitForExistence(timeout: 5))
        editHome.tap()
        capture("widget-home-edit", app: home)
        let edit = home.buttons.matching(NSPredicate(format: "label IN %@", ["编辑", "Edit"])).firstMatch
        XCTAssertTrue(edit.waitForExistence(timeout: 5))
        edit.tap()
        capture("widget-home-edit-menu", app: home)
        home.buttons["添加小组件"].tap()
        let search = home.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout: 8))
        search.tap(); search.typeText("SleepDown")
        capture("widget-gallery-search", app: home)
        let result = home.cells["SleepDown"].firstMatch
        XCTAssertTrue(result.waitForExistence(timeout: 8))
        result.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        capture("widget-gallery-sleepdown", app: home)
        for _ in 0..<8 {
            if home.staticTexts["今日与明日"].exists { break }
            home.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: 0.57))
                .press(forDuration: 0.05, thenDragTo: home.coordinate(withNormalizedOffset: CGVector(dx: 0.15, dy: 0.57)))
        }
        capture("widget-gallery-two-day", app: home)
        XCTAssertTrue(home.staticTexts["今日与明日"].exists)
        home.buttons.matching(NSPredicate(format: "label CONTAINS %@", "添加小组件")).firstMatch.tap()
        let done = home.buttons["完成"]
        XCTAssertTrue(done.waitForExistence(timeout: 5))
        done.tap()
        capture("widget-two-day-installed", app: home)
        app.activate()
        back(app: app)
        try verifyWidgetBindingAndTap(app: app)
    }

    func testExistingWidgetBindingAndTap() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        try verifyWidgetBindingAndTap(app: app)
    }

    private func verifyWidgetBindingAndTap(app: XCUIApplication) throws {
        openMenu("新建课表", app: app)
        let name = "小组件绑定-" + String(Int(Date().timeIntervalSince1970))
        let field = app.textFields["课表名称"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap(); field.typeText(name)
        app.buttons["创建"].tap()
        XCUIDevice.shared.press(.home)
        XCUIDevice.shared.press(.home)
        let home = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let candidates = home.icons.matching(NSPredicate(format: "label == %@ AND value CONTAINS %@", "SleepDown", "小组件"))
        XCTAssertTrue(candidates.firstMatch.waitForExistence(timeout: 8))
        let widget = try XCTUnwrap(candidates.allElementsBoundByIndex.first { $0.frame.width > 300 && $0.isHittable })
        widget.press(forDuration: 1.2)
        capture("widget-instance-menu", app: home)
        let edit = home.buttons["编辑小组件"]
        XCTAssertTrue(edit.waitForExistence(timeout: 5))
        edit.tap()
        let parameter = home.cells.containing(.staticText, identifier: "课表").buttons.firstMatch
        XCTAssertTrue(parameter.waitForExistence(timeout: 120))
        capture("widget-instance-editor", app: home)
        parameter.tap()
        let choice = home.buttons[name].firstMatch
        for _ in 0..<18 {
            if choice.isHittable { break }
            home.swipeUp()
        }
        capture("widget-instance-picker", app: home)
        XCTAssertTrue(choice.isHittable)
        choice.tap()
        capture("widget-instance-selection-confirmation", app: home)
        XCTAssertTrue(home.buttons[name].waitForExistence(timeout: 8))
        home.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.12)).tap()
        XCTAssertTrue(home.staticTexts[name].waitForExistence(timeout: 30))
        capture("widget-instance-bound", app: home)
        app.activate()
        openMenu("新建课表", app: app)
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap(); field.typeText(name + "其他")
        app.buttons["创建"].tap()
        XCTAssertTrue(app.staticTexts[name + "其他"].waitForExistence(timeout: 8))
        XCUIDevice.shared.press(.home)
        capture("widget-instance-after-table-switch", app: home)
        XCTAssertTrue(home.staticTexts[name].waitForExistence(timeout: 30))
        widget.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2)).tap()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 8))
        XCTAssertTrue(app.staticTexts[name].waitForExistence(timeout: 8))
        XCTAssertFalse(app.staticTexts[name + "其他"].exists)
        capture("widget-instance-opened-bound-table", app: app)
    }

    @available(iOS 16.4, *)
    func testWidgetDeepLinkRepeatsAndPreservesOpenDraft() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        openMenu("新建课表", app: app)
        let name = "小组件跳转-" + String(Int(Date().timeIntervalSince1970))
        let field = app.textFields["课表名称"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap(); field.typeText(name)
        app.buttons["创建"].tap()
        let firstWeek = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "第 1 周 ·")).firstMatch
        let url = URL(string: "sleepdown://week?epochDay=0")!
        app.terminate()
        XCUIDevice.shared.system.open(url)
        XCTAssertTrue(firstWeek.waitForExistence(timeout: 8))
        app.buttons["选择周次"].tap()
        app.buttons["2"].tap()
        XCUIDevice.shared.system.open(url)
        XCTAssertTrue(firstWeek.waitForExistence(timeout: 8))
        app.buttons["添加课程"].tap()
        let draft = app.textFields["课程名称"]
        XCTAssertTrue(draft.waitForExistence(timeout: 5))
        draft.tap(); draft.typeText("保留草稿")
        XCUIDevice.shared.system.open(url)
        XCTAssertTrue(draft.waitForExistence(timeout: 8))
        XCTAssertEqual(draft.value as? String, "保留草稿")
        capture("widget-deep-link-draft", app: app)
        app.navigationBars["添加课程"].buttons["取消"].tap()
        XCTAssertTrue(draft.waitForNonExistence(timeout: 5))
        XCTAssertTrue(firstWeek.waitForExistence(timeout: 8))
        XCUIDevice.shared.system.open(URL(string: "sleepdown://week?tableID=missing-widget-table&epochDay=0")!)
        XCTAssertTrue(app.alerts.firstMatch.waitForExistence(timeout: 8))
        XCTAssertTrue(app.staticTexts["找不到所选课表，请在主 App 中重新发布。"].exists)
        app.alerts.buttons.firstMatch.tap()
        XCTAssertTrue(app.staticTexts[name].exists)
        capture("widget-deep-link-root", app: app)
    }

    func testSecondaryMenusNavigationAndPersistence() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        if app.buttons["创建第一张课表"].waitForExistence(timeout: 5) {
            app.buttons["创建第一张课表"].tap()
        } else { openMenu("新建课表", app: app) }
        let name = "二级菜单-" + String(Int(Date().timeIntervalSince1970))
        let field = app.textFields["课表名称"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap(); field.typeText(name)
        app.buttons["创建"].tap()
        XCTAssertTrue(app.buttons["课表操作"].waitForExistence(timeout: 8))

        openMenu("课表设置", app: app)
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        XCTAssertTrue(app.navigationBars["课表设置"].exists)
        field.tap(); field.typeText("已保存")
        app.swipeUp()
        app.buttons["保存"].tap()
        openMenu("课表设置", app: app)
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        XCTAssertEqual(field.value as? String, name + "已保存")
        capture("secondary-table-settings", app: app)
        back(app: app)

        openMenu("外观", app: app)
        let grid = app.switches["显示网格辅助线"]
        XCTAssertTrue(grid.waitForExistence(timeout: 5))
        grid.coordinate(withNormalizedOffset: CGVector(dx: 0.93, dy: 0.5)).tap()
        XCTAssertEqual(grid.value as? String, "1")
        capture("secondary-appearance", app: app)
        back(app: app)
        openMenu("外观", app: app)
        XCTAssertTrue(grid.waitForExistence(timeout: 5))
        XCTAssertEqual(grid.value as? String, "1")
        back(app: app)

        openMenu("课程管理", app: app)
        XCTAssertTrue(app.navigationBars["课程管理"].waitForExistence(timeout: 5))
        app.navigationBars.buttons["添加课程"].tap()
        let courseName = app.textFields["课程名称"]
        XCTAssertTrue(courseName.waitForExistence(timeout: 5))
        courseName.tap(); courseName.typeText("菜单测试课程")
        app.buttons["保存课程"].tap()
        let course = app.buttons.containing(.staticText, identifier: "菜单测试课程").firstMatch
        XCTAssertTrue(course.waitForExistence(timeout: 5))
        capture("secondary-course-management", app: app)
        back(app: app)

        openMenu("时间表", app: app)
        XCTAssertTrue(app.navigationBars["可复用时间表"].waitForExistence(timeout: 5))
        capture("secondary-time-tables", app: app)
        app.buttons["新建时间表"].tap()
        let scheduleName = app.textFields["时间表名称"]
        XCTAssertTrue(scheduleName.waitForExistence(timeout: 5))
        scheduleName.tap(); scheduleName.typeText("菜单时间表-" + name)
        app.navigationBars.buttons["保存时间表"].tap()
        XCTAssertTrue(app.navigationBars["可复用时间表"].waitForExistence(timeout: 5))
        back(app: app)

        openMenu("设置", app: app)
        XCTAssertTrue(app.navigationBars["设置"].waitForExistence(timeout: 5))
        capture("secondary-settings", app: app)
        back(app: app)
        openMenu("桌面小组件", app: app)
        XCTAssertTrue(app.buttons["刷新小组件数据"].waitForExistence(timeout: 5))
        app.buttons["刷新小组件数据"].tap()
        XCTAssertTrue(app.staticTexts["共享数据已更新"].waitForExistence(timeout: 5))
        capture("secondary-widgets", app: app)
        back(app: app)
        openMenu("课表管理", app: app)
        XCTAssertTrue(app.navigationBars["课表管理"].waitForExistence(timeout: 5))
        capture("secondary-management", app: app)
        back(app: app)
        app.terminate(); app.launch()
        openMenu("外观", app: app)
        XCTAssertTrue(grid.waitForExistence(timeout: 5))
        XCTAssertEqual(grid.value as? String, "1")
        back(app: app)
        XCTAssertTrue(app.buttons["菜单测试课程"].waitForExistence(timeout: 5))
    }

    func testManagementDragTimeTableBindingAndCourseDeletion() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        let suffix = String(Int(Date().timeIntervalSince1970))
        let firstName = "管理A-" + suffix
        let secondName = "管理B-" + suffix
        for name in [firstName, secondName] {
            if app.buttons["创建第一张课表"].waitForExistence(timeout: 3) { app.buttons["创建第一张课表"].tap() }
            else { openMenu("新建课表", app: app) }
            let field = app.textFields["课表名称"]
            XCTAssertTrue(field.waitForExistence(timeout: 5))
            field.tap(); field.typeText(name)
            app.buttons["创建"].tap()
            XCTAssertTrue(app.buttons["课表操作"].waitForExistence(timeout: 5))
        }
        openMenu("课表管理", app: app)
        let first = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@ AND label CONTAINS %@", "table.select.", firstName)).firstMatch
        let second = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@ AND label CONTAINS %@", "table.select.", secondName)).firstMatch
        for _ in 0..<12 {
            if first.isHittable && second.isHittable && max(first.frame.maxY, second.frame.maxY) < app.frame.maxY - 90 { break }
            app.swipeUp()
        }
        XCTAssertTrue(first.isHittable && second.isHittable)
        XCTAssertLessThan(max(first.frame.maxY, second.frame.maxY), app.frame.maxY - 90)
        XCTAssertLessThan(first.frame.minY, second.frame.minY)
        capture("secondary-before-reorder", app: app)
        first.press(forDuration: 1.0, thenDragTo: second)
        capture("secondary-after-drop", app: app)
        let reordered = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            second.frame.minY < first.frame.minY
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [reordered], timeout: 8), .completed)
        capture("secondary-direct-reorder", app: app)
        // Releasing outside the cards must leave the most recently saved order intact.
        let outsideCards = app.coordinate(withNormalizedOffset: .zero)
            .withOffset(CGVector(dx: 8, dy: second.frame.midY))
        first.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
            .press(forDuration: 1.0, thenDragTo: outsideCards)
        XCTAssertLessThan(second.frame.minY, first.frame.minY)
        capture("secondary-reorder-cancelled", app: app)
        back(app: app)
        openMenu("时间表", app: app)
        app.buttons["新建时间表"].tap()
        let scheduleName = "范围课间-" + suffix
        let nameField = app.textFields["时间表名称"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap(); nameField.typeText(scheduleName)
        // Dismiss the keyboard before interacting with the stepper.
        app.buttons["完成"].tap()
        let minutes = app.steppers["schedule.break.minutes"]
        XCTAssertTrue(minutes.waitForExistence(timeout: 5))
        for _ in 0..<5 { minutes.coordinate(withNormalizedOffset: CGVector(dx: 0.93, dy: 0.5)).tap() }
        app.buttons["应用课间时长"].tap()
        app.navigationBars.buttons["保存时间表"].tap()
        XCTAssertTrue(app.navigationBars["可复用时间表"].waitForExistence(timeout: 5))
        let row = app.cells.containing(.staticText, identifier: scheduleName).firstMatch
        for _ in 0..<12 {
            if row.buttons["使用"].isHittable { break }
            app.swipeUp()
        }
        row.buttons["使用"].tap()
        row.buttons["编辑"].tap()
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        XCTAssertEqual(nameField.value as? String, scheduleName)
        app.swipeUp()
        capture("secondary-break-saved", app: app)
        XCTAssertTrue(app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "09:00")).firstMatch.exists)
        let deleteNode = app.buttons["删除此节次"].firstMatch
        XCTAssertTrue(deleteNode.isHittable)
        deleteNode.tap()
        app.navigationBars.buttons["保存时间表"].tap()
        XCTAssertTrue(app.navigationBars["可复用时间表"].waitForExistence(timeout: 5))
        back(app: app)
        openMenu("课程管理", app: app)
        app.navigationBars.buttons["添加课程"].tap()
        let courseName = app.textFields["课程名称"]
        XCTAssertTrue(courseName.waitForExistence(timeout: 5))
        courseName.tap(); courseName.typeText("待删除测试课程")
        app.buttons["保存课程"].tap()
        let courseRow = app.cells.containing(.staticText, identifier: "待删除测试课程").firstMatch
        XCTAssertTrue(courseRow.waitForExistence(timeout: 5))
        courseRow.swipeLeft()
        app.buttons["删除"].tap()
        app.buttons["删除整门课程"].tap()
        XCTAssertTrue(app.staticTexts["还没有课程，点击添加课程开始安排。"].waitForExistence(timeout: 5))
        back(app: app)
        app.terminate(); app.launch()
        openMenu("课表管理", app: app)
        for _ in 0..<12 {
            if first.isHittable && second.isHittable && max(first.frame.maxY, second.frame.maxY) < app.frame.maxY - 90 { break }
            app.swipeUp()
        }
        XCTAssertLessThan(second.frame.minY, first.frame.minY)
        capture("secondary-management-persisted", app: app)
        back(app: app)
    }

    func testReminderAndDocumentMenus() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        if app.buttons["创建第一张课表"].waitForExistence(timeout: 3) { app.buttons["创建第一张课表"].tap() }
        else { openMenu("新建课表", app: app) }
        let field = app.textFields["课表名称"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap(); field.typeText("提醒验收-" + String(Int(Date().timeIntervalSince1970)))
        app.buttons["创建"].tap()
        openMenu("设置", app: app)
        let reminderLink = app.buttons["提醒内容与时间"]
        for _ in 0..<5 {
            if reminderLink.isHittable { break }
            app.swipeUp()
        }
        reminderLink.tap()
        let start = app.switches["上课提醒"]
        XCTAssertTrue(start.waitForExistence(timeout: 5))
        start.coordinate(withNormalizedOffset: CGVector(dx: 0.93, dy: 0.5)).tap()
        XCTAssertEqual(start.value as? String, "1")
        app.navigationBars.buttons["保存提醒设置"].tap()
        XCTAssertTrue(reminderLink.waitForExistence(timeout: 5))
        reminderLink.tap()
        XCTAssertTrue(start.waitForExistence(timeout: 5))
        XCTAssertEqual(start.value as? String, "1")
        capture("secondary-reminders", app: app)
        back(app: app)
        back(app: app)
        app.buttons["导入文件"].tap()
        let importReady = app.buttons["取消"].waitForExistence(timeout: 30)
        capture("secondary-import-picker", app: app)
        XCTAssertTrue(importReady)
        app.buttons["取消"].tap()
        app.buttons["导出"].tap()
        XCTAssertTrue(app.buttons["JSON 备份"].waitForExistence(timeout: 5))
        app.buttons["JSON 备份"].tap()
        let cancelExport = app.descendants(matching: .any)["取消"].firstMatch
        XCTAssertTrue(cancelExport.waitForExistence(timeout: 8))
        XCTAssertTrue(app.buttons["保存"].exists)
        capture("secondary-export-picker", app: app)
        cancelExport.tap()
        XCTAssertTrue(app.buttons["课表操作"].waitForExistence(timeout: 5))
    }

    func testGridCourseCreationDetailsAndMoveScopes() throws {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(zh-Hans)", "-AppleLocale", "zh_CN"]
        app.launch()
        let create = app.buttons["创建第一张课表"]
        if create.waitForExistence(timeout: 10) {
            create.tap()
        } else {
            app.buttons["课表操作"].tap()
            app.buttons["新建课表"].tap()
        }
        let tableName = app.textFields["课表名称"]
        XCTAssertTrue(tableName.waitForExistence(timeout: 5))
        tableName.tap()
        tableName.typeText("移植验收-" + String(Int(Date().timeIntervalSince1970)))
        app.buttons["创建"].tap()
        let cell = app.descendants(matching: .any)["grid.cell.2.3"].firstMatch
        XCTAssertTrue(cell.waitForExistence(timeout: 10))
        XCTAssertEqual(app.tabBars.count, 0)
        capture("01-week-grid", app: app)
        cell.tap()
        let selection = app.buttons["grid.selection.add"]
        XCTAssertTrue(selection.waitForExistence(timeout: 3))
        capture("02-grid-selection", app: app)
        let selectedFrame = selection.frame
        selection.tap()
        let name = app.textFields["课程名称"]
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        name.tap(); name.typeText("线性代数")
        capture("03-course-draft", app: app)
        app.buttons["保存课程"].tap()
        let course = app.buttons["线性代数"]
        XCTAssertTrue(course.waitForExistence(timeout: 8))
        XCTAssertEqual(course.frame.minX, selectedFrame.minX, accuracy: 3)
        XCTAssertEqual(course.frame.minY, selectedFrame.minY, accuracy: 3)
        capture("04-course-on-grid", app: app)
        let hittable = XCTNSPredicateExpectation(predicate: NSPredicate(format: "isHittable == true"), object: course)
        XCTAssertEqual(XCTWaiter.wait(for: [hittable], timeout: 5), .completed)
        course.tap()
        XCTAssertTrue(app.buttons["编辑课程"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["单次调课"].exists)
        XCTAssertTrue(app.buttons["删除"].exists)
        capture("05-course-details", app: app)
        app.buttons["编辑课程"].tap()
        XCTAssertTrue(name.waitForExistence(timeout: 5))
        XCTAssertEqual(name.value as? String, "线性代数")
        name.tap(); name.typeText("未保存")
        app.buttons["取消"].tap()
        XCTAssertTrue(course.waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["线性代数未保存"].exists)
        course.tap()
        XCTAssertTrue(app.buttons["编辑课程"].waitForExistence(timeout: 5))
        app.buttons["完成"].tap()
        XCTAssertTrue(course.waitForExistence(timeout: 5))
        let original = course.frame
        let target = app.descendants(matching: .any)["grid.cell.3.5"].firstMatch
        course.press(forDuration: 0.6, thenDragTo: target)
        XCTAssertTrue(app.buttons["仅本周"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["全部周次"].exists)
        capture("06-move-scope", app: app)
        app.buttons["取消"].tap()
        XCTAssertTrue(course.waitForExistence(timeout: 5))
        XCTAssertEqual(course.frame.minX, original.minX, accuracy: 3)
        XCTAssertEqual(course.frame.minY, original.minY, accuracy: 3)
        capture("07-move-cancelled", app: app)
        app.terminate(); app.launch()
        XCTAssertTrue(course.waitForExistence(timeout: 10))
        capture("08-persisted-course", app: app)
        XCTAssertEqual(course.frame.minX, original.minX, accuracy: 3)
        XCTAssertEqual(course.frame.minY, original.minY, accuracy: 3)

        course.press(forDuration: 0.6, thenDragTo: target)
        XCTAssertTrue(app.buttons["仅本周"].waitForExistence(timeout: 5))
        app.buttons["仅本周"].tap()
        XCTAssertTrue(course.waitForExistence(timeout: 5))
        let thisWeekFrame = course.frame
        XCTAssertEqual(thisWeekFrame.midX, target.frame.midX, accuracy: 3)
        XCTAssertEqual(thisWeekFrame.minY, target.frame.minY + 2, accuracy: 3)
        capture("09-this-week-saved", app: app)
        chooseWeek(2, app: app)
        XCTAssertEqual(course.frame.minX, original.minX, accuracy: 3)
        XCTAssertEqual(course.frame.minY, original.minY, accuracy: 3)
        chooseWeek(1, app: app)
        XCTAssertEqual(course.frame.minX, thisWeekFrame.minX, accuracy: 3)
        XCTAssertEqual(course.frame.minY, thisWeekFrame.minY, accuracy: 3)

        let allWeeksTarget = app.descendants(matching: .any)["grid.cell.4.6"].firstMatch
        course.press(forDuration: 0.6, thenDragTo: allWeeksTarget)
        XCTAssertTrue(app.buttons["全部周次"].waitForExistence(timeout: 5))
        app.buttons["全部周次"].tap()
        XCTAssertTrue(course.waitForExistence(timeout: 5))
        XCTAssertEqual(course.frame.midX, allWeeksTarget.frame.midX, accuracy: 3)
        XCTAssertEqual(course.frame.minY, allWeeksTarget.frame.minY + 2, accuracy: 3)
        chooseWeek(2, app: app)
        XCTAssertEqual(app.buttons.matching(identifier: "线性代数").count, 1)
        XCTAssertEqual(course.frame.midX, allWeeksTarget.frame.midX, accuracy: 3)
        XCTAssertEqual(course.frame.minY, allWeeksTarget.frame.minY + 2, accuracy: 3)
        capture("10-all-weeks-saved", app: app)
        app.terminate(); app.launch()
        XCTAssertTrue(course.waitForExistence(timeout: 10))
        XCTAssertEqual(app.buttons.matching(identifier: "线性代数").count, 1)
        XCTAssertEqual(course.frame.midX, allWeeksTarget.frame.midX, accuracy: 3)
        XCTAssertEqual(course.frame.minY, allWeeksTarget.frame.minY + 2, accuracy: 3)
        capture("11-moves-persisted", app: app)
        course.tap()
        XCTAssertTrue(app.buttons["删除"].waitForExistence(timeout: 5))
        app.buttons["删除"].tap()
        XCTAssertTrue(app.buttons["仅删除本次"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["当前上课时间的全部周次"].exists)
        XCTAssertTrue(app.buttons["从本周起删除"].exists)
        XCTAssertTrue(app.buttons["删除整门课程"].exists)
        capture("12-delete-scopes", app: app)
        app.buttons["取消"].tap()
        XCTAssertTrue(app.buttons["编辑课程"].waitForExistence(timeout: 5))
        app.buttons["完成"].tap()
        XCTAssertTrue(course.waitForExistence(timeout: 5))
    }
}
