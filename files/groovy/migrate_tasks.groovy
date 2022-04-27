import groovy.json.JsonOutput
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Monthly
import org.sonatype.nexus.scheduling.schedule.Schedule
import org.sonatype.nexus.scheduling.schedule.Weekly
import java.text.SimpleDateFormat
import java.util.*

def fileName = 'tasks.yml'
def migrationTasks = ['nexus_scheduled_tasks':[]]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]
TaskScheduler taskScheduler = container.lookup(TaskScheduler.class.getName())
List<TaskInfo> existingTask = taskScheduler.listsTasks()

existingTask.each { rtTaks ->
    def curentTaskProperty = []
    Map<String,String> currentTask = [:]

    TaskConfiguration currentTaskConfiguration = rtTaks.getConfiguration()

    String tasksTypeId = currentTaskConfiguration.getTypeId()

    def knownTask = false

    def taskProperty = [:]
    def boolProperty = [:]

    switch (tasksTypeId) {
        case 'repository.docker.upload-purge':
            taskProperty.put('age', currentTaskConfiguration.getString('age'))
            knownTask = true
            break
        case 'repository.maven.remove-snapshots':
            taskProperty.put('minimumRetained', currentTaskConfiguration.getString('minimumRetained'))
            taskProperty.put('snapshotRetentionDays', currentTaskConfiguration.getString('snapshotRetentionDays'))
            taskProperty.put('gracePeriodInDays', currentTaskConfiguration.getString('gracePeriodInDays'))
            boolProperty.put('removeIfReleased', currentTaskConfiguration.getString('removeIfReleased'))
            taskProperty.put('repositoryName', currentTaskConfiguration.getString('repositoryName'))
            knownTask = true
            break
        case ['blobstore.compact', 'blobstore.rebuildComponentDB']:
            taskProperty.put('blobstoreName', currentTaskConfiguration.getString('blobstoreName'))
            knownTask = true
            break
        case ['repository.maven.purge-unused-snapshots', 'repository.purge-unused']:
            taskProperty.put('lastUsed', currentTaskConfiguration.getString('lastUsed'))
            taskProperty.put('repositoryName', currentTaskConfiguration.getString('repositoryName'))
            knownTask = true
            break
        case 'script':
            taskProperty.put('source', currentTaskConfiguration.getString('source'))
            taskProperty.put('language', currentTaskConfiguration.getString('language'))
            knownTask = true
            break
        case 'repository.yum.rebuild.metadata':
            boolProperty.put('yumMetadataCaching', currentTaskConfiguration.getString('yumMetadataCaching'))
            taskProperty.put('repositoryName', currentTaskConfiguration.getString('repositoryName'))
            knownTask = true
            break
        case 'repository.maven.rebuild-metadata':
            taskProperty.put('groupId', currentTaskConfiguration.getString('groupId'))
            taskProperty.put('artifactId', currentTaskConfiguration.getString('artifactId'))
            taskProperty.put('baseVersion', currentTaskConfiguration.getString('baseVersion'))
            taskProperty.put('rebuildChecksums', currentTaskConfiguration.getString('rebuildChecksums'))
            taskProperty.put('repositoryName', currentTaskConfiguration.getString('repositoryName'))
            knownTask = true
            break
        case 'db.backup':
            taskProperty.put('location', currentTaskConfiguration.getString('location'))
            knownTask = true
            break
        case 'rebuild.asset.uploadMetadata':
            boolProperty.put('dryRun', currentTaskConfiguration.getString('dryRun'))
            boolProperty.put('restoreBlobMetadata', currentTaskConfiguration.getString('restoreBlobMetadata'))
            boolProperty.put('unDeleteReferencedBlobs', currentTaskConfiguration.getString('unDeleteReferencedBlobs'))
            boolProperty.put('integrityCheck', currentTaskConfiguration.getString('integrityCheck'))
            knownTask = true
            break
        case ['create.browse.nodes', 'repository.maven.publish-dotindex', 'repository.docker.gc', 'repository.maven.unpublish-dotindex']:
            taskProperty.put('repositoryName', currentTaskConfiguration.getString('repositoryName'))
            knownTask = true
            break
        case ['security.purge-api-keys']:
            knownTask = true
            break
    }

    if (knownTask) {
        currentTask.put('name', currentTaskConfiguration.getName())
        currentTask.put('typeId', tasksTypeId)
        currentTask.put('task_alert_email', currentTaskConfiguration.getAlertEmail())
        currentTask.put('notificationCondition', currentTaskConfiguration.getNotificationCondition())
        currentTask.put('enabled', currentTaskConfiguration.isEnabled())

        Schedule currentTaskScheduleType = rtTaks.getSchedule()

        scheduleType = currentTaskScheduleType.getType()
        switch (scheduleType) {
            case ['daily', 'hourly', 'once']:
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                currentTask.put('schedule_type', scheduleType)
                currentTask.put('start_date_time', dateFormat.format(currentTaskScheduleType.getStartAt()))
                break
            case 'weekly':
                currentTask.put('schedule_type', scheduleType)
                currentTask.put('weekly_days', currentTaskScheduleType.getDaysToRun())
                break
            case 'monthly':
                currentTask.put('schedule_type', scheduleType)
                monthly_days = currentTaskScheduleType.getDaysToRun()
                def mDay = []
                monthly_days.each { dayVal ->
                    mDay.add(dayVal['day'])
                }
                currentTask.put('monthly_days', mDay)
                break
            case 'cron':
                schedule_cronExpression = currentTaskScheduleType.getCronExpression()
                currentTask.put('schedule_type', scheduleType)
                currentTask.put('cron', schedule_cronExpression.toString())
                break
            case ['manual', 'now']:
                currentTask.put('schedule_type', scheduleType)
                break
        }
        if (! boolProperty.isEmpty()) {
            currentTask.put('booleanTaskProperties', boolProperty)
        }
        if (! taskProperty.isEmpty()) {
            currentTask.put('taskProperties', taskProperty)
        }
        migrationTasks['nexus_scheduled_tasks'].add(currentTask)
    }

}

scriptResults['action_details'].put(fileName, migrationTasks)
return JsonOutput.toJson(scriptResults)
