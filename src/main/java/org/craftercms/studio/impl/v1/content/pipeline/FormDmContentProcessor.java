/*
 * Crafter Studio Web-content authoring solution
 * Copyright (C) 2007-2016 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.studio.impl.v1.content.pipeline;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.craftercms.studio.api.v1.constant.DmConstants;
import org.craftercms.studio.api.v1.content.pipeline.DmContentProcessor;
import org.craftercms.studio.api.v1.content.pipeline.PipelineContent;
import org.craftercms.studio.api.v1.dal.ObjectMetadata;
import org.craftercms.studio.api.v1.exception.ContentNotFoundException;
import org.craftercms.studio.api.v1.exception.ContentProcessException;
import org.craftercms.studio.api.v1.exception.ServiceException;
import org.craftercms.studio.api.v1.log.Logger;
import org.craftercms.studio.api.v1.log.LoggerFactory;
import org.craftercms.studio.api.v1.repository.ContentRepository;
import org.craftercms.studio.api.v1.service.activity.ActivityService;
import org.craftercms.studio.api.v1.service.configuration.ServicesConfig;
import org.craftercms.studio.api.v1.service.content.ContentService;
import org.craftercms.studio.api.v1.service.content.ObjectMetadataManager;
import org.craftercms.studio.api.v1.service.dependency.DmDependencyService;
import org.craftercms.studio.api.v1.service.workflow.WorkflowService;
import org.craftercms.studio.api.v1.to.ContentItemTO;
import org.craftercms.studio.api.v1.to.DmPathTO;
import org.craftercms.studio.api.v1.to.ResultTO;
import org.craftercms.studio.impl.v1.util.ContentFormatUtils;
import org.craftercms.studio.impl.v1.util.ContentUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormDmContentProcessor extends PathMatchProcessor implements DmContentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FormDmContentProcessor.class);

    public static final String NAME = "WriteContentToDmProcessor";


    /**
     * default constructor
     */
    public FormDmContentProcessor() {
        super(NAME); 
    }
 
    /**
     * constructor that sets the process name
     *
     * @param name
     */
    public FormDmContentProcessor(String name) {
        super(name);
    }

    public void process(PipelineContent content, ResultTO result) throws ContentProcessException {
        try {
            writeContent(content);
        } catch (ServiceException e) {
            logger.error("Failed to write " + content.getId(),e);
            throw new ContentProcessException("Failed to write " + content.getId(), e);
        } finally {
            content.closeContentStream();
        }
    }

    /**
     * write content to the given path
     *
     * @param site
     *            site to write the content to
     * @param user
     * 			content owner
     * @param path
     *            path to write the content to
     * @param fileName
     *            content name
     * @param contentType
     * 				content type
     * @param input
     *            content input stream
     * @param createFolders
     * 				create missing folders?
     * @param overwrite
     * 				overwrite the existing content?
     * @param isPreview
     * @param unlock
     * 			unlock the content upon update?
     * @throws ServiceException
     */
    protected ActivityService.ActivityType writeContent(String site, String user, String path, String fileName, String contentType, InputStream input,
                                                        boolean createFolders, boolean overwrite, boolean isPreview, boolean unlock) throws ServiceException {
        String parentContentPath = contentService.expandRelativeSitePath(site, path);
        try {
            // look up the path content first
            boolean parentExists = contentService.contentExists(site, path);
            ContentItemTO parentItem = contentService.getContentItem(site, path, 0);
            if (parentExists && createFolders) {
                parentItem = createMissingFoldersInPath(site, path, isPreview);
            }
            if (parentItem != null) {
                // if the parent content name is the same as the file name
                // update the content
                // look up the path content first
                if (parentItem.getName().equals(fileName)) {
                    updateFile(site, parentItem, parentContentPath, input, user, isPreview, unlock);
                    return ActivityService.ActivityType.UPDATED;
                } else {
                    // otherwise, create new one
                    //contentPath = contentPath.replaceFirst(WcmConstants.INDEX_FILE, "");
                    String fileFullPath =  parentContentPath + "/" + fileName;
                    String relativeFilePath = contentService.getRelativeSitePath(site, fileFullPath);
                    boolean contentExists = contentService.contentExists(site, fileFullPath);
                    ContentItemTO fileItem = contentService.getContentItem(site, relativeFilePath, 0);
                    if (fileItem != null && overwrite) {
                        updateFile(site, fileItem, fileFullPath, input, user, isPreview, unlock);
                        return ActivityService.ActivityType.UPDATED;
                    } else {
                        createNewFile(site, parentItem, fileName, contentType, input, user,unlock);
                        return ActivityService.ActivityType.CREATED;
                    }
                }
            } else {
                throw new ContentNotFoundException(path + " does not exist in site: " + site);
            }
        } catch (ContentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw new ContentNotFoundException("Unexpected exception ", e);
        } finally {
            ContentUtils.release(input);
        }

    }

    protected void writeContent(PipelineContent content) throws ServiceException {
        String user = content.getProperty(DmConstants.KEY_USER);
        String site = content.getProperty(DmConstants.KEY_SITE);
        String path = content.getProperty(DmConstants.KEY_PATH);
        String fileName = content.getProperty(DmConstants.KEY_FILE_NAME);
        String contentType = content.getProperty(DmConstants.KEY_CONTENT_TYPE);
        InputStream input = content.getContentStream();
        boolean isPreview = ContentFormatUtils.getBooleanValue(content.getProperty(DmConstants.KEY_IS_PREVIEW));
        boolean createFolders = ContentFormatUtils.getBooleanValue(content.getProperty(DmConstants.KEY_CREATE_FOLDERS));
        String unlockValue = content.getProperty(DmConstants.KEY_UNLOCK);
        boolean unlock = (!StringUtils.isEmpty(unlockValue) && unlockValue.equalsIgnoreCase("false")) ? false : true;

        String fullPath = contentService.expandRelativeSitePath(site, path);
        String parentContentPath = fullPath;
        if (parentContentPath.endsWith("/" + fileName)) {
            parentContentPath = parentContentPath.replace("/" + fileName, "");
        } else {
            fullPath = fullPath + "/" + fileName;
        }
        String parentRelativePath = contentService.getRelativeSitePath(site, parentContentPath);
        //contentPath = (isPreview) ? DmUtils.getPreviewPath(contentPath) : contentPath;
        try {
            // look up the path content first
            ContentItemTO parentItem = contentService.getContentItem(site, parentRelativePath, 0);
            boolean parentContentExists = contentService.contentExists(site, parentRelativePath);
            if (!parentContentExists && createFolders) {
                parentItem = createMissingFoldersInPath(site, path, isPreview);
            }
            if (parentItem != null) {
                // if the parent content name is the same as the file name
                // update the content
                // look up the path content first
                if (parentItem.getName().equals(fileName)) {
                    ContentItemTO item = contentService.getContentItem(site, path, 0);
                    InputStream existingContent = contentService.getContent(site, path);

                    String existingMd5 = ContentUtils.getMd5ForFile(existingContent);
                    String newMd5 = ContentUtils.getMd5ForFile(input);
                    if (!existingMd5.equals(newMd5)) {
                        updateFile(site, item, fullPath, input, user, isPreview, unlock);
                        content.addProperty(DmConstants.KEY_ACTIVITY_TYPE, ActivityService.ActivityType.UPDATED.toString());
                    } else {
                        updateLastEditedProperties(site, item.getUri(), user);
                        if (!isPreview) {
                            DmPathTO pathTO = new DmPathTO(fullPath);
                            if (cancelWorkflow(site, path)) {
                                workflowService.removeFromWorkflow(site, pathTO.getRelativePath(), true);
                                //dmDependencyService.updateDependencies(site,pathTO.getRelativePath(), DmConstants.DM_STATUS_IN_PROGRESS);
                            } else {
                                if(updateWorkFlow(site,pathTO.getRelativePath())) {
                                    workflowService.updateWorkflowSandboxes(site,pathTO.getRelativePath());
                                }
                            }
                        }
                    }
                    if (unlock) {
                        // TODO: We need ability to lock/unlock content in repo
                        contentService.unLockContent(site, path);
                        logger.debug("Unlocked the content " + parentContentPath);
                    }
                    return;
                } else {
                    // otherwise, create new one
                    if (fullPath.endsWith(DmConstants.XML_PATTERN) && !fullPath.endsWith(DmConstants.INDEX_FILE)){
                        parentContentPath = fullPath.substring(0, fullPath.lastIndexOf("/"));
                        String partentRelativePath = contentService.getRelativeSitePath(site, parentContentPath);
                        parentItem = contentService.getContentItem(site, partentRelativePath, 0);
                    }

                    String fileRelativePath = contentService.getRelativeSitePath(site, fullPath);
                    boolean fileExists = contentService.contentExists(site, fileRelativePath);
                    if (fileExists) {
                        ContentItemTO contentItem = contentService.getContentItem(site, fileRelativePath, 0);
                        InputStream existingContent = contentService.getContent(site, fileRelativePath);
                        String existingMd5 = ContentUtils.getMd5ForFile(existingContent);
                        String newMd5 = ContentUtils.getMd5ForFile(input);
                        if (!existingMd5.equals(newMd5)) {
                            updateFile(site, contentItem, fullPath, input, user, isPreview, unlock);
                            content.addProperty(DmConstants.KEY_ACTIVITY_TYPE, ActivityService.ActivityType.UPDATED.toString());
                        } else {
                            updateLastEditedProperties(site, contentItem.getUri(), user);
                            if (!isPreview) {
                                DmPathTO pathTO = new DmPathTO(fullPath);
                                if (cancelWorkflow(site, pathTO.getRelativePath())) {
                                    workflowService.removeFromWorkflow(site, pathTO.getRelativePath(), true);
                                    //dmDependencyService.updateDependencies(site,pathTO.getRelativePath(), DmConstants.DM_STATUS_IN_PROGRESS);
                                } else {
                                    if(updateWorkFlow(site,pathTO.getRelativePath())) {
                                        workflowService.updateWorkflowSandboxes(site,pathTO.getRelativePath());
                                    }
                                }
                            }
                        }
                        if (unlock) {
                            // TODO: We need ability to lock/unlock content in repo
                            contentService.unLockContent(site, path);
                            logger.debug("Unlocked the content " + fullPath);
                        }
                        return;
                    } else {
                        ContentItemTO newFileItem = createNewFile(site, parentItem, fileName, contentType, input, user, unlock);
                        content.addProperty(DmConstants.KEY_ACTIVITY_TYPE, ActivityService.ActivityType.CREATED.toString());
                        return;
                    }
                }
            } else {
                throw new ContentNotFoundException(path + " does not exist in site: " + site);
            }
        } catch (ContentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw new ContentNotFoundException("Unexpected exception ", e);
        } finally {
            ContentUtils.release(input);
        }

    }

    private void updateLastEditedProperties(String site, String relativePath, String user) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ObjectMetadata.PROP_MODIFIER, user);
        properties.put(ObjectMetadata.PROP_MODIFIED, new Date());
        if (!objectMetadataManager.metadataExist(site, relativePath)) {
            objectMetadataManager.insertNewObjectMetadata(site, relativePath);
        }
        objectMetadataManager.setObjectMetadata(site, relativePath, properties);
	}


    /**
     * create new file to the given path. If the path is a file name, it will
     * create a new folder with the same name as the file name (without the
     * prefix) and move the existing file to the folder created. Then it creates
     * new file to the folder
     *
     * @param site
     *            Site name
     * @param parentNode
     *            Parent node
     * @param fileName
     *            new file name
     * @param contentType
     * 			content type
     * @param input
     *            file content
     * @param user
     *            current user
     * @throws ContentNotFoundException
     */
    protected ContentItemTO createNewFile(String site, ContentItemTO parentItem, String fileName, String contentType, InputStream input,
    		String user, boolean unlock)
            throws ContentNotFoundException {
        ContentItemTO fileItem = null;

        if (parentItem != null) {
            // convert file to folder if target path is a file
            String folderPath = fileToFolder(site, parentItem.getUri());
            try {
                contentService.writeContent(site, parentItem.getUri() + "/" + fileName, input);
                objectMetadataManager.insertNewObjectMetadata(site, parentItem.getUri() + "/" + fileName);
                Map<String, Object> properties = new HashMap<>();
                properties.put(ObjectMetadata.PROP_NAME, fileName);
                properties.put(ObjectMetadata.PROP_MODIFIED, new Date());
                properties.put(ObjectMetadata.PROP_CREATOR, user);
                properties.put(ObjectMetadata.PROP_MODIFIER, user);
                properties.put(ObjectMetadata.PROP_OWNER, user);
                if (unlock) {
                    properties.put(ObjectMetadata.PROP_LOCK_OWNER, StringUtils.EMPTY);
                } else {
                    properties.put(ObjectMetadata.PROP_LOCK_OWNER, user);
                }
                objectMetadataManager.setObjectMetadata(site, parentItem.getUri() + "/" + fileName, properties);
            } catch (Exception e) {
                logger.error("Error writing new file: " + fileName, e);
            } finally {
                IOUtils.closeQuietly(input);
            }

            // unlock the content upon save
            if (unlock) {
                contentRepository.unLockItem(site, parentItem.getUri() + "/" + fileName);
                //persistenceManagerService.unlock(fileNode);
            } else {
            }

            fileItem = contentService.getContentItem(site, parentItem.getUri() + "/" + fileName, 0);
            return fileItem;
        } else {
            String parentPath = contentService.expandRelativeSitePath(site, parentItem.getUri());
            throw new ContentNotFoundException(parentPath + " does not exist in site: " + site);
        }
    }


    /**
     * update the file at the given content node
     *
     * @param contentNode
     * @param fullPath
     * @param input
     * @param user
     * @param isPreview
     * @param unlock
     * 			unlock the content upon update?
     * @throws ServiceException
     */
    protected void updateFile(String site, ContentItemTO contentItem, String fullPath, InputStream input, String user, boolean isPreview, boolean unlock)
            throws ServiceException {

        // TODO: check lock status of content
        /*
    	LockStatus lockStatus = persistenceManagerService.getLockStatus(contentNode);
        String nodeStatus = (String)persistenceManagerService.getProperty(contentNode, CStudioContentModel.PROP_STATUS);
        /**
         * added to handle issue with submitting locked content (Dejan 2012/04/12)
         *//*
        if (LockStatus.NO_LOCK.equals(lockStatus) || LockStatus.LOCK_OWNER.equals(lockStatus) ||
                (nodeStatus.equalsIgnoreCase(DmConstants.DM_STATUS_SUBMITTED))
        		)
        /***** end ******//*
        {*/
            try {
                contentService.writeContent(fullPath, input);
            } finally {
                ContentUtils.release(input);
            }

        Map<String, Object> properties = new HashMap<>();
        properties.put(ObjectMetadata.PROP_MODIFIER, user);
        properties.put(ObjectMetadata.PROP_MODIFIED, new Date());
        if (unlock) {
            properties.put(ObjectMetadata.PROP_LOCK_OWNER, StringUtils.EMPTY);
        } else {
            properties.put(ObjectMetadata.PROP_LOCK_OWNER, user);
        }
        String relativePath = contentService.getRelativeSitePath(site, fullPath);
        if (!objectMetadataManager.metadataExist(site, relativePath)) {
            objectMetadataManager.insertNewObjectMetadata(site, relativePath);
        }
        objectMetadataManager.setObjectMetadata(site, relativePath, properties);

        //TODO: create next minor version
        /*
            dmVersionService.createNextMinorVersion(site, fullPath);
            */
            // unlock the content upon save if the flag is true
            if (unlock) {
                contentRepository.unLockItem(site, relativePath);
                logger.debug("Unlocked the content " + fullPath);
            } else {
                contentRepository.lockItem(site, relativePath);
            }
            // if there is anything pending and this is not a preview update, cancel workflow
            if (!isPreview) {
                DmPathTO path = new DmPathTO(fullPath);
                if (cancelWorkflow(site, path.getRelativePath())) {
                    workflowService.removeFromWorkflow(site, path.getRelativePath(), true);
                    //dmDependencyService.updateDependencies(site,path.getRelativePath(), DmConstants.DM_STATUS_IN_PROGRESS);
                } else {
                    if(updateWorkFlow(site,path.getRelativePath())) {
                        workflowService.updateWorkflowSandboxes(site,path.getRelativePath());
                    }
                }
            }/*
        } else {
            String owner = (String)persistenceManagerService.getProperty(contentNode, ContentModel.PROP_LOCK_OWNER);
            throw new ServiceException("The content is locked by another user: " + owner);
        }*/
    }

    /**
     * cancel the pending workflow upon editing the content at the given path?
     *
     * @param site
     * @param path
     * @return
     */
    protected boolean cancelWorkflow(String site, String path) {
        // don't cancel if the content is a level descriptor
        if (path.endsWith(servicesConfig.getLevelDescriptorName(site))) {
            return false;
        } else {
            List<String> pagePatterns = servicesConfig.getPagePatterns(site);
            // cancel if the content is a page
            if (ContentUtils.matchesPatterns(path, pagePatterns)) {
                return true;
            }

            List<String> componentPatterns = servicesConfig.getComponentPatterns(site);
            if (ContentUtils.matchesPatterns(path, componentPatterns)) {
                return true;
            }

            // Checking for document also
            List<String> documentPatterns = servicesConfig.getDocumentPatterns(site);
            // cancel if the content is a document
            if (ContentUtils.matchesPatterns(path, documentPatterns)) {
                return true;
            }
        }
        return false;
    }

    protected boolean updateWorkFlow(String site,String path) {
        List<String> assetPatterns = servicesConfig.getAssetPatterns(site);
        return  ContentUtils.matchesPatterns(path, assetPatterns);
    }

    @Override
    public ContentItemTO createMissingFoldersInPath(String site, String path, boolean isPreview) {
        // create parent folders if missing
        String [] levels = path.split("/");
        String parentPath = "";
        ContentItemTO lastItem = null;
        for (String level : levels) {
            if (!StringUtils.isEmpty(level) && !level.endsWith(DmConstants.XML_PATTERN)) {
                String currentPath = parentPath + "/" + level;
                if (!contentService.contentExists(site, currentPath)) {
                    contentService.createFolder(site, parentPath, level);
                }
                parentPath = currentPath;
            }
        }
        lastItem = contentService.getContentItem(site, parentPath, 0);
        return lastItem;
    }


    @Override
    public String fileToFolder(String site, String path) {
        // Check if it is already a folder

        if (contentService.contentExists(site, path)) {
            ContentItemTO itemTO = contentService.getContentItem(site, path, 0);
            if (itemTO.isFolder() || itemTO.isDeleted()) {
                return contentService.expandRelativeSitePath(site, path);
            }
            int index = path.lastIndexOf("/");
            String folderPath = path.substring(0, index);
            String parentFileName = itemTO.getName();
            int dotIndex = parentFileName.indexOf(".");
            String folderName = (dotIndex > 0) ? parentFileName.substring(0, parentFileName.indexOf(".")) : parentFileName;
            contentService.createFolder(site, folderPath, folderName);
            folderPath = folderPath + "/" + folderName;
            contentService.moveContent(site, path, folderPath + "/" + DmConstants.INDEX_FILE);
            logger.debug("Changed file to folder from " + path + " to " + folderPath);

            return folderPath;
        } else {
            return contentService.expandRelativeSitePath(site, path);
        }
    }

    protected ContentService contentService;
    protected WorkflowService workflowService;
    protected DmDependencyService dmDependencyService;
    protected ServicesConfig servicesConfig;
    protected ObjectMetadataManager objectMetadataManager;
    protected ContentRepository contentRepository;

    public ContentService getContentService() { return contentService; }
    public void setContentService(ContentService contentService) { this.contentService = contentService; }

    public WorkflowService getWorkflowService() { return workflowService; }
    public void setWorkflowService(WorkflowService workflowService) { this.workflowService = workflowService; }

    public DmDependencyService getDmDependencyService() { return dmDependencyService; }
    public void setDmDependencyService(DmDependencyService dmDependencyService) { this.dmDependencyService = dmDependencyService; }

    public ServicesConfig getServicesConfig() { return servicesConfig; }
    public void setServicesConfig(ServicesConfig servicesConfig) { this.servicesConfig = servicesConfig; }

    public ObjectMetadataManager getObjectMetadataManager() { return objectMetadataManager; }
    public void setObjectMetadataManager(ObjectMetadataManager objectMetadataManager) { this.objectMetadataManager = objectMetadataManager; }

    public ContentRepository getContentRepository() { return contentRepository; }
    public void setContentRepository(ContentRepository contentRepository) { this.contentRepository = contentRepository; }
}
