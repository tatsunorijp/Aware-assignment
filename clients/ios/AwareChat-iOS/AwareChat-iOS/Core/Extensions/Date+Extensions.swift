//
//  Date+Extensions.swift
//  AwareChat-iOS
//
//  Created by Wellington Tatsunori Asahide on 2026-09-11.
//

import Foundation

extension Date {
  /// A localized hour-and-minute representation for message metadata.
  var messageTime: String {
    formatted(date: .omitted, time: .shortened)
  }
}
