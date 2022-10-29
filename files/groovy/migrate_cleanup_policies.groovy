import groovy.json.JsonOutput
import org.sonatype.nexus.cleanup.storage.CleanupPolicy
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage
import java.util.concurrent.TimeUnit

def fileName = 'cleanup_policies.yml'
def migrationClenanupPolicies = ['nexus_repos_cleanup_policies': []]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]
CleanupPolicyStorage cleanupPolicyStorage = container.lookup(CleanupPolicyStorage.class.getName())
List<CleanupPolicy> cleanupPolicies = cleanupPolicyStorage.getAll()

cleanupPolicies.each { rtCleanupPolicy ->
    Map<String, String> currentPolicy = [:]

    currentPolicy.put('name', rtCleanupPolicy.getName())
    currentPolicy.put('format', rtCleanupPolicy.getFormat())
    currentPolicy.put('mode', rtCleanupPolicy.getMode())
    currentPolicy.put('notes', rtCleanupPolicy.getNotes())
    Map<String, String> rtCleanupCriteria = rtCleanupPolicy.getCriteria()
    def criteria = [:]
    if (rtCleanupCriteria.get('lastBlobUpdated') != null) {
        criteria.put('lastBlobUpdated', asIntDays(rtCleanupCriteria.get('lastBlobUpdated')))
    }
    if (rtCleanupCriteria.get('lastDownloaded') != null) {
        criteria.put('lastDownloaded', asIntDays(rtCleanupCriteria.get('lastDownloaded')))
    }
    if (rtCleanupCriteria.get('isPrerelease') != null) {
        if (rtCleanupCriteria.get('isPrerelease') == "true") {
            criteria.put('preRelease', 'PRERELEASES')
        }
        else{
            criteria.put('preRelease', 'RELEASES')
        }
    }
    if (rtCleanupCriteria.get('regex') != null) {
        criteria.put('regexKey', rtCleanupCriteria.get('regex'))
    }

    if (! criteria.isEmpty()) {
        currentPolicy.put('criteria', criteria)
    }

    migrationClenanupPolicies['nexus_repos_cleanup_policies'].add(currentPolicy)
}
scriptResults['action_details'].put(fileName, migrationClenanupPolicies)

return JsonOutput.toJson(scriptResults)

def Integer asDays(secondsInt) {
    return secondsInt / TimeUnit.DAYS.toSeconds(1)
}

def Integer asIntDays(secondsStr) {
    return asDays(Integer.valueOf(secondsStr))
}
