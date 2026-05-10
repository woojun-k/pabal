-- =====================================================================
-- PostgreSQL Extensions / UUID Utilities
-- =====================================================================
-- 목적
--  - 애플리케이션 전역에서 사용할 PostgreSQL 확장 기능을 활성화한다.
--  - DB 레벨에서도 UUID v7 기본값을 사용할 수 있도록 uuidv7() 함수를 제공한다.
--
-- 운영 원칙
--  - Java/JPA에서는 UuidV7IdGenerator가 주 ID 생성 책임을 가진다.
--  - DB DEFAULT uuidv7()는 수동 SQL, 운영 보정, 테스트 데이터 생성 시의 안전장치다.
--  - Flyway가 DB 스키마의 SSoT이며, Hibernate DDL 생성은 사용하지 않는다.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION public.uuidv7()
RETURNS uuid
LANGUAGE plpgsql
AS $$
DECLARE
unix_ts_ms        bigint;
  ts_hex            text;

  rand_bytes        bytea;
  rand_hex          text;
  rand_a            text;
  rand_rest         text;

  variant_low2      int;
  variant_nibble    int;
  variant_hex       text;

  g1 text;
  g2 text;
  g3 text;
  g4 text;
  g5 text;
BEGIN
  unix_ts_ms := floor(extract(epoch FROM clock_timestamp()) * 1000);
  ts_hex := right(lpad(to_hex(unix_ts_ms), 12, '0'), 12);

  rand_bytes := gen_random_bytes(10);
  rand_hex := encode(rand_bytes, 'hex');

  rand_a := substr(rand_hex, 1, 3);
  rand_rest := substr(rand_hex, 4);

  variant_low2 := get_byte(gen_random_bytes(1), 0) & 3;
  variant_nibble := 8 + variant_low2;
  variant_hex := to_hex(variant_nibble);

  g1 := substr(ts_hex, 1, 8);
  g2 := substr(ts_hex, 9, 4);
  g3 := '7' || rand_a;
  g4 := variant_hex || substr(rand_rest, 1, 3);
  g5 := substr(rand_rest, 4, 12);

RETURN (g1 || '-' || g2 || '-' || g3 || '-' || g4 || '-' || g5)::uuid;
END;
$$;