package com.atex.jira.plugin.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;
import org.ofbiz.core.entity.GenericValue;

import webwork.action.Action;
import webwork.action.ActionContext;

import com.atex.jira.plugin.action.ReleaseNoteByComponentAction.ComponentVersion;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.util.velocity.DefaultVelocityRequestContextFactory;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.order.SortOrder;
import com.atlassian.velocity.VelocityManager;
import com.opensymphony.user.User;
import com.opensymphony.util.TextUtils;

public class CustomReleaseNoteManager {
    private static final Logger log = Logger.getLogger(CustomReleaseNoteManager.class);

    public static final String RELEASE_NOTE_NAME = "jira.releasenotes.templatenames";
    public static final String RELEASE_NOTE_DEFAULT = "jira.releasenotes.default";
    public static final String RELEASE_NOTE_TEMPLATE = "jira.releasenotes.templates";
    public static final String TEMPLATES_DIR = "templates/jira/project/customreleasenotes/";

    private Map styles;

    private final ApplicationProperties applicationProperties;
    private final VelocityManager velocityManager;
    private final ConstantsManager constantsManager;
    private final SearchProvider searchProvider;
    private final CustomFieldManager customFieldManager;
    private final VersionManager versionManager;

    public CustomReleaseNoteManager(final ApplicationProperties applicationProperties,
            final VelocityManager velocityManager, final ConstantsManager constantsManager,
            final SearchProvider searchProvider, final CustomFieldManager customFieldManager,
            final VersionManager versionManager) {
        this.applicationProperties = applicationProperties;
        this.velocityManager = velocityManager;
        this.constantsManager = constantsManager;
        this.searchProvider = searchProvider;
        this.customFieldManager = customFieldManager;
        this.versionManager = versionManager;
    }

    public Map getStyles() {
        if (styles == null) {
            loadReleaseNoteTemplates();
        }
        return styles;
    }

    private List splitString(String strings) {
        if (strings == null) {
            return Collections.EMPTY_LIST;
        }

        List stringList = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(strings, ",");
        while (tokenizer.hasMoreTokens()) {
            stringList.add(tokenizer.nextToken().trim());
        }
        return stringList;
    }

    private void loadReleaseNoteTemplates() {
        List allReleaseNoteNames = splitString(applicationProperties.getDefaultBackedString(RELEASE_NOTE_NAME));
        List allReleaseNoteTemplates = splitString(applicationProperties.getDefaultBackedString(RELEASE_NOTE_TEMPLATE));
        if (allReleaseNoteTemplates.size() != allReleaseNoteNames.size()) {
            throw new RuntimeException(
                    "Error loading release notes; differing numbers of names and templates specified in properties file.");
        }

        styles = new HashMap(allReleaseNoteTemplates.size());

        for (int i = 0; i < allReleaseNoteNames.size(); i++) {
            styles.put(allReleaseNoteNames.get(i), allReleaseNoteTemplates.get(i));
        }
    }

    /**
     * Return a releasenote for this version, using the specified releaseNoteStyleName. The issues returned will be the
     * issues that the user has permission to see.
     * 
     * @param projectId
     * @param componentVersion
     * 
     * @throws IllegalArgumentException
     *             if there is no matching template for this releaseNoteStyleName
     */
    public String getReleaseNote(Action action, String releaseNoteStyleName, Version version, User user,
            GenericValue project, String component, ProjectComponent projectComponent, String projectId,
            ComponentVersion componentVersion) throws IllegalArgumentException {
        try {
            String templateName = (String) getStyles().get(releaseNoteStyleName);

            // use Default
            if (templateName == null) {
                final String defaultType = applicationProperties.getDefaultBackedString(RELEASE_NOTE_DEFAULT);
                if (StringUtils.isNotBlank(defaultType)) {
                    templateName = (String) getStyles().get(defaultType);
                }
            }

            // use first
            if (templateName == null) {
                templateName = getFirstStyle();
            }

            if (templateName == null) {
                log.error("No styles available for release notes");
                throw new IllegalArgumentException("No styles available for release notes");
            }

            Map templateVariables = null;
            if (version != null)
                templateVariables = getTemplateVariables(action, version, component, user, project, projectComponent,
                        projectId);
            else
                templateVariables = getTemplateVariablesForAllVersion(action, component, user, project,
                        projectComponent, projectId, componentVersion);

            return velocityManager.getBody(TEMPLATES_DIR, templateName, templateVariables);
        } catch (VelocityException e) {
            log.error("Exception occurred while attempting to get velocity body for release note template.", e);
            return null;
        }
    }

    private String getFirstStyle() {
        final Collection values = getStyles().values();
        if (values != null) {
            final Iterator iterator = values.iterator();
            if (iterator.hasNext()) {
                return (String) iterator.next();
            }
        }
        return null;
    }

    private Map getTemplateVariables(Action action, Version version, String component, User user, GenericValue project,
            ProjectComponent projectComponent, String projectId) {
        List<IssuesByType> issueTypes = new ArrayList<IssuesByType>();
        for (IssueType issueType : constantsManager.getAllIssueTypeObjects()) {
            issueTypes
                    .add(new IssuesByType(issueType, user, version != null ? version.getLong("id") : null, component));
        }
        TextUtils textUtils = new TextUtils();
        Map templateVarMap = EasyMap.build("action", action, "req", ActionContext.getRequest(), "issueTypes",
                issueTypes, "appProps", applicationProperties, "project", project.getString("name"), "component",
                projectComponent.getDescription(), "componentId", component, "projectId", projectId, "textUtils",
                textUtils, "requestContext",
                new DefaultVelocityRequestContextFactory(applicationProperties).getJiraVelocityRequestContext());
        templateVarMap.put("constantsManager", constantsManager);
        templateVarMap.put("customFieldManager", customFieldManager);
        if (version != null) {
            templateVarMap.put("version", version.getName());
            templateVarMap.put("versionObj", version);
        }
        return templateVarMap;
    }

    private Map getTemplateVariablesForAllVersion(Action action, String component, User user, GenericValue project,
            ProjectComponent projectComponent, String projectId, ComponentVersion componentVersion) {
        Map<String, List<IssuesByType>> versionMaps = new LinkedHashMap<String, List<IssuesByType>>();
        Collection<IssueType> issueTypes = constantsManager.getAllIssueTypeObjects();

        List<Version> versions = null;

        if (ComponentVersion.Released == componentVersion) {
            versions = new ArrayList<Version>(versionManager.getVersionsReleased(project, false));
        } else {
            versions = new ArrayList<Version>(versionManager.getVersionsUnreleased(project, false));
        }

        if (!versions.isEmpty()) {
            Collections.reverse(versions);
            for (Version version : versions) {
                int numberOfIssues = 0;
                List<IssuesByType> issueByTypes = new ArrayList<IssuesByType>();
                for (IssueType issueType : issueTypes) {
                    IssuesByType issues = new IssuesByType(issueType, user, version != null ? version.getLong("id")
                            : null, component);
                    numberOfIssues += issues.getIssues().size();
                    issueByTypes.add(issues);
                }

                if (numberOfIssues > 0)
                    versionMaps.put(version.getName(), issueByTypes);
            }
        }
        TextUtils textUtils = new TextUtils();
        Map templateVarMap = EasyMap.build("action", action, "req", ActionContext.getRequest(), "issueTypesByVersion",
                versionMaps, "appProps", applicationProperties, "project", project.getString("name"), "component",
                projectComponent.getDescription(), "componentId", component, "projectId", projectId, "textUtils",
                textUtils, "requestContext",
                new DefaultVelocityRequestContextFactory(applicationProperties).getJiraVelocityRequestContext());
        templateVarMap.put("constantsManager", constantsManager);
        templateVarMap.put("customFieldManager", customFieldManager);
        return templateVarMap;
    }

    public class IssuesByType {
        private final IssueType issueType;
        private final User user;
        private final Long fixForVersion;
        private final String component;
        private Collection issues;

        private IssuesByType(IssueType issueType, User user, @NotNull Long fixForVersion, @NotNull String component) {
            this.issueType = issueType;
            this.user = user;
            this.fixForVersion = fixForVersion;
            this.component = component;
        }

        public String getName() {
            return issueType.getNameTranslation();
        }

        public Collection getIssues() {
            if (issues == null) {
                final JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();

                if (fixForVersion != null) {
                    queryBuilder.where().fixVersion(fixForVersion).and().issueType(issueType.getId()).and()
                            .component(component);
                } else {
                    queryBuilder.where().issueType(issueType.getId()).and().component(component);
                }
                queryBuilder.orderBy().issueKey(SortOrder.ASC);
                try {
                    issues = searchProvider.search(queryBuilder.buildQuery(), user, PagerFilter.getUnlimitedFilter())
                            .getIssues();
                } catch (SearchException e) {
                    log.error("Error searching for issues", e);
                }
            }
            return issues;
        }
    }
}
