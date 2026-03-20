#!/bin/bash
# ─── Config ──────────────────────────────────────────────
PRODUCT_ID="PROD-001"
INITIAL_STOCK=20
ORDER_QTY=1
ORDER_COUNT=10

# ─── Lấy token ───────────────────────────────────────────
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"
echo ""

# ─── Hàm lấy stock hiện tại ──────────────────────────────
get_stock() {
  curl -s "http://localhost:8080/inventory-service/inventory/$PRODUCT_ID" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('quantity','?'))" 2>/dev/null
}

# ─── Seed stock ban đầu ──────────────────────────────────
echo "=== Seeding stock ==="
curl -s -X POST "http://localhost:8080/inventory-service/inventory/$PRODUCT_ID/stock?quantity=$INITIAL_STOCK" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

# FIX 1: đọc stock thực tế sau seed làm baseline
ACTUAL_INITIAL=$(get_stock)
echo "  $PRODUCT_ID: +$INITIAL_STOCK units seeded"
echo "  Stock thực tế hiện tại: $ACTUAL_INITIAL units"
echo ""

# ─── Hàm theo dõi status của 1 order ────────────────────
watch_order() {
  local ORDER_NUM=$1
  local ID=$2
  local PREV_STATUS=""
  local MAX_WAIT=60   # timeout sau 60 giây
  local ELAPSED=0

  local STOCK_BEFORE=$(get_stock)
  echo "Order $ORDER_NUM ($ID) | stock trước: $STOCK_BEFORE"

  while [ $ELAPSED -lt $MAX_WAIT ]; do
    STATUS=$(curl -s "http://localhost:8080/order-service/orders/$ID" \
      -H "Authorization: Bearer $TOKEN" \
      | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)

    if [ "$STATUS" != "$PREV_STATUS" ] && [ -n "$STATUS" ]; then
      TIMESTAMP=$(date '+%H:%M:%S')
      STOCK_NOW=$(get_stock)

      if [ -z "$PREV_STATUS" ]; then
        echo "  [$TIMESTAMP] $STATUS  (stock: $STOCK_NOW)"
      else
        echo "  [$TIMESTAMP] $PREV_STATUS → $STATUS  (stock: $STOCK_NOW)"
      fi
      PREV_STATUS="$STATUS"

      if [[ "$STATUS" == "COMPLETED" || "$STATUS" == "CANCELLED" ]]; then
        # FIX 2: chờ 3 giây khi CANCELLED để releaseInventory kịp chạy xong
        [ "$STATUS" == "CANCELLED" ] && sleep 3
        STOCK_AFTER=$(get_stock)
        if [ "$STATUS" == "COMPLETED" ]; then
          DIFF=$(( STOCK_BEFORE - STOCK_AFTER ))
          echo "  ✓ Done | stock sau: $STOCK_AFTER  (-$DIFF units sold)"
        else
          if [ "$STOCK_BEFORE" == "$STOCK_AFTER" ]; then
            echo "  ✓ Done | stock sau: $STOCK_AFTER  (rollback: OK ✓)"
          else
            echo "  ✓ Done | stock sau: $STOCK_AFTER  (rollback: FAIL ✗)"
          fi
        fi
        return
      fi
    fi

    sleep 1
    ELAPSED=$((ELAPSED + 1))
  done

  echo "  ✗ Timeout sau ${MAX_WAIT}s, status cuối: $PREV_STATUS"
}

# ─── Tạo orders và theo dõi ──────────────────────────────
echo "=== Running $ORDER_COUNT orders (qty=$ORDER_QTY each) ==="
echo ""

COMPLETED_COUNT=0
CANCELLED_COUNT=0

for i in $(seq 1 $ORDER_COUNT); do
  RESPONSE=$(curl -s -X POST http://localhost:8080/order-service/orders \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"items\":[{\"productId\":\"$PRODUCT_ID\",\"quantity\":$ORDER_QTY,\"price\":50000}]}")

  ID=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

  if [ -z "$ID" ]; then
    echo "Order $i: Failed to create"
    continue
  fi

  watch_order $i "$ID"

  FINAL=$(curl -s "http://localhost:8080/order-service/orders/$ID" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
  [[ "$FINAL" == "COMPLETED" ]] && COMPLETED_COUNT=$((COMPLETED_COUNT + 1))
  [[ "$FINAL" == "CANCELLED" ]] && CANCELLED_COUNT=$((CANCELLED_COUNT + 1))

  echo ""
done

# ─── Tổng kết ─────────────────────────────────────────────
# FIX 3: chờ thêm để đảm bảo tất cả release đã xong
sleep 3
FINAL_STOCK=$(get_stock)
# FIX 1: dùng ACTUAL_INITIAL thay vì INITIAL_STOCK hardcode
EXPECTED_STOCK=$(( ACTUAL_INITIAL - COMPLETED_COUNT * ORDER_QTY ))

echo "======================================="
echo "  Tổng orders  : $ORDER_COUNT"
echo "  COMPLETED     : $COMPLETED_COUNT"
echo "  CANCELLED     : $CANCELLED_COUNT"
echo "  Stock ban đầu : $ACTUAL_INITIAL"
echo "  Stock cuối    : $FINAL_STOCK"
echo "  Stock kỳ vọng : $EXPECTED_STOCK  (initial - completed*qty)"
if [ "$FINAL_STOCK" == "$EXPECTED_STOCK" ]; then
  echo "  Kiểm tra stock: ✓ PASS"
else
  echo "  Kiểm tra stock: ✗ FAIL (lệch $(( EXPECTED_STOCK - FINAL_STOCK )) units)"
fi
echo "======================================="
echo ""
echo "=== Full transition log từ Docker ==="
docker logs order-service 2>&1 | grep -E "status updated|INVENTORY_CHECKING|PAYMENT_PROCESSING|SHIPPING|COMPLETED|CANCELLED" | tail -30