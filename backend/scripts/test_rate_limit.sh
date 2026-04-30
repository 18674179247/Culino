#!/bin/bash

# 限流功能测试脚本

BASE_URL="http://localhost:8080"
LOGIN_ENDPOINT="$BASE_URL/api/v1/user/login"

echo "=========================================="
echo "限流功能测试"
echo "=========================================="
echo ""

# 测试数据
TEST_USER='{"username":"test","password":"test123"}'

echo "测试 1: 正常登录（5次请求，应该全部成功）"
echo "------------------------------------------"
SUCCESS_COUNT=0
RATE_LIMIT_COUNT=0

for i in {1..5}; do
  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$LOGIN_ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$TEST_USER")

  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

  if [ "$HTTP_CODE" = "429" ]; then
    echo "请求 $i: ❌ 被限流 (429)"
    ((RATE_LIMIT_COUNT++))
  else
    echo "请求 $i: ✅ 正常响应 ($HTTP_CODE)"
    ((SUCCESS_COUNT++))
  fi

  sleep 0.1
done

echo ""
echo "结果: $SUCCESS_COUNT 成功, $RATE_LIMIT_COUNT 被限流"
echo ""

if [ $RATE_LIMIT_COUNT -eq 0 ]; then
  echo "✅ 测试通过：正常使用不会触发限流"
else
  echo "❌ 测试失败：正常使用不应该触发限流"
fi

echo ""
echo "=========================================="
echo ""

echo "测试 2: 暴力请求（25次快速请求，应该触发限流）"
echo "------------------------------------------"
SUCCESS_COUNT=0
RATE_LIMIT_COUNT=0

for i in {1..25}; do
  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$LOGIN_ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$TEST_USER")

  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

  if [ "$HTTP_CODE" = "429" ]; then
    echo "请求 $i: 🛑 被限流 (429)"
    ((RATE_LIMIT_COUNT++))
  else
    echo "请求 $i: ✅ 正常响应 ($HTTP_CODE)"
    ((SUCCESS_COUNT++))
  fi
done

echo ""
echo "结果: $SUCCESS_COUNT 成功, $RATE_LIMIT_COUNT 被限流"
echo ""

if [ $RATE_LIMIT_COUNT -gt 0 ]; then
  echo "✅ 测试通过：暴力请求触发了限流保护"
else
  echo "❌ 测试失败：暴力请求应该触发限流"
fi

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
