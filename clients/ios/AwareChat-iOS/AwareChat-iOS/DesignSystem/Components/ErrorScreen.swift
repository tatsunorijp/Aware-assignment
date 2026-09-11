//
//  ErrorScreen.swift
//  AwareChat-iOS
//

import SwiftUI

struct ErrorScreen: View {
  @Environment(\.dismiss) private var dismiss

  let message: String
  let retryAction: () -> Void

  init(
    message: String,
    retryAction: @escaping () -> Void
  ) {
    self.message = message
    self.retryAction = retryAction
  }

  var body: some View {
    ZStack {
      Tokens.Colors.background
        .ignoresSafeArea()

      VStack(spacing: Tokens.Spacing.medium.value) {
        BodyText(message)
          .foregroundStyle(Tokens.Colors.textPrimary)
          .multilineTextAlignment(.center)

        LargeButton("Retry", style: .primary, action: retryAction)

        LargeButton("Cancel", style: .secondary) {
          dismiss()
        }
      }
      .padding(.horizontal, Tokens.Spacing.large.value)
    }
  }
}

private struct ErrorScreenPreview: View {
  @State private var isPresented = true
  @State private var retryCount = 0

  var body: some View {
    VStack(spacing: Tokens.Spacing.medium.value) {
      BodyText("Underlying content")

      LargeButton("Show Error", style: .primary) {
        isPresented = true
      }
      .padding(.horizontal, Tokens.Spacing.large.value)
    }
    .sheet(isPresented: $isPresented) {
      ErrorScreen(
        message: "Something went wrong...\nRetry count: \(retryCount)"
      ) {
        retryCount += 1
      }
    }
  }
}

#Preview("Error Screen") {
  ErrorScreenPreview()
}
