package com.gamepilot
import org.junit.Assert.*
import org.junit.Test
class ImportantGameNewsTest {
  @Test fun avoidAlertingEveryNormalUpdate() {
    assertTrue(ImportantGameNews.isCritical("Emergency maintenance notice"))
    assertTrue(ImportantGameNews.isCritical("End of service announced"))
    assertFalse(ImportantGameNews.isCritical("New banner update tomorrow"))
    assertFalse(ImportantGameNews.isCritical("New costume and 5-star character"))
  }
}
