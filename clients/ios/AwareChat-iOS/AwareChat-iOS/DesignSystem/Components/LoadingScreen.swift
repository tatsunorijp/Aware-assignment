//
//  LoadingScreen.swift
//  AwareChat-iOS
//

import SwiftUI

struct LoadingScreen: View {
  var body: some View {
    ZStack {
      Tokens.Colors.background
        .ignoresSafeArea()

      VStack(spacing: Tokens.Spacing.medium.value) {
        ProgressView()
          .controlSize(.large)

        BodyText("Loading...")
          .foregroundStyle(Tokens.Colors.textSecondary)
      }
    }
    .accessibilityElement(children: .combine)
    .accessibilityLabel("Loading")
  }
}

#Preview("Loading Screen") {
  LoadingScreen()
}
