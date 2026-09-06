import Foundation
import SwiftUI
import WidgetKit

enum AppLanguage: String, CaseIterable, Identifiable {
    case system
    case simplifiedChinese = "zh-Hans"
    case english = "en"

    static let storageKey = "appLanguage"

    var id: String { rawValue }

    var locale: Locale {
        AppLocalization.locale(for: self)
    }
}

enum AppLocalization {
    static var currentLanguage: AppLanguage {
        let standardValue = UserDefaults.standard.string(forKey: AppLanguage.storageKey)
        let sharedValue = UserDefaults(suiteName: WidgetSharedData.appGroupID)?
            .string(forKey: AppLanguage.storageKey)
        return AppLanguage(rawValue: standardValue ?? sharedValue ?? "") ?? .system
    }

    static var currentLocale: Locale {
        locale(for: currentLanguage)
    }

    static func locale(for language: AppLanguage) -> Locale {
        switch language {
        case .system:
            return .autoupdatingCurrent
        case .simplifiedChinese:
            return Locale(identifier: "zh-Hans")
        case .english:
            return Locale(identifier: "en")
        }
    }

    static func locale(for rawValue: String) -> Locale {
        locale(for: AppLanguage(rawValue: rawValue) ?? .system)
    }

    static func synchronizeWidgetLanguage(rawValue: String? = nil) {
        let language = AppLanguage(rawValue: rawValue ?? currentLanguage.rawValue) ?? .system
        UserDefaults(suiteName: WidgetSharedData.appGroupID)?.set(
            language.rawValue,
            forKey: AppLanguage.storageKey
        )
        WidgetCenter.shared.reloadAllTimelines()
    }

    static func string(_ key: String, _ arguments: CVarArg...) -> String {
        let format = localizedBundle().localizedString(forKey: key, value: key, table: nil)
        guard !arguments.isEmpty else { return format }
        return String(format: format, locale: currentLocale, arguments: arguments)
    }

    private static func localizedBundle() -> Bundle {
        let identifier = resourceLanguageIdentifier(for: currentLanguage)
        let candidates = [Bundle.main, Bundle(for: LocalizationBundleToken.self)]
        for baseBundle in candidates {
            if let path = baseBundle.path(forResource: identifier, ofType: "lproj"),
               let bundle = Bundle(path: path) {
                return bundle
            }
        }
        return Bundle.main
    }

    private static func resourceLanguageIdentifier(for language: AppLanguage) -> String {
        switch language {
        case .simplifiedChinese:
            return "zh-Hans"
        case .english:
            return "en"
        case .system:
            let languageCode = Locale.autoupdatingCurrent.languageCode?.lowercased() ?? "en"
            return languageCode == "zh" || languageCode.hasPrefix("zh-") ? "zh-Hans" : "en"
        }
    }
}

private final class LocalizationBundleToken: NSObject {}
