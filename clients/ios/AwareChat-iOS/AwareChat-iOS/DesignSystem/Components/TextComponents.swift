//
//  TextComponents.swift
//  AwareChat-iOS
//
//  Created by Wellington Tatsunori Asahide on 2026-09-11.
//

import SwiftUI

struct LargeTitleText: View {
  let text: String
  let weight: Font.Weight

  init(_ text: String, weight: Font.Weight = .regular) {
    self.text = text
    self.weight = weight
  }

  var body: some View {
    Text(text)
      .font(.largeTitle)
      .fontWeight(weight)
  }
}

struct TitleText: View {
  let text: String
  let weight: Font.Weight

  init(_ text: String, weight: Font.Weight = .regular) {
    self.text = text
    self.weight = weight
  }

  var body: some View {
    Text(text)
      .font(.title)
      .fontWeight(weight)
  }
}

struct HeadlineText: View {
  let text: String
  let weight: Font.Weight

  init(_ text: String, weight: Font.Weight = .regular) {
    self.text = text
    self.weight = weight
  }

  var body: some View {
    Text(verbatim: text)
      .font(.headline)
      .fontWeight(weight)
  }
}

struct SubheadlineText: View {
  let text: String
  let weight: Font.Weight

  init(_ text: String, weight: Font.Weight = .regular) {
    self.text = text
    self.weight = weight
  }

  var body: some View {
    Text(text)
      .font(.subheadline)
      .fontWeight(weight)
  }
}

struct BodyText: View {
  let text: String
  let weight: Font.Weight

  init(_ text: String, weight: Font.Weight = .regular) {
    self.text = text
    self.weight = weight
  }

  var body: some View {
    Text(text)
      .font(.body)
      .fontWeight(weight)
  }
}

struct FootnoteText: View {
  let text: String
  let weight: Font.Weight
  
  init(_ text: String, weight: Font.Weight = .regular) {
    self.text = text
    self.weight = weight
  }
  
  var body: some View {
    Text(text)
      .font(.footnote)
      .fontWeight(weight)
  }
}

struct Caption2Text: View {
  let text: String
  let weight: Font.Weight
  
  init(_ text: String, weight: Font.Weight = .regular) {
    self.text = text
    self.weight = weight
  }
  
  var body: some View {
    Text(verbatim: text)
      .font(.caption2)
      .fontWeight(weight)
  }
}
