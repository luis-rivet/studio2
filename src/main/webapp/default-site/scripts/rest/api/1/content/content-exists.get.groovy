import scripts.api.ContentServices;

def result = [:]
def site = params.site
def path = params.path

def context = ContentServices.createContext(applicationContext, request)

result.content = ContentServices.doesContentItemExist(site, path, context)

return result 
