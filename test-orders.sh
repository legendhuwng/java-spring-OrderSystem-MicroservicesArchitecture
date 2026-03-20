#!/bin/bash

# ─── Lấy token ───────────────────────────────────────────
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"
echo ""

# ─── Hàm theo dõi status của 1 order ────────────────────
watch_order() {
  local ORDER_NUM=$1
  local ID=$2
  local PREV_STATUS=""
  local MAX_WAIT=60   # timeout sau 60 giây
  local ELAPSED=0

  echo "Order $ORDER_NUM ($ID):"

  while [ $ELAPSED -lt $MAX_WAIT ]; do
    STATUS=$(curl -s "http://localhost:8080/order-service/orders/$ID" \
      -H "Authorization: Bearer $TOKEN" \
      | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)

    if [ "$STATUS" != "$PREV_STATUS" ] && [ -n "$STATUS" ]; then
      TIMESTAMP=$(date '+%H:%M:%S')
      if [ -z "$PREV_STATUS" ]; then
        echo "  [$TIMESTAMP] $STATUS"
      else
        echo "  [$TIMESTAMP] $PREV_STATUS → $STATUS"
      fi
      PREV_STATUS="$STATUS"

      # Dừng khi đạt trạng thái cuối
      if [[ "$STATUS" == "COMPLETED" || "$STATUS" == "CANCELLED" ]]; then
        echo "  ✓ Done"
        return
      fi
    fi

    sleep 1
    ELAPSED=$((ELAPSED + 1))
  done

  echo "  ✗ Timeout sau ${MAX_WAIT}s, status cuối: $PREV_STATUS"
}

# ─── Tạo 10 orders và theo dõi từng cái ─────────────────
for i in {1..10}; do
  ID=$(curl -s -X POST http://localhost:8080/order-service/orders \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"items":[{"productId":"PROD-001","quantity":1,"price":50000}]}' \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

  watch_order $i "$ID"
  echo ""
done

echo ""
echo "=== Full transition log từ Docker ==="
docker logs order-service 2>&1 | grep -E "status updated|INVENTORY_CHECKING|PAYMENT_PROCESSING|SHIPPING|COMPLETED|CANCELLED" | tail -30