Summary

  Phase 1: User Role

  - Created UserRole.java enum with USER and ADMIN values
  - Updated User.java domain with role field and isAdmin() method
  - Updated UserEntity.java with role column
  - Updated UserResponse.java to include role

  Phase 2: Match Status

  - Created MatchStatus.java enum with DRAFT and OPEN values
  - Updated Match.java domain with status field, isOpen(), open(), and update() methods
  - Updated MatchEntity.java with status column
  - Updated MatchResponse.java to include status
  - Created MatchNotOpenException.java and MatchAlreadyOpenException.java
  - Created UnauthorizedException.java in common exception package

  Phase 3: Admin Match CRUD

  - Created CreateMatchCommand.java and UpdateMatchCommand.java
  - Created Use Case interfaces: CreateMatchUseCase, UpdateMatchUseCase, DeleteMatchUseCase,
  OpenMatchUseCase
  - Updated RecordMatchPort.java with delete() method
  - Updated MatchPersistenceAdapter.java with delete implementation
  - Updated MatchService.java with CRUD operations

  Phase 4: Match Open + Allocation Pre-populate

  - Added loadAllSeats() to LoadSeatPort.java
  - Added saveAll() to RecordAllocationPort.java
  - Added availableForMatch() factory method to Allocation.java
  - Implemented in SitePersistenceAdapter.java and AllocationPersistenceAdapter.java
  - Implemented openMatch() in MatchService with allocation pre-population

  Phase 5: Admin Controller

  - Created CreateMatchRequest.java and UpdateMatchRequest.java
  - Created AdminMatchController.java with endpoints:
    - POST /api/admin/matches - Create match
    - PUT /api/admin/matches/{matchId} - Update match
    - DELETE /api/admin/matches/{matchId} - Delete match
    - POST /api/admin/matches/{matchId}/open - Open match

  Phase 6: Open Check in Services

  - Added match open validation to AllocationService.allocateSeat()
  - Added match open validation to ReservationService.createReservation()

  Test Fixes

  - Updated MatchSteps.java to open matches after creation in acceptance tests
  - Updated SeatAllocationConcurrencyTest.java to ensure match is OPEN before tests

  All tests pass successfully (174 tests, 0 failures).