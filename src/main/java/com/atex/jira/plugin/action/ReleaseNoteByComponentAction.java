package com.atex.jira.plugin.action;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import com.atex.jira.plugin.util.CustomReleaseNoteManager;
import com.atlassian.jira.bc.EntityNotFoundException;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.util.ReleaseNoteManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.web.action.browser.ReleaseNote;

public class ReleaseNoteByComponentAction extends ReleaseNote {
    private static final long serialVersionUID = 1L;

    private CustomReleaseNoteManager customReleaseNoteManager;
    private String component;
    private VersionManager versionManager;
    private ProjectManager projectManager;
    private ProjectComponentManager projectComponentManager;

    public ReleaseNoteByComponentAction(ProjectManager projectManager, ReleaseNoteManager releaseNoteManager,
            ConstantsManager constantsManager, VersionManager versionManager,
            CustomReleaseNoteManager customReleaseNoteManager, ProjectComponentManager projectComponentManager) {
        super(projectManager, releaseNoteManager, constantsManager, versionManager);
        this.customReleaseNoteManager = customReleaseNoteManager;
        this.versionManager = versionManager;
        this.projectManager = projectManager;
        this.projectComponentManager = projectComponentManager;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }
    
    public Collection<ProjectComponent> getProjectComponents(){
        return projectComponentManager.findAllForProject(getProjectId());
    }
    
    public String doConfigureByComponent() throws GenericEntityException {
        System.out.println("doConfigureByComponent");
        if (getProjectGV() == null || getVersions().isEmpty() || getStyleNames().isEmpty() || StringUtils.isBlank(component))
        {
            return ERROR;
        }
        else
        {
            return SUCCESS;
        }
    }

    @Override
    public String getReleaseNote() {
        if (StringUtils.isNotBlank(component)) {
            return customReleaseNoteManager.getReleaseNote(this, getStyleName(), getSelectedVersion(), getRemoteUser(),
                    getProjectGV(), component, getProjectComponent());
        } else {
            return super.getReleaseNote();
        }
    }
    
    private ProjectComponent getProjectComponent(){
        try
        {
            ProjectComponent projectComponent = projectComponentManager.find(Long.parseLong(component));
            return projectComponent;
        }
        catch (NumberFormatException e)
        {
            return null;
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Version getSelectedVersion() {
        try {
            return versionManager.getVersion(Long.parseLong(getVersion()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private GenericValue getProjectGV() {
        return projectManager.getProject(getProjectId());
    }
}
