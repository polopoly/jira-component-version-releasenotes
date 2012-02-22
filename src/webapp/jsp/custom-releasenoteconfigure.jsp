<%@ taglib uri="webwork" prefix="ww" %>
<%@ taglib uri="webwork" prefix="ui" %>
<%@ taglib uri="sitemesh-page" prefix="page" %>
<html>
<head>
	<title><ww:text name="'releasenotes.configure'" /></title>
</head>
<body>
	<page:applyDecorator name="jiraform">
		<page:param name="title"><ww:text name="'releasenotes.configure'"/></page:param>
		<page:param name="description"><ww:text name="'releasenotes.configure.desc'"/></page:param>
        <page:param name="cancelURI">BrowseProject.jspa</page:param>
		<page:param name="action">ReleaseNoteByComponent.jspa</page:param>
		<page:param name="submitId">create_submit</page:param>
		<page:param name="submitName"><ww:text name="'releasenotes.create'"/></page:param>
        <page:param name="method">get</page:param>
        
        <ui:select label="text('Please Select Component')" name="'component'" list="projectComponents" listKey="'id'" listValue="'name'" >
            <ui:param name="'headerrow'">Please Select Component</ui:param>
            <ui:param name="'headervalue'" value="''" />
            <ui:param name="'mandatory'" value="true"/>
        </ui:select>

        <ui:select label="text('releasenotes.versions')" name="'version'" list="versions" listKey="'key'" listValue="'value'" >
            <ui:param name="'headerrow'"><ww:text name="'releasenotes.versions'"/></ui:param>
            <ui:param name="'headervalue'" value="''" />
            <ui:param name="'mandatory'" value="false"/>
        </ui:select>

		<ui:select label="text('releasenotes.styles')" name="'styleName'" list="styleNames" listKey="'.'" listValue="'.'">
            <ui:param name="'headerrow'"><ww:text name="'releasenotes.styles'"/></ui:param>
            <ui:param name="'headervalue'" value="''" />
            <ui:param name="'mandatory'" value="true"/>
		</ui:select>

        <input type="hidden" name="projectId" value="<ww:property value="projectId"/>" >
	</page:applyDecorator>
</body>
</html>
