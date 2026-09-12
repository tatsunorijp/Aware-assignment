import Foundation

nonisolated enum WireDateCodec {
  static func decode(_ value: String) throws -> Date {
    let pattern = #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?(?:Z|\+00:00)$"#
    guard value.range(of: pattern, options: .regularExpression) != nil else {
      throw WireModelError.invalidDate
    }

    let utcSuffixLength = value.hasSuffix("Z") ? 1 : 6
    let timestamp = String(value.dropLast(utcSuffixLength))
    let components = timestamp.split(separator: ".", maxSplits: 1).map(String.init)
    guard let wholeSeconds = wholeSecondsFormatter().date(from: components[0]) else {
      throw WireModelError.invalidDate
    }

    guard components.count == 2 else { return wholeSeconds }
    let fraction = Double(components[1])! / pow(10, Double(components[1].count))
    return wholeSeconds.addingTimeInterval(fraction)
  }

  static func encode(_ date: Date) -> String {
    let interval = date.timeIntervalSince1970
    var wholeSeconds = floor(interval)
    var microseconds = Int(((interval - wholeSeconds) * 1_000_000).rounded())
    if microseconds == 1_000_000 {
      wholeSeconds += 1
      microseconds = 0
    }

    let base = wholeSecondsFormatter().string(
      from: Date(timeIntervalSince1970: wholeSeconds)
    )
    guard microseconds != 0 else { return "\(base)Z" }
    return String(format: "%@.%06dZ", base, microseconds)
  }

  private static func wholeSecondsFormatter() -> DateFormatter {
    let formatter = DateFormatter()
    formatter.calendar = Calendar(identifier: .gregorian)
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
    formatter.isLenient = false
    return formatter
  }
}
