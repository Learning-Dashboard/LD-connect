package rest;

import java.util.concurrent.Semaphore;
import com.google.common.util.concurrent.RateLimiter;

public class GlobalLimiter {
    private static final GlobalLimiter instance = new GlobalLimiter(); // Static instance to share the same semaphore and rate limiter for all RESTInvoker instances.

    private final Semaphore semaphore;
    private final RateLimiter rateLimiter;

    private GlobalLimiter() {
        semaphore = new Semaphore(25); // Max 25 requests at the same time.
        rateLimiter = RateLimiter.create(25.0); // Max 25 requests per second.
    }

    public static GlobalLimiter getInstance() {
        return instance;
    }

    public void acquire() throws InterruptedException {
        rateLimiter.acquire(); 
        semaphore.acquire();   
    }

    public void release() {
        semaphore.release();
    }
}
