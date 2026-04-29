package com.polarishb.pabal.common.cqrs;

public interface QueryHandler<Q extends Query, R> {
    R handle(Q query);
}
