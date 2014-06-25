package com.robot;

import java.util.Date;

/**
 * Created by fernandinho on 6/25/14.
 */
public interface BaseModel {

    /**
     * Persists the current BaseModel
     */
    public void save();

    /**
     * Synchronizes the current model's local representation with the remote representation. Normally this means uploading to an API.
     */
    public void sync();

    /**
     * @return the current upload state
     * @see com.robot.UploadState
     */
    public UploadState getUploadState();

    /**
     * @return true if the current uploadState is UploadState.uploaded
     */
    public boolean isUploaded();

    /**
     * @return true if the current uploadState is UploadState.uploading
     */
    public boolean isUploading();

    /**
     * @return true if the current uploadState is UploadState.needsUploading
     */
    public boolean needsUploading();

    /**
     * Sets the upload state
     * @param uploadState the upload state
     */
    public void setUploadState(UploadState uploadState);

    /**
     * @return the date when a successful sync was last made
     */
    public Date getLastSyncDate();

    /**
     * @return the date when a successful save was last made
     */
    public Date getLastSaveDate();

    /**
     * @return the date when this BaseModel was created locally, not remotely.
     */
    public Date getCreationDate();

}
