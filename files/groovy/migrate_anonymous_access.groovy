import groovy.json.JsonOutput
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration
import org.sonatype.nexus.security.anonymous.AnonymousManager

def fileName = "chi/security.yml"
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

def anonymousManager = container.lookup(AnonymousManager.class.getName())

def content = [:]

content.put('nexus_anonymous_access', anonymousManager.isEnabled())
scriptResults['action_details'].put(fileName, content)

return JsonOutput.toJson(scriptResults)
