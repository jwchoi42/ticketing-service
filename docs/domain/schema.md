---
trigger: model_decision
description: domain-schema
---

Database Schema Definition
===
- This document defines the database structure using **DBML (Database Markup Language)** which is compatible with [dbdiagram.io](https://dbdiagram.io).
- Claude should refer to this schema when generating queries, designing API endpoints, or analyzing data relationships.
- Use DBML to define your database structure / Docs: https://dbml.dbdiagram.io/docs

Schema Design Rules
===
- **Naming Convention**: All table names must be in plural form.
- **Referential Integrity**: Relationships are defined using the `Ref` syntax. Pay close attention to the one-to-many and one-to-one mappings.
- **Enum Types**: Enum values are stored as VARCHAR in the database but mapped to Java enums in the application layer.

Schema Details
===

``` DBML
Table users {
  id integer [primary key]
  email varchar
  password varchar
}

Ref: users.id < reservations.user_id

Table payments {
  id integer [primary key]
  reservation_id integer
  amount integer
  method varchar
  status varchar // PENDING, PAID, FAILED
  payment_gateway_provider varchar
  payment_transaction_id varchar
  created_at timestamp
  paid_at timestamp  
}

Ref: reservations.id < payments.reservation_id

Table matches {
  id integer [primary key]
  stadium varchar
  home_team varchar
  away_team varchar
  date_time timestamp
}

Ref: matches.id < reservations.match_id

Table reservations {
  id integer [primary key]
  user_id integer
  match_id integer
  status varchar // PENDING, CONFIRMED, CANCELLED
}

Table allocations {
  id integer [primary key]
  reservation_id integer [null]
  match_id integer [not null]
  block_id integer [not null]
  seat_id integer [not null]
  status varchar [not null] // AVAILABLE, HOLD, OCCUPIED (mapped to AllocationStatus enum)
  hold_expires_at timestamp [null] // TTL for temporary seat hold
  
  Indexes {
    (match_id, seat_id) [unique] // Ensures one allocation per seat per match
    (match_id, block_id) // Optimized lookup for seat status by block
  }
  
  Note: '''
  - status: Maps to dev.ticketing.core.site.domain.allocation.AllocationStatus enum
  - AVAILABLE: Seat is available for selection
  - HOLD: Temporarily held by a user (expires after hold_expires_at)
  - OCCUPIED: Permanently reserved after payment
  - hold_expires_at: Used for automatic cleanup of expired holds
  '''
}

Ref: matches.id < allocations.match_id
Ref: seats.id < allocations.seat_id
Ref: reservations.id < allocations.reservation_id

Table seats {
  id integer [primary key]
  block_id integer
  row_number integer
  seat_number integer
}

Ref: blocks.id < seats.block_id

Table blocks {
  id integer [primary key]
  section_id integer
  name varchar
}

Ref: sections.id < blocks.section_id

Table sections {
  id integer [primary key]
  area_id integer
  name varchar // HOME, AWAY, LEFT, RIGHT
}

Ref: areas.id < sections.area_id

Table areas {
  id integer [primary key]
  name varchar // INFIELD, OUTFIELD
}
```

RDB-First Implementation Notes
===

Seat Allocation Strategy
---
- **Pessimistic Locking**: Use `SELECT FOR UPDATE` on allocations table to prevent race conditions
- **TTL Management**: Scheduled job cleans up expired HOLD status allocations
- **Transaction Boundaries**: 
  - Seat selection: Single transaction for status update
  - Reservation confirmation: Transaction spanning allocation + reservation creation

Index Strategy
---
- `(match_id, seat_id)` unique index ensures data integrity
- Consider adding index on `(match_id, status)` for efficient seat availability queries
- Consider adding index on `hold_expires_at` for cleanup job performance

Future Enhancements
---
- Add `created_at`, `updated_at` timestamps to all tables for audit trail
- Consider partitioning `allocations` table by `match_id` for large-scale events
- Add soft delete support with `deleted_at` column if needed
