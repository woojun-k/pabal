package com.polarishb.pabal.security.context;

import jakarta.servlet.ServletException;

import java.io.IOException;

@FunctionalInterface
public interface ServletFilterOperation {

    void filter() throws ServletException, IOException;
}
