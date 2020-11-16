package com.davehock;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AquaLinkServletContextListener implements ServletContextListener {

    private AquaLinkManager aquaLinkManagerThread = null;

    public void contextInitialized(ServletContextEvent sce) {
       if ((aquaLinkManagerThread == null) || (!aquaLinkManagerThread.isAlive())) {
           aquaLinkManagerThread = new AquaLinkManager();
           aquaLinkManagerThread.init();
           sce.getServletContext().setAttribute(AquaLinkManager.getName(), aquaLinkManagerThread);
        }
    }

    public void contextDestroyed(ServletContextEvent sce){
        try {
           aquaLinkManagerThread.close();
           aquaLinkManagerThread.interrupt();

        } catch (Exception ex) {
        	System.out.println(ex.getMessage());
        }
    }
}