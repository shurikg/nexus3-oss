import groovy.json.JsonOutput
import groovy.json.JsonSlurper

List<Map<String, String>> actionDetails = []
@Field Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)
Map<String, String> currentResult = []

parsed_args = new JsonSlurper().parseText(args)

try {
    security.securitySystem.changePassword('admin', parsed_args.new_password)
    currentResult.put('status', 'changed')
    scriptResults['changed'] = true

} catch (Exception e) {
    currentResult.put('status', 'error')
    currentResult.put('error_msg', e.toString())
    scriptResults['error'] = true
}

scriptResults['action_details'].add(currentResult)

return JsonOutput.toJson(scriptResults)