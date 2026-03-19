#!/bin/bash

# ─── Lấy token ───────────────────────────────────────────
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"
echo ""

# ─── Tạo 10 orders và kiểm tra kết quả ──────────────────
for i in {1..10}; do
  ID=$(curl -s -X POST http://localhost:8080/order-service/orders \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"items":[{"productId":"PROD-001","quantity":1,"price":50000}]}' \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

  echo -n "Order $i ($ID): PENDING → "
  sleep 5

  curl -s "http://localhost:8080/order-service/orders/$ID" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])"
done