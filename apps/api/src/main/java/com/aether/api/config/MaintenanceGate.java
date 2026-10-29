package com.aether.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Let in-flight imports finish before replacing or clearing their local data. */
@Component
@Order(20)
public class MaintenanceGate extends OncePerRequestFilter {
    private final ReentrantReadWriteLock gate = new ReentrantReadWriteLock(true);
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path=request.getRequestURI();
        if(!path.startsWith("/api/")){chain.doFilter(request,response);return;}
        boolean maintenance=path.equals("/api/data/reset")||path.equals("/api/data/restore")||path.equals("/api/data/export");
        var lock=maintenance?gate.writeLock():gate.readLock();
        lock.lock();try{chain.doFilter(request,response);}finally{lock.unlock();}
    }
}
