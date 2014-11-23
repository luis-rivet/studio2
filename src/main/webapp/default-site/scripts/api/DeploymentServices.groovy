package scripts.api

import scripts.api.ServiceFactory

import groovy.util.logging.Log

@Log
class DeploymentServices {

    /**
     * create the context object
     * @param applicationContext - studio application's contect (spring container etc)
     * @param request - web request if in web request context
     */
    static createContext(applicationContext, request) {
        return ServiceFactory.createContext(applicationContext, request)
    }

    /** 
     * Return deployment history for a give nsite
     * @param site - the project ID
     * @param daysFromToday - number of days back to get
     * @param numberOfItems - number of items to get 
     * @param sort - field to sort on
     * @param ascending - true or false
     * @param filterType - pages/components/all 
     * @paran context - container for passing request, token and other values that may be needed by the implementation
     */
    static getDeploymentHistory(site, daysFromToday, numberOfItems, sort, ascending, filterType, context) {
        def deploymentServicesImpl = ServiceFactory.getDeploymentServices(context)
        return deploymentServicesImpl.getDeploymentHistory(site, daysFromToday, numberOfItems, sort, ascending, filterType)
    }

    /** 
     * get the scheduled items for a site
     * @param site - the project ID
     * @param filter - filters to apply to listing
     */ 
    static getScheduledItems(site, filter) {

    }

}