import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.cleanup.storage.CleanupPolicy
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage

import java.util.concurrent.TimeUnit

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

CleanupPolicyStorage cleanupPolicyStorage = container.lookup(CleanupPolicyStorage.class.getName())
List<CleanupPolicy> cleanupPolicies = cleanupPolicyStorage.getAll()

parsed_args.details.each { cleanupPolicyDef ->
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []

    existingCleanupPolicy = cleanupPolicyStorage.get(cleanupPolicyDef.name)
    if (existingCleanupPolicy == null) {
        currentResult.put('change_in_git', "definition of new ${cleanupPolicyDef.name} clenaup policy")
        currentResult.put('change_in_runtime', 'N/A')
        currentResult.put('change_type', 'add')
        currentResult.put('description', "the ${cleanupPolicyDef.name} clenaup policy will be added")
        currentResult.put('resource', 'cleanup policy')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
    else {
        if (cleanupPolicyDef.format != existingCleanupPolicy.getFormat()) {
            gitChangeMessage.add("format = ${cleanupPolicyDef.format}")
            runtimeChangeMessage.add("format = ${existingCleanupPolicy.getFormat()}")
        }
        if (cleanupPolicyDef.mode != existingCleanupPolicy.getMode()) {
            gitChangeMessage.add("mode = ${cleanupPolicyDef.mode}")
            runtimeChangeMessage.add("mode = ${existingCleanupPolicy.getMode()}")
        }
        if (cleanupPolicyDef.notes != existingCleanupPolicy.getNotes()) {
            gitChangeMessage.add("notes = ${cleanupPolicyDef.notes}")
            runtimeChangeMessage.add("notes = ${existingCleanupPolicy.getNotes()}")
        }

        Map<String, String> existingCleanupCriteria = existingCleanupPolicy.getCriteria()
        if (cleanupPolicyDef.criteria.lastBlobUpdated != asIntDays(existingCleanupCriteria.get('lastBlobUpdated'))) {
            gitChangeMessage.add("component age = ${cleanupPolicyDef.criteria.lastBlobUpdated}")
            runtimeChangeMessage.add("component age = ${asIntDays(existingCleanupCriteria.get('lastBlobUpdated'))}")
        }
        if (cleanupPolicyDef.criteria.lastDownloaded != asIntDays(existingCleanupCriteria.get('lastDownloaded'))) {
            gitChangeMessage.add("component usage = ${cleanupPolicyDef.criteria.lastDownloaded}")
            runtimeChangeMessage.add("component usage = ${asIntDays(existingCleanupCriteria.get('lastDownloaded'))}")
        }
        if (existingCleanupCriteria.get('isPrerelease') != null && cleanupPolicyDef.criteria.preRelease != existingCleanupCriteria.get('isPrerelease') ||
                existingCleanupCriteria.get('isPrerelease') == null && cleanupPolicyDef.criteria.preRelease != "") {
            gitChangeMessage.add("release type = ${cleanupPolicyDef.criteria.preRelease}")
            runtimeChangeMessage.add("release type = " + existingCleanupCriteria.get('isPrerelease') != null ?
                    (existingCleanupCriteria.get('isPrerelease')=="true" ? "PRERELEASES" : "RELEASES")
                    : "N/A")
        }
        if (existingCleanupCriteria.get('regex') != null && cleanupPolicyDef.criteria.regexKey != existingCleanupCriteria.get('regex') ||
                existingCleanupCriteria.get('regex') == null && cleanupPolicyDef.criteria.regexKey != "") {
            gitChangeMessage.add("asset name matcher = ${cleanupPolicyDef.criteria.regexKey}")
            runtimeChangeMessage.add("asset name matcher = " + existingCleanupCriteria.get('regex') != null ? existingCleanupCriteria.get('regex') : "N/A")
        }

        if (gitChangeMessage) {
            currentResult.put('change_in_git', gitChangeMessage.join('\n'))
            currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
            currentResult.put('change_type', 'change')
            currentResult.put('description', "the ${cleanupPolicyDef.name} cleanup policy will be updated")
            currentResult.put('resource', 'cleanup policy')
            currentResult.put('downtime', false)
            scriptResults['action_details'].add(currentResult)
        }
    }
}

cleanupPolicies.each { rtCleanupPolicy ->
    def needToDelete = true
    Map<String, String> currentResult = [:]
    def rtCleanupPolicyName = rtCleanupPolicy.getName()

    parsed_args.details.any { cleanupDef ->
        if (rtCleanupPolicyName == cleanupDef.name) {
            needToDelete = false
            return true
        }
    }
    if (needToDelete){
        currentResult.put('change_in_git', "N/A")
        currentResult.put('change_in_runtime', "${rtCleanupPolicyName}")
        currentResult.put('change_type', 'delete')
        currentResult.put('description', "the ${rtCleanupPolicyName} cleanup policy will be deleted")
        currentResult.put('resource', 'cleanup policy')
        currentResult.put('downtime', false)

        scriptResults['action_details'].add(currentResult)

        if (! parsed_args.dry_run) {
            cleanupPolicyStorage.remove(rtCleanupPolicy)
        }
    }
}

return JsonOutput.toJson(scriptResults)

def Integer asDays(secondsInt) {
    return secondsInt / TimeUnit.DAYS.toSeconds(1)
}

def String asIntDays(secondsStr) {
    return asDays(Integer.valueOf(secondsStr))
}