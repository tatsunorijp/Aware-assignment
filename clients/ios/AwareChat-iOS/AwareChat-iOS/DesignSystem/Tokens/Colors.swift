//
//  Colors.swift
//  AwareChat-iOS
//
//  Created by Wellington Tatsunori Asahide on 2026-08-30.
//
import SwiftUI

extension Tokens {
  enum Colors {
    /// Outgoing message backgrounds, primary actions and server-acceptance checkmarks.
    static let primary = Color(.primary)

    /// Incoming message backgrounds and secondary actions such as Cancel.
    static let secondary = Color(.secondary)

    /// Main screen background.
    static let background = Color(.background)

    /// Names, titles and message text on light surfaces.
    static let textPrimary = Color(.textPrimary)

    /// Dates, times and placeholder text.
    static let textSecondary = Color(.textSecondary)

    /// Subtle separators and borders.
    static let divider = Color(.divider)
  }
}
