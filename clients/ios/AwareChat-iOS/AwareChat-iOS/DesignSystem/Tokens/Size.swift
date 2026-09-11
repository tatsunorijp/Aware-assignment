//
//  Size.swift
//  PadelRithm
//
//  Created by Wellington Tatsunori Asahide on 2026-08-31.
//

import Foundation

extension Tokens {
  enum Size: CGFloat {
    case xSmall = 4
    case small = 8
    case medium = 16
    case large = 32
    case xLarge = 64
    case xxLarge = 128
    case xxxLarge = 256
    case xxxxLarge = 512

    var value: CGFloat {
      return self.rawValue
    }
  }
}
