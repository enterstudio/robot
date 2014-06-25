package com.robot;

/**
 * An enum for possible upload states
 */
public enum UploadState {

    /**
     * Represents the fact that the current model is being synchronized with the remote representation. If an attempt to synchronize the local representation fails,
     * it should flip back to {@link #needsUploading}
     */
    uploading,
    /**
     * represents the fact that the current local representation is up to date with the remote representation
     */
    uploaded,
    /**
     * represent the fact that the current local representation has changed and is therefore not up to date with the remote representation
     */
    needsUploading
}
