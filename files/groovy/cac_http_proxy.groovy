import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.sonatype.nexus.internal.httpclient.HttpClientManagerImpl
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration
import org.sonatype.nexus.httpclient.config.ProxyConfiguration
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration

def getBooleanValue(value) {
    if (value == null) {
        return false
    }
    return value
}

def getRealValueOrNA(value, flag) {
    if (flag) {
        return value
    }
    return 'N/A'
}

parsed_args = new JsonSlurper().parseText(args)
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', [])

def httpClientManager = container.lookup(HttpClientManagerImpl.class.getName())
def httpConfiguration = httpClientManager.getConfiguration()
ProxyConfiguration proxyConfiguration = httpConfiguration.getProxy()

def needCheckNoProxy = false
def gitChangeMessage = []
def runtimeChangeMessage = []

ProxyServerConfiguration rtHttpProxy = proxyConfiguration?.getHttp()
def isRtHttp = getBooleanValue(rtHttpProxy?.isEnabled())

if (parsed_args.with_http_proxy != false || isRtHttp != false) {
    if (parsed_args.with_http_proxy != getBooleanValue(rtHttpProxy?.isEnabled())) {
        gitChangeMessage.add("http proxy = ${parsed_args.with_http_proxy}")
        runtimeChangeMessage.add("http proxy = ${isRtHttp}")
    }
    if (parsed_args.http_proxy_host != rtHttpProxy?.getHost()) {
        gitChangeMessage.add("http host = ${getRealValueOrNA(parsed_args.http_proxy_host, parsed_args.with_http_proxy)}")
        runtimeChangeMessage.add("http host = ${getRealValueOrNA(rtHttpProxy?.getHost(),isRtHttp)}")
    }
    if (parsed_args.http_proxy_port != rtHttpProxy?.getPort()) {
        gitChangeMessage.add("http port = ${getRealValueOrNA(parsed_args.http_proxy_port, parsed_args.with_http_proxy)}")
        runtimeChangeMessage.add("http port = ${getRealValueOrNA(rtHttpProxy?.getPort(),isRtHttp)}")
    }
    if (parsed_args.nexus_http_proxy_username != rtHttpProxy?.getAuthentication()?.getUsername()) {
        gitChangeMessage.add("http proxy username = ${getRealValueOrNA(parsed_args.nexus_http_proxy_username, parsed_args.with_http_proxy)}")
        runtimeChangeMessage.add("http proxy username = ${getRealValueOrNA(rtHttpProxy?.getAuthentication()?.getUsername(),isRtHttp)}")
    }
    if (parsed_args.nexus_http_proxy_password != rtHttpProxy?.getAuthentication()?.getPassword()) {
        gitChangeMessage.add("http proxy password = ${getRealValueOrNA(parsed_args.nexus_http_proxy_password, parsed_args.with_http_proxy)}")
        runtimeChangeMessage.add("http proxy password = ${getRealValueOrNA(rtHttpProxy?.getAuthentication()?.getPassword(),isRtHttp)}")
    }

    needCheckNoProxy = true
}

ProxyServerConfiguration rtHttpsProxy = proxyConfiguration?.getHttps()
def isRtHttps = getBooleanValue(rtHttpsProxy?.isEnabled())

if (parsed_args.with_https_proxy != false || isRtHttps != false) {
    if (parsed_args.with_https_proxy != getBooleanValue(rtHttpsProxy?.isEnabled())) {
        gitChangeMessage.add("https proxy = ${parsed_args.with_https_proxy}")
        runtimeChangeMessage.add("https proxy = ${isRtHttps}")
    }
    if (parsed_args.https_proxy_host != rtHttpsProxy?.getHost()) {
        gitChangeMessage.add("https host = ${getRealValueOrNA(parsed_args.https_proxy_host, parsed_args.with_https_proxy)}")
        runtimeChangeMessage.add("https host = ${getRealValueOrNA(rtHttpsProxy?.getHost(), isRtHttps)}")
    }
    if (parsed_args.https_proxy_port != rtHttpsProxy?.getPort()) {
        gitChangeMessage.add("https port = ${getRealValueOrNA(parsed_args.https_proxy_port, parsed_args.with_https_proxy)}")
        runtimeChangeMessage.add("https port = ${getRealValueOrNA(rtHttpsProxy?.getPort(), isRtHttps)}")
    }
    if (parsed_args.nexus_https_proxy_username != rtHttpsProxy?.getAuthentication()?.getUsername()) {
        gitChangeMessage.add("https proxy username = ${getRealValueOrNA(parsed_args.nexus_https_proxy_username, parsed_args.with_https_proxy)}")
        runtimeChangeMessage.add("https proxy username = ${getRealValueOrNA(rtHttpsProxy?.getAuthentication()?.getUsername(), isRtHttps)}")
    }
    if (parsed_args.nexus_https_proxy_password != rtHttpsProxy?.getAuthentication()?.getPassword()) {
        gitChangeMessage.add("https proxy password = ${getRealValueOrNA(parsed_args.nexus_https_proxy_password, parsed_args.with_https_proxy)}")
        runtimeChangeMessage.add("https proxy password = ${getRealValueOrNA(rtHttpsProxy?.getAuthentication()?.getPassword(), isRtHttps)}")
    }
    needCheckNoProxy = true
}

if (needCheckNoProxy) {
    if (parsed_args.nexus_proxy_exclude_hosts != proxyConfiguration?.getNonProxyHosts()) {
        gitChangeMessage.add("no proxy = ${parsed_args.nexus_https_proxy_password}")
        runtimeChangeMessage.add("no proxy = ${proxyConfiguration?.getNonProxyHosts()}")
    }
}

if (gitChangeMessage) {
    Map<String, String> currentResult = [:]

    currentResult.put('change_in_git', gitChangeMessage.join('\n'))
    currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
    currentResult.put('change_type', 'change')
    currentResult.put('description', 'the http proxy configuration will be update')
    currentResult.put('resource', 'http proxy')
    currentResult.put('downtime', false)
    scriptResults['action_details'].add(currentResult)
}

if (! parsed_args.dry_run && ! parsed_args.with_http_proxy && ! parsed_args.with_https_proxy) {
    if (rtHttpProxy?.isEnabled()) {
        core.removeHTTPProxy()
    }
    if (rtHttpsProxy?.isEnabled()) {
        core.removeHTTPSProxy()
    }
    if (proxyConfiguration?.getNonProxyHosts()) {
        core.nonProxyHosts()
    }
}
return JsonOutput.toJson(scriptResults)
