package dev.ticketing.acceptance.context;

import dev.ticketing.acceptance.client.model.TestResponse;
import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TestContext {

    private TestResponse response;
    
    // 지원하는 여러 경기들의 ID 관리 (Key: 시나리오상 임시 ID, Value: 실제 DB ID)
    private final Map<Long, Long> matchIdMap = new HashMap<>();
    
    private final Map<String, Long> userEmailMap = new HashMap<>();
    private String currentUserEmail;

    // 현재 시나리오에서 점유(Hold)된 좌석들의 ID 목록
    private final List<Long> heldSeatIds = new ArrayList<>();
    
    // 마지막으로 작업한 좌석의 위치 정보 (검증용)
    private Long lastHeldBlockId;
    private int lastHeldRow;
    private int lastHeldCol;

    public void saveMatchId(Long alias, Long internalId) {
        matchIdMap.put(alias, internalId);
    }

    public Long getMatchId(Long alias) {
        return matchIdMap.get(alias);
    }

    public void addHeldSeatId(Long seatId) {
        if (!heldSeatIds.contains(seatId)) {
            heldSeatIds.add(seatId);
        }
    }

    public void setLastHeldPosition(Long blockId, int row, int col) {
        this.lastHeldBlockId = blockId;
        this.lastHeldRow = row;
        this.lastHeldCol = col;
    }

    public Long getLastHeldSeatId() {
        if (heldSeatIds.isEmpty()) return null;
        return heldSeatIds.get(heldSeatIds.size() - 1);
    }

    public void clearHeldSeats() {
        heldSeatIds.clear();
        lastHeldBlockId = null;
        lastHeldRow = 0;
        lastHeldCol = 0;
    }

    private Long lastReservationId;

    public int getStatusCode() {
        return response == null ? 0 : response.getStatusCode();
    }

    public <T> T getBody(Class<T> classType) {
        return response.as(classType);
    }

    public String getStringFromJsonPath(String jsonPath) {
        return response.jsonPath().getString(jsonPath);
    }

    public void saveUserId(String email, Long internalId) {
        userEmailMap.put(email, internalId);
    }

    public Long getUserId(String email) {
        return userEmailMap.get(email);
    }

    public Long getCurrentUserId() {
        return getUserId(currentUserEmail);
    }

    private final Map<String, Object> data = new HashMap<>();

    public void set(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }
}
