package com.atex.jira.plugin.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import com.atex.jira.plugin.util.CustomReleaseNoteManager;
import com.atlassian.jira.bc.EntityNotFoundException;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.VersionProxy;
import com.atlassian.jira.project.util.ReleaseNoteManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.web.action.browser.ReleaseNote;
import com.atlassian.sal.api.message.I18nResolver;

public class ReleaseNoteByComponentAction extends ReleaseNote {
    private static final long serialVersionUID = 1L;

    public enum ComponentVersion {
        Released, UnReleased;
    }

    private String component;
    private String version;

    private final CustomReleaseNoteManager customReleaseNoteManager;
    private final VersionManager versionManager;
    private final ProjectManager projectManager;
    private final ProjectComponentManager projectComponentManager;
    private final I18nResolver i18nResolver;

    public ReleaseNoteByComponentAction(ProjectManager projectManager, ReleaseNoteManager releaseNoteManager,
            ConstantsManager constantsManager, VersionManager versionManager,
            CustomReleaseNoteManager customReleaseNoteManager, ProjectComponentManager projectComponentManager,
            I18nResolver i18nResolver) {
        super(projectManager, releaseNoteManager, constantsManager, versionManager);
        this.customReleaseNoteManager = customReleaseNoteManager;
        this.versionManager = versionManager;
        this.projectManager = projectManager;
        this.projectComponentManager = projectComponentManager;
        this.i18nResolver = i18nResolver;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Collection<ProjectComponent> getProjectComponents() {
        return projectComponentManager.findAllForProject(getProjectId());
    }

    @Override
    public String getVersion() {
        if (version == null)
            version = "";
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Collection<VersionProxy> getVersions() throws GenericEntityException {
        GenericValue project = projectManager.getProject(getProjectId());

        if (project == null) {
            return Collections.emptyList();
        }

        List<VersionProxy> unreleased = new ArrayList<VersionProxy>();
        Iterator<Version> unreleasedIter = versionManager.getVersionsUnreleased(project, false).iterator();
        if (unreleasedIter.hasNext()) {
            unreleased.add(new VersionProxy(-2, getText("common.filters.unreleasedversions")));
            while (unreleasedIter.hasNext()) {
                unreleased.add(new VersionProxy((Version) unreleasedIter.next()));
            }
        }

        // reverse the order of the releasedIter versions.
        List<VersionProxy> released = new ArrayList<VersionProxy>();
        List<Version> releasedIter = new ArrayList<Version>(versionManager.getVersionsReleased(project, false));
        if (!releasedIter.isEmpty()) {
            released.add(new VersionProxy(-3, getText("common.filters.releasedversions")));
            Collections.reverse(releasedIter);
            for (Iterator<Version> iterator = releasedIter.iterator(); iterator.hasNext();) {
                released.add(new VersionProxy((Version) iterator.next()));
            }
        }

        List<VersionProxy> versions = new ArrayList<VersionProxy>();
        versions.addAll(unreleased);
        versions.addAll(released);

        return versions;
    }

    @Override
    protected void doValidation() {
        if (getProjectGV() == null) {
            addError("component", getText("releasenotes.project.error"));
        } else if (StringUtils.isBlank(component)) {
            addError("component", i18nResolver.getText("releasenotes.component.error"));
        }
    }

    @Override
    public String getReleaseNote() {
        ComponentVersion componentVersion = null;
        if ("-2".equals(version))
            componentVersion = ComponentVersion.UnReleased;
        else if ("-3".equals(version) || StringUtils.isBlank(version))
            componentVersion = ComponentVersion.Released;

        return customReleaseNoteManager.getReleaseNote(this, getStyleName(), getSelectedVersion(), getRemoteUser(),
                getProjectGV(), component, getProjectComponent(), String.valueOf(getProjectId()), componentVersion);
    }

    private ProjectComponent getProjectComponent() {
        try {
            ProjectComponent projectComponent = projectComponentManager.find(Long.parseLong(component));
            return projectComponent;
        } catch (NumberFormatException e) {
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
