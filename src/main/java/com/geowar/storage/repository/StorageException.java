package com.geowar.storage.repository;

/**
 * Unchecked wrapper for {@link java.sql.SQLException} raised in the repository
 * layer. Callers run repository work through the async executor, which logs the
 * cause; wrapping keeps the repository method signatures free of checked
 * exceptions without swallowing the failure.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
