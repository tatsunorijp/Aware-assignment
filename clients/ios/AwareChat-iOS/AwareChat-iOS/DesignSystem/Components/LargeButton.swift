//
//  LargeButton.swift
//  AwareChat-iOS
//

import SwiftUI

struct LargeButton: View {
  enum Style {
    case primary
    case secondary
  }

  let title: String
  let style: Style
  let action: () -> Void

  init(
    _ title: String,
    style: Style,
    action: @escaping () -> Void
  ) {
    self.title = title
    self.style = style
    self.action = action
  }

  var body: some View {
    Button(action: action) {
      BodyText(title, weight: .medium)
        .foregroundStyle(foregroundColor)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    .frame(height: Tokens.Size.LargeButtonHeight.value)
    .background(backgroundColor)
    .clipShape(Capsule())
  }

  private var backgroundColor: Color {
    switch style {
    case .primary:
      Tokens.Colors.primary
    case .secondary:
      Tokens.Colors.secondary
    }
  }

  private var foregroundColor: Color {
    switch style {
    case .primary:
      Tokens.Colors.background
    case .secondary:
      Tokens.Colors.textPrimary
    }
  }
}

#Preview("Large Button") {
  VStack(spacing: Tokens.Spacing.medium.value) {
    LargeButton("Primary", style: .primary) { }
    LargeButton("Secondary", style: .secondary) { }
  }
  .padding(Tokens.Spacing.medium.value)
  .background(Tokens.Colors.background)
}
