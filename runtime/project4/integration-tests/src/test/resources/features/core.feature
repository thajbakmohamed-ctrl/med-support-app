Feature: Project 4 API and event contracts
  Scenario: Booking notification crosses the service boundary
    Given a valid booking notification
    When Notification consumes the event
    Then the notification is submitted once for persistence

  Scenario: Invalid notification cannot reach persistence
    Given an invalid booking notification
    When Notification consumes the event
    Then the event is rejected without persistence

  Scenario: Booking history requires authentication
    When a visitor requests their bookings without a bearer token
    Then the response is unauthorized
