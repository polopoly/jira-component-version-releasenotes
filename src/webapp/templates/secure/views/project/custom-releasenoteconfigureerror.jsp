<%@ taglib uri="webwork" prefix="ww" %>
<%@ taglib uri="webwork" prefix="ui" %>
<%@ taglib uri="sitemesh-page" prefix="page" %>
<html>
<head>
	<title><ww:text name="'releasenotes.configure'" /></title>
</head>
<body>
    <page:applyDecorator name="jiraform">
    <page:param name="title"><ww:text name="'common.concepts.releasenotes'"/></page:param>
    <page:param name="description">
        <ww:if test="versions/size <= 0 && styleNames/size <=0">
        <ww:text name="'releasenotes.generate.note'"/>
        </ww:if>
        <ww:elseIf test="versions/size <= 0">
        <ww:text name="'releasenotes.generate.versions'"/>
        </ww:elseIf>
        <ww:elseIf test="styleNames/size <= 0">
        <ww:text name="'releasenotes.generate.styles'"/>
        </ww:elseIf>
    </page:param>
    <page:param name="submitId">cancel_submit</page:param>
    <page:param name="submitName"><ww:text name="'common.forms.cancel'"/></page:param>
    <page:param name="action">Dashboard.jspa</page:param>
</page:applyDecorator>
</body>
</html>
