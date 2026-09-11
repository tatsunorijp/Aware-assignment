//
//  Spacing.swift
//  PadelRithm
//
//  Created by Wellington Tatsunori Asahide on 2026-08-31.
//

import Foundation

extension Tokens {
  enum Spacing: CGFloat {
    case xSmall = 4
    case small = 8
    case medium = 16
    case large = 32
    case xLarge = 64
    case xxLarge = 128

    var value: CGFloat {
      return self.rawValue
    }
  }
}
