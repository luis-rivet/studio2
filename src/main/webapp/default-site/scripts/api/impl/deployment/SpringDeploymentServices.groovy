package scripts.api.impl.deployment

import org.craftercms.cstudio.api.service.deployment.DeploymentService;

class SpringDeploymentServices {

    def context = null

    /**
     * constructor
     *
     * @param context - service context
     */
    def SpringDeploymentServices(context) {
        this.context = context
    }

    /** 
     * Return deployment history for a give nsite
     * @param site - the project ID
     * @param daysFromToday - number of days back to get
     * @param numberOfItems - number of items to get 
     * @param sort - field to sort on
     * @param ascending - true or false
     * @param filterType - pages/components/all 
     * @oaran context - container for passing request and other values that may be needed by the implementation
     */
    def getDeploymentHistory(site, daysFromToday, numberOfItems, sort, ascending, filterType) {
        
        def springBackedService = this.context.applicationContext.get("cstudioDmDeploymentService")
        return springBackedService.getDeploymentHistory(site, daysFromToday, numberOfItems, sort, ascending, filterType)
    }
}