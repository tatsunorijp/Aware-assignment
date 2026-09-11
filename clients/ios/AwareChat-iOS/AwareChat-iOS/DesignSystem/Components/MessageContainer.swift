//
//  MessageContainer.swift
//  AwareChat-iOS
//

import SwiftUI

enum ACKMessageState {
  case sending
  case sent
  case failed
}

struct MessageContainer: View {
  enum Origin {
    case sended
    case received
  }

  let origin: Origin
  let text: String
  let date: Date
  let ackState: ACKMessageState

  var body: some View {
    VStack(alignment: horizontalAlignment, spacing: Tokens.Spacing.xSmall.value) {
      BodyText(text)
        .foregroundStyle(messageTextColor)
        .padding(.horizontal, Tokens.Spacing.medium.value)
        .padding(.vertical, Tokens.Spacing.small.value)
        .background(messageBackgroundColor)
        .clipShape(RoundedRectangle(cornerRadius: Tokens.CornerRadius.medium))

      HStack(spacing: Tokens.Spacing.xSmall.value) {
        Caption2Text(date.messageTime)
          .foregroundStyle(Tokens.Colors.textSecondary)

        if let statusIcon {
          Image(systemName: statusIcon.name)
            .foregroundStyle(statusIcon.color)
            .accessibilityLabel(statusIcon.accessibilityLabel)
        }
      }
    }
    .frame(maxWidth: .infinity, alignment: frameAlignment)
  }

  private var horizontalAlignment: HorizontalAlignment {
    origin == .sended ? .trailing : .leading
  }

  private var frameAlignment: Alignment {
    origin == .sended ? .trailing : .leading
  }

  private var messageBackgroundColor: Color {
    origin == .sended ? Tokens.Colors.primary : Tokens.Colors.secondary
  }

  private var messageTextColor: Color {
    origin == .sended ? Tokens.Colors.background : Tokens.Colors.textPrimary
  }

  private var statusIcon: (name: String, color: Color, accessibilityLabel: String)? {
    guard origin == .sended else { return nil }

    switch ackState {
    case .sending:
      return nil
    case .sent:
      return (Tokens.Icons.checkmark, Tokens.Colors.primary, "Sent to server")
    case .failed:
      return (Tokens.Icons.failed, Tokens.Colors.red, "Failed to send")
    }
  }
}

#Preview("Message Containers") {
  ScrollView {
    VStack(spacing: Tokens.Spacing.medium.value) {
      MessageContainer(
        origin: .received,
        text: "Hi! Thanks for reaching out.",
        date: .now,
        ackState: .sent
      )
      
      MessageContainer(
        origin: .received,
        text: "Hi! Thanks for reaching out. And it is a reallyyyy long message to be sent by me, and is just to test how to component acts when a large message is sent",
        date: .now,
        ackState: .sent
      )

      MessageContainer(
        origin: .sended,
        text: "This message is waiting for the server.",
        date: .now,
        ackState: .sending
      )

      MessageContainer(
        origin: .sended,
        text: "This message was accepted by the server.",
        date: .now,
        ackState: .sent
      )

      MessageContainer(
        origin: .sended,
        text: "This message failed to send. And it is a reallyyyy long message to be sent by me, and is just to test how to component acts when a large message is sent",
        date: .now,
        ackState: .failed
      )
    }
    .padding(Tokens.Spacing.medium.value)
  }
  .background(Tokens.Colors.background)
}
