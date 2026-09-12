//
//  ErrorScreen.swift
//  AwareChat-iOS
//

import SwiftUI

struct ErrorScreen: View {
  @Environment(\.dismiss) private var dismiss

  let message: String
  let retryAction: (() -> Void)?
  let cancelAction: (() -> Void)?
  let showsCancel: Bool

  init(
    message: String,
    retryAction: (() -> Void)?,
    cancelAction: (() -> Void)? = nil,
    showsCancel: Bool = true
  ) {
    self.message = message
    self.retryAction = retryAction
    self.cancelAction = cancelAction
    self.showsCancel = showsCancel
  }

  var body: some View {
    ZStack {
      Tokens.Colors.background
        .ignoresSafeArea()

      VStack(spacing: Tokens.Spacing.medium.value) {
        BodyText(message)
          .foregroundStyle(Tokens.Colors.textPrimary)
          .multilineTextAlignment(.center)

        if let retryAction {
          LargeButton("Retry", style: .primary, action: retryAction)
        }

        if showsCancel {
          LargeButton("Cancel", style: .secondary) {
            if let cancelAction {
              cancelAction()
            } else {
              dismiss()
            }
          }
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
        message: "Something went wrong...\nRetry count: \(retryCount)",
        retryAction: { retryCount += 1 }
      )
    }
  }
}

#Preview("Error Screen") {
  ErrorScreenPreview()
}
