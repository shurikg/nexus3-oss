import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration
import org.sonatype.nexus.security.anonymous.AnonymousManager

parsed_args = new JsonSlurper().parseText(args)

def anonymousManager = container.lookup(AnonymousManager.class.getName())

// def anonymousConfig = anonymousManager.getConfiguration()

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false, runner: 'anonymous']
scriptResults.put('action_details', actionDetails)

if (Boolean.valueOf(parsed_args.anonymous_access) != anonymousManager.isEnabled()) {
    Map<String, String> currentResult = [:]
    currentResult.put('change_in_git', "anonymous access = ${parsed_args.anonymous_access}")
    currentResult.put('change_in_runtime', "anonymous access = ${anonymousManager.isEnabled()}")
    currentResult.put('change_type', 'change')
    currentResult.put('description', "the anonymous access will be change")
    currentResult.put('resource', 'anonymous access')
    currentResult.put('downtime', false)
    scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)

