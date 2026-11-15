package com.learn.english.component;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

final class UserUpdateQueue {
    private static final long IDLE_MILLIS = 300_000;

    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayDeque<PendingUpdate> pending = new ArrayDeque<>();
    private boolean scheduled;
    private boolean retired;
    private long idleSince = System.currentTimeMillis();

    Accept enqueue(PendingUpdate update) {
        lock.lock();
        try {
            if (retired) {
                return Accept.RETIRED;
            }
            pending.add(update);
            idleSince = 0;
            if (scheduled) {
                return Accept.QUEUED;
            }
            scheduled = true;
            return Accept.START;
        } finally {
            lock.unlock();
        }
    }

    PendingUpdate poll() {
        lock.lock();
        try {
            PendingUpdate update = pending.poll();
            if (update == null) {
                scheduled = false;
                idleSince = System.currentTimeMillis();
            }
            return update;
        } finally {
            lock.unlock();
        }
    }

    boolean retireIfIdle(long now) {
        lock.lock();
        try {
            if (!scheduled && pending.isEmpty() && idleSince > 0 && now - idleSince > IDLE_MILLIS) {
                retired = true;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    enum Accept {
        START, QUEUED, RETIRED
    }

    record PendingUpdate(Update update, CompletableFuture<Void> done) {
    }
}
