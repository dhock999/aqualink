<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="UTF-8" import="com.davehock.*,javax.comm.*" %><?xml version="1.0" encoding="UTF-8"?>
<%
AquaLinkManager al = (AquaLinkManager) this.getServletContext().getAttribute(AquaLinkManager.getName());
    
    if (request.getMethod().equals("POST"))
    {
    	if (request.getParameter("keyUp")!=null)
    		al.setKeyPress("KEY_UP");
    	else if (request.getParameter("keyDown")!=null)
    		al.setKeyPress("KEY_DOWN");
    	else if (request.getParameter("keyBack")!=null)
    		al.setKeyPress("KEY_BACK");
    	else if (request.getParameter("keyOne")!=null)
    		al.setKeyPress("KEY_ONE");
    	else if (request.getParameter("keyTwo")!=null)
    		al.setKeyPress("KEY_TWO");
    	else if (request.getParameter("keySelect")!=null)
    		al.setKeyPress("KEY_SELECT");
    	else if (request.getParameter("keyPower")!=null)
    	{
    		if (al.getUnitStatus()==AquaLinkManager.EQUIPMENTSTATUS.ON)
    		  al.setUnitStatus("OFF");
    		else
    		  al.setUnitStatus("ON");
    	}
	}

%>
<html>
<head>
<meta http-equiv="refresh" content="3">
<style type="text/css">
input[type=submit]
{
    font-size: 160%; 
}
body {
    font-size: 160%; 
}
</style>

</head>
<body>
<% 
if (request.getMethod().equals("POST"))
{
	Thread.sleep(250);
	for (int i=0;i<20;i++)
	{
		if (al.isKeyPressPending())
			Thread.sleep(100);
		else
			break;
	}
} 
 %>

<%=al.getScreenDisplayHtml()%>

<form method="POST">

<table border="0">
<tr>
<td align="center">
<input type="submit" value="BACK" name="keyBack" />
</td>
<td align="center">
<input type="submit" value="UP" name="keyUp" />
</td>
<td align="center">
<input type="submit" value="Power" name="keyPower" />
</td>
</tr>

<tr>
<td align="center">
</td>
<td align="center">
<input type="submit" value="Select" name="keySelect" />
</td>
<td align="center">
</td>
</tr>

<tr>
<td align="center">
<input type="submit" value="1" name="keyOne" />
</td>
<td align="center">
<input type="submit" value="DOWN" name="keyDown" />
</td>
<td align="center">
<input type="submit" value="2" name="keyTwo" />
</td>
</tr>

<tr>
<td align="center">
</td>
<td align="center">
<input type="submit" value="Refresh" name="keyRefresh" /></td>
</td>
<td align="center"></td>
</tr>
</table>

</form>

</body>
</html>
