package com.devsusana.hometutorpro.data.local.entities

/**
 * Sync status for entities in premium flavor.
 * Tracks the synchronization state between Room and Firestore.
 */
enum class SyncStatus {
    /**
     * Entity is in sync with Firestore.
     */
    SYNCED,
    
    /**
     * Entity has local changes that need to be uploaded to Firestore.
     */
    PENDING_UPLOAD,
    
    /**
     * Entity is marked for deletion and needs to be removed from Firestore.
     */
    PENDING_DELETE,
    
    /**
     * Conflict detected between local and remote versions.
     */
    CONFLICT,
    
    /**
     * Sync error occurred for this entity.
     */
    ERROR
}
