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
--
-- 구현 정책
--  - UUIDv7 layout:
--      [ timestamp_ms 48bit | version 4bit | rand_a 12bit ]
--      [ variant 2bit | rand_b 62bit ]
--
--  - monotonic state:
--      state = (timestamp_ms << 12) | rand_a
--
--  - 현재 ms > 이전 ms:
--      새 timestamp_ms + random rand_a
--
--  - 현재 ms <= 이전 ms:
--      이전 timestamp_ms 유지 + rand_a 증가
--
--  - rand_a overflow:
--      lock을 잡은 채 기다리지 않고,
--      lock 해제 → sleep → lock 재획득 → 상태 재확인
--
-- 주의
--  - public.uuidv7_state_seq는 uuidv7() 내부 상태 저장용이다.
--  - 외부에서 nextval()/setval()을 직접 호출하지 않는다.
--  - 애플리케이션/운영 SQL에서 동일 advisory lock key를 직접 사용하지 않는다.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SEQUENCE IF NOT EXISTS public.uuidv7_state_seq
    AS bigint
    INCREMENT BY 1
    MINVALUE 0
    MAXVALUE 1152921504606846975 -- 2^60 - 1 = 48bit timestamp + 12bit rand_a
    START WITH 0
    CACHE 1
    NO CYCLE;

COMMENT ON SEQUENCE public.uuidv7_state_seq
  IS 'Internal monotonic state for public.uuidv7(). Do not call nextval() or setval() directly.';

CREATE OR REPLACE FUNCTION public.uuidv7()
RETURNS uuid
LANGUAGE plpgsql
VOLATILE
PARALLEL UNSAFE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
ts_mask_48             constant bigint  := 281474976710655; -- 0xFFFFFFFFFFFF
  rand_a_mask_12_bigint  constant bigint  := 4095;             -- 0x0FFF
  rand_a_mask_12_int     constant integer := 4095;             -- 0x0FFF
  max_retry_attempts     constant integer := 1000;

  -- Internal advisory lock key for uuidv7 monotonic state.
  -- Do not use the same key from application or operational SQL.
  lock_key               bigint := hashtextextended('public.uuidv7.monotonic', 0);
  locked                 boolean := false;

  now_ts_ms              bigint;
  prev_state             bigint;
  prev_ts_ms             bigint;
  prev_rand_a            integer;

  next_ts_ms             bigint;
  next_rand_a            integer;
  next_state             bigint;

  retry_count            integer := 0;
  frozen_ts_ms           bigint := NULL;

  rand2                  bytea;
  rand_b_bytes           bytea;

  ts_hex                 text;
  rand_a_hex             text;
  lsb_hex                text;

  g1                     text;
  g2                     text;
  g3                     text;
  g4                     text;
  g5                     text;
BEGIN
  LOOP
    /*
     * Java AtomicLong.compareAndSet(prev, next)에 해당하는 임계 구간.
     *
     * session-level advisory lock을 사용하되,
     * rand_a overflow로 기다려야 할 때는 반드시 unlock 후 sleep한다.
     */
PERFORM pg_advisory_lock(lock_key);
    locked := true;

SELECT last_value
INTO prev_state
FROM public.uuidv7_state_seq;

prev_ts_ms := prev_state >> 12;
    prev_rand_a := (prev_state & rand_a_mask_12_bigint)::integer;

    now_ts_ms := (
      floor(extract(epoch FROM clock_timestamp()) * 1000)::bigint
      & ts_mask_48
    );

    IF now_ts_ms > prev_ts_ms THEN
      rand2 := gen_random_bytes(2);

      next_rand_a := (
        (get_byte(rand2, 0) * 256 + get_byte(rand2, 1))
        & rand_a_mask_12_int
      );

      next_ts_ms := now_ts_ms;

ELSE
      next_ts_ms := prev_ts_ms;
      next_rand_a := (prev_rand_a + 1) & rand_a_mask_12_int;

      IF next_rand_a = 0 THEN
        frozen_ts_ms := prev_ts_ms;

        PERFORM pg_advisory_unlock(lock_key);
        locked := false;

        retry_count := retry_count + 1;

        IF retry_count > max_retry_attempts THEN
          RAISE EXCEPTION
            'UUIDv7 generation rate exceeded or system clock rollback did not recover; timestamp_ms=%',
            frozen_ts_ms;
END IF;

        PERFORM pg_sleep(0.0001); -- 0.1ms requested; actual resolution depends on OS/DB scheduling.
CONTINUE;
END IF;
END IF;

    next_state := (next_ts_ms << 12) | next_rand_a::bigint;

    /*
     * sequence 상태 갱신.
     * setval은 transaction rollback으로 되돌아가지 않는다.
     */
    PERFORM setval('public.uuidv7_state_seq'::regclass, next_state, true);

    PERFORM pg_advisory_unlock(lock_key);
    locked := false;

    EXIT;
END LOOP;

  /*
   * rand_b 62bit 생성.
   *
   * UUID 하위 64bit:
   *   [ variant 2bit = 10 | rand_b 62bit ]
   */
  rand_b_bytes := gen_random_bytes(8);

  rand_b_bytes := set_byte(
    rand_b_bytes,
    0,
    ((get_byte(rand_b_bytes, 0) & 63) | 128)
  );

  ts_hex := lpad(to_hex(next_ts_ms & ts_mask_48), 12, '0');
  rand_a_hex := lpad(to_hex(next_rand_a), 3, '0');
  lsb_hex := encode(rand_b_bytes, 'hex');

  g1 := substr(ts_hex, 1, 8);
  g2 := substr(ts_hex, 9, 4);
  g3 := '7' || rand_a_hex;
  g4 := substr(lsb_hex, 1, 4);
  g5 := substr(lsb_hex, 5, 12);

RETURN (g1 || '-' || g2 || '-' || g3 || '-' || g4 || '-' || g5)::uuid;

EXCEPTION
  WHEN OTHERS THEN
    IF locked THEN
      PERFORM pg_advisory_unlock(lock_key);
END IF;
    RAISE;
END;
$$;

COMMENT ON FUNCTION public.uuidv7()
  IS 'Generates RFC4122-variant UUIDv7 using a database-local monotonic timestamp/rand_a state. Intended as DB fallback, not hot-path application ID generation.';

REVOKE ALL ON FUNCTION public.uuidv7() FROM PUBLIC;
REVOKE ALL ON SEQUENCE public.uuidv7_state_seq FROM PUBLIC;

-- 필요 시 실제 애플리케이션 DB role에만 허용
-- GRANT EXECUTE ON FUNCTION public.uuidv7() TO app_user;