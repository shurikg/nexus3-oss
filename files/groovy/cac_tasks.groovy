import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Monthly
import org.sonatype.nexus.scheduling.schedule.Schedule
import org.sonatype.nexus.scheduling.schedule.Weekly
import java.text.SimpleDateFormat

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

TaskScheduler taskScheduler = container.lookup(TaskScheduler.class.getName())

def compareSchedualer(gitTask, rtTask, gitChangeMessage, runtimeChangeMessage) {

    type = gitTask.get('schedule_type', 'cron')

    Schedule currentTaskSchedule = rtTask.getSchedule()
    scheduleType = currentTaskSchedule.getType()

    if (type != scheduleType) {
        gitChangeMessage.add("schedule type = ${type}")
        runtimeChangeMessage.add("schedule type = ${scheduleType}")

        return
    }

    switch(type) {
        case ['daily', 'hourly', 'once']:
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            date_time_string = gitTask.get('start_date_time', null)
            Date start_date
            if (date_time_string)
                start_date = dateFormat.parse(date_time_string)
            else
                start_date = new Date()

            if (dateFormat.format(start_date) != dateFormat.format(currentTaskSchedule.getStartAt())) {
                gitChangeMessage.add("start at = ${dateFormat.format(start_date)}")
                runtimeChangeMessage.add("start at = ${dateFormat.format(currentTaskSchedule.getStartAt())}")
            }
            break
        case 'weekly':
            weekly_days = gitTask.get('weekly_days', null)
            def wDay = []
            currentTaskSchedule.getDaysToRun().each { dayVal ->
                wDay.add(dayVal.toString())
            }
            if (weekly_days != wDay) {
                gitChangeMessage.add("weekly days = ${weekly_days}")
                runtimeChangeMessage.add("weekly days = ${wDay}")
            }
            break
        case 'monthly':
            monthly_days = gitTask.get('monthly_days', null)
            def mDay = []
            currentTaskSchedule.getDaysToRun().each { dayVal ->
                mDay.add(dayVal['day'])
            }
            if (monthly_days != mDay) {
                gitChangeMessage.add("monthly days = ${monthly_days}")
                runtimeChangeMessage.add("monthly days = ${mDay}")
            }
            break
        case 'cron':
            cron = gitTask.get('cron', null)

            if (cron != currentTaskSchedule.getCronExpression().toString()) {
                gitChangeMessage.add("cron = ${cron}")
                runtimeChangeMessage.add("cron = ${currentTaskSchedule.getCronExpression().toString()}")
            }
            break
    }
}

TaskInfo existingTask

parsed_args.details.each { taskDef ->
    existingTask = taskScheduler.listsTasks().find { TaskInfo taskInfo ->
        taskInfo.name == taskDef.name && taskInfo.getTypeId() == taskDef.typeId
    }

    Map<String, String> currentResult = [:]

    if (existingTask) {
        def gitChangeMessage = []
        def runtimeChangeMessage = []
        def taskIdChanged = false

        TaskConfiguration currentTaskConfiguration = existingTask.getConfiguration()

        if (currentTaskConfiguration.getAlertEmail() != taskDef.get('task_alert_email', '')) {
            gitChangeMessage.add("alert email = ${taskDef.get('task_alert_email', '')}")
            runtimeChangeMessage.add("alert email = ${currentTaskConfiguration.getAlertEmail()}")
        }

        if (currentTaskConfiguration.getNotificationCondition().toString() != taskDef.get('notificationCondition', 'FAILURE')) {
            gitChangeMessage.add("notification condition = ${taskDef.get('notificationCondition', 'FAILURE')}")
            runtimeChangeMessage.add("notification condition = ${currentTaskConfiguration.getNotificationCondition().toString()}")
        }

        if (currentTaskConfiguration.isEnabled() != Boolean.valueOf(taskDef.get('enabled', 'true') as String)) {
            gitChangeMessage.add("enabled = ${taskDef.get('enabled', 'true')}")
            runtimeChangeMessage.add("enabled = ${currentTaskConfiguration.isEnabled().toString()}")
        }

        if ( ! taskIdChanged ) {
            switch (taskDef.typeId) {
                case 'repository.docker.upload-purge':
                    if (currentTaskConfiguration.getString('age') != taskDef.taskProperties.age) {
                        gitChangeMessage.add("age = ${taskDef.taskProperties.age}")
                        runtimeChangeMessage.add("age = ${currentTaskConfiguration.getString('age')}")
                    }
                    break
                case 'repository.maven.remove-snapshots':
                    if (currentTaskConfiguration.getString('minimumRetained') != taskDef.taskProperties.minimumRetained) {
                        gitChangeMessage.add("minimum retained = ${taskDef.taskProperties.minimumRetained}")
                        runtimeChangeMessage.add("minimum retained = ${currentTaskConfiguration.getString('minimumRetained')}")
                    }
                    if (currentTaskConfiguration.getString('snapshotRetentionDays') != taskDef.taskProperties.snapshotRetentionDays) {
                        gitChangeMessage.add("snapshot retention days = ${taskDef.taskProperties.snapshotRetentionDays}")
                        runtimeChangeMessage.add("snapshot retention days = ${currentTaskConfiguration.getString('snapshotRetentionDays')}")
                    }
                    if (currentTaskConfiguration.getString('gracePeriodInDays') != taskDef.taskProperties.gracePeriodInDays) {
                        gitChangeMessage.add("grace period in days = ${taskDef.taskProperties.gracePeriodInDays}")
                        runtimeChangeMessage.add("grace period in days = ${currentTaskConfiguration.getString('gracePeriodInDays')}")
                    }
                    if (currentTaskConfiguration.getString('removeIfReleased') != taskDef.booleanTaskProperties.removeIfReleased) {
                        gitChangeMessage.add("remove if released = ${taskDef.booleanTaskProperties.removeIfReleased}")
                        runtimeChangeMessage.add("remove if released = ${currentTaskConfiguration.getString('removeIfReleased')}")
                    }
                    if (currentTaskConfiguration.getString('repositoryName') != taskDef.taskProperties.repositoryName) {
                        gitChangeMessage.add("repository name = ${taskDef.taskProperties.repositoryName}")
                        runtimeChangeMessage.add("repository name = ${currentTaskConfiguration.getString('repositoryName')}")
                    }
                    break
                case ['blobstore.compact', 'blobstore.rebuildComponentDB']:
                    if (currentTaskConfiguration.getString('blobstoreName') != taskDef.taskProperties.blobstoreName) {
                        gitChangeMessage.add("blobstore name = ${taskDef.taskProperties.blobstoreName}")
                        runtimeChangeMessage.add("blobstore name = ${currentTaskConfiguration.getString('blobstoreName')}")
                    }
                    break
                case ['repository.maven.purge-unused-snapshots', 'repository.purge-unused']:
                    if (currentTaskConfiguration.getString('lastUsed') != taskDef.taskProperties.lastUsed) {
                        gitChangeMessage.add("last used = ${taskDef.taskProperties.lastUsed}")
                        runtimeChangeMessage.add("last used = ${currentTaskConfiguration.getString('lastUsed')}")
                    }
                    if (currentTaskConfiguration.getString('repositoryName') != taskDef.taskProperties.repositoryName) {
                        gitChangeMessage.add("repository name = ${taskDef.taskProperties.repositoryName}")
                        runtimeChangeMessage.add("repository name = ${currentTaskConfiguration.getString('repositoryName')}")
                    }
                    break
                case 'script':
                    if (currentTaskConfiguration.getString('source') != taskDef.taskProperties.source) {
                        gitChangeMessage.add("source = ${taskDef.taskProperties.source}")
                        runtimeChangeMessage.add("source = ${currentTaskConfiguration.getString('source')}")
                    }
                    if (currentTaskConfiguration.getString('language') != taskDef.taskProperties.language) {
                        gitChangeMessage.add("language = ${taskDef.taskProperties.language}")
                        runtimeChangeMessage.add("language = ${currentTaskConfiguration.getString('language')}")
                    }
                    break
                case 'repository.yum.rebuild.metadata':
                    if (currentTaskConfiguration.getString('yumMetadataCaching') != taskDef.booleanTaskProperties.yumMetadataCaching) {
                        gitChangeMessage.add("yum metadata caching = ${taskDef.booleanTaskProperties.yumMetadataCaching}")
                        runtimeChangeMessage.add("yum metadata caching = ${currentTaskConfiguration.getString('yumMetadataCaching')}")
                    }
                    if (currentTaskConfiguration.getString('repositoryName') != taskDef.taskProperties.repositoryName) {
                        gitChangeMessage.add("repository name = ${taskDef.taskProperties.repositoryName}")
                        runtimeChangeMessage.add("repository name = ${currentTaskConfiguration.getString('repositoryName')}")
                    }
                    break
                case 'repository.maven.rebuild-metadata':
                    if (currentTaskConfiguration.getString('groupId') != taskDef.taskProperties.groupId) {
                        gitChangeMessage.add("groupId = ${taskDef.taskProperties.groupId}")
                        runtimeChangeMessage.add("groupId = ${currentTaskConfiguration.getString('groupId')}")
                    }
                    if (currentTaskConfiguration.getString('artifactId') != taskDef.taskProperties.artifactId) {
                        gitChangeMessage.add("artifactId = ${taskDef.taskProperties.artifactId}")
                        runtimeChangeMessage.add("artifactId = ${currentTaskConfiguration.getString('artifactId')}")
                    }
                    if (currentTaskConfiguration.getString('baseVersion') != taskDef.taskProperties.baseVersion) {
                        gitChangeMessage.add("baseVersion = ${taskDef.taskProperties.baseVersion}")
                        runtimeChangeMessage.add("baseVersion = ${currentTaskConfiguration.getString('baseVersion')}")
                    }
                    if (currentTaskConfiguration.getString('rebuildChecksums') != taskDef.taskProperties.rebuildChecksums) {
                        gitChangeMessage.add("rebuild checksums = ${taskDef.taskProperties.rebuildChecksums}")
                        runtimeChangeMessage.add("rebuild checksums = ${currentTaskConfiguration.getString('rebuildChecksums')}")
                    }
                    if (currentTaskConfiguration.getString('repositoryName') != taskDef.taskProperties.repositoryName) {
                        gitChangeMessage.add("repository name = ${taskDef.taskProperties.repositoryName}")
                        runtimeChangeMessage.add("repository name = ${currentTaskConfiguration.getString('repositoryName')}")
                    }
                    break
                case 'db.backup':
                    if (currentTaskConfiguration.getString('location') != taskDef.taskProperties.location) {
                        gitChangeMessage.add("location = ${taskDef.taskProperties.location}")
                        runtimeChangeMessage.add("location = ${currentTaskConfiguration.getString('location')}")
                    }
                    break
                case 'rebuild.asset.uploadMetadata':
                    if (currentTaskConfiguration.getString('dryRun') != taskDef.booleanTaskProperties.dryRun) {
                        gitChangeMessage.add("dryRun = ${taskDef.booleanTaskProperties.dryRun}")
                        runtimeChangeMessage.add("dryRun = ${currentTaskConfiguration.getString('dryRun')}")
                    }
                    if (currentTaskConfiguration.getString('restoreBlobMetadata') != taskDef.booleanTaskProperties.restoreBlobMetadata) {
                        gitChangeMessage.add("restore blob metadata = ${taskDef.booleanTaskProperties.restoreBlobMetadata}")
                        runtimeChangeMessage.add("restore blob metadata = ${currentTaskConfiguration.getString('restoreBlobMetadata')}")
                    }
                    if (currentTaskConfiguration.getString('unDeleteReferencedBlobs') != taskDef.booleanTaskProperties.unDeleteReferencedBlobs) {
                        gitChangeMessage.add("undelete referenced blobs = ${taskDef.booleanTaskProperties.unDeleteReferencedBlobs}")
                        runtimeChangeMessage.add("undelete referenced blobs = ${currentTaskConfiguration.getString('unDeleteReferencedBlobs')}")
                    }
                    if (currentTaskConfiguration.getString('integrityCheck') != taskDef.booleanTaskProperties.integrityCheck) {
                        gitChangeMessage.add("integrity check = ${taskDef.booleanTaskProperties.integrityCheck}")
                        runtimeChangeMessage.add("integrity check = ${currentTaskConfiguration.getString('integrityCheck')}")
                    }
                    break
                case 'create.browse.nodes':
                    if (currentTaskConfiguration.getString('repositoryName') != taskDef.taskProperties.repositoryName) {
                        gitChangeMessage.add("repository name = ${taskDef.taskProperties.repositoryName}")
                        runtimeChangeMessage.add("repository name = ${currentTaskConfiguration.getString('repositoryName')}")
                    }
                    break
                case ['repository.maven.publish-dotindex', 'repository.maven.unpublish-dotindex', 'repository.docker.gc']:
                    if (currentTaskConfiguration.getString('repositoryName') != taskDef.taskProperties.repositoryName) {
                        gitChangeMessage.add("repository name = ${taskDef.taskProperties.repositoryName}")
                        runtimeChangeMessage.add("repository name = ${currentTaskConfiguration.getString('repositoryName')}")
                    }
                    break
            }
        }

        compareSchedualer(taskDef, existingTask, gitChangeMessage, runtimeChangeMessage)

        if (gitChangeMessage) {
            currentResult.put('change_in_git', gitChangeMessage.join('\n'))
            currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
            currentResult.put('change_type', 'change')
            currentResult.put('description', "the configuration of ${taskDef.name} from ${existingTask.getTypeId()} type will be update")
            currentResult.put('resource', 'tasks')
            currentResult.put('downtime', false)

            scriptResults['action_details'].add(currentResult)
        }
    }
    else {
        currentResult.put('change_in_git', "definition of new ${taskDef.name} task")
        currentResult.put('change_in_runtime', 'N/A')
        currentResult.put('change_type', 'add')
        currentResult.put('description', "the ${taskDef.name} task with ${taskDef.typeId} type will be added")
        currentResult.put('resource', 'tasks')
        currentResult.put('downtime', false)

        scriptResults['action_details'].add(currentResult)
    }
}

// Runtime comparison vs git -> Delete task if needed

taskScheduler.listsTasks().each { rtTask ->
    Map<String, String> currentResult = [:]

    def needToDelete = true

    TaskConfiguration currentTaskConfiguration = rtTask.getConfiguration()

    // if (rtTask.getTypeId() == 'tasklog.cleanup' ) {
    //     needToDelete = false
    // }
    else {
        parsed_args.details.any { taskDef ->
            if (rtTask.getTypeId() == taskDef.typeId && rtTask.getName() == taskDef.name) {
                needToDelete = false
                return true
            }
        }
    }

    if (needToDelete) {
        currentResult.put('change_in_git', 'N/A')
        currentResult.put('change_in_runtime', "${currentTaskConfiguration.getName()} task exist")
        currentResult.put('change_type', 'delete')
        currentResult.put('description', "the ${currentTaskConfiguration.getName()} task from the ${currentTaskConfiguration.getTypeId()} type will be deleted")
        currentResult.put('resource', 'tasks')
        currentResult.put('downtime', false)

        scriptResults['action_details'].add(currentResult)

        if (! parsed_args.dry_run) {
            rtTask.remove()
        }
    }
}

return JsonOutput.toJson(scriptResults)
