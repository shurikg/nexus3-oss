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
if (parsed_args.with_http_proxy != false || getBooleanValue(rtHttpProxy?.isEnabled()) != false) {
    if (parsed_args.with_http_proxy != getBooleanValue(rtHttpProxy?.isEnabled())) {
        gitChangeMessage.add("http proxy = ${parsed_args.with_http_proxy}")
        runtimeChangeMessage.add("http proxy = ${getBooleanValue(rtHttpProxy?.isEnabled())}")
    }
    if (parsed_args.http_proxy_host != rtHttpProxy?.getHost()) {
        gitChangeMessage.add("http host = ${parsed_args.http_proxy_host}")
        runtimeChangeMessage.add("http host = ${rtHttpProxy?.getHost()}")
    }
    if (parsed_args.http_proxy_port != rtHttpProxy?.getPort()) {
        gitChangeMessage.add("http port = ${parsed_args.http_proxy_port}")
        runtimeChangeMessage.add("http port = ${rtHttpProxy?.getPort()}")
    }
    if (parsed_args.nexus_http_proxy_username != rtHttpProxy?.getAuthentication()?.getUsername()) {
        gitChangeMessage.add("http proxy username = ${parsed_args.nexus_http_proxy_username}")
        runtimeChangeMessage.add("http proxy username = ${rtHttpProxy?.getAuthentication()?.getUsername()}")
    }
    if (parsed_args.nexus_http_proxy_password != rtHttpProxy?.getAuthentication()?.getPassword()) {
        gitChangeMessage.add("http proxy password = ${parsed_args.nexus_http_proxy_password}")
        runtimeChangeMessage.add("http proxy password = ${rtHttpProxy?.getAuthentication()?.getPassword()}")
    }
    needCheckNoProxy = true
}

ProxyServerConfiguration rtHttpsProxy = proxyConfiguration?.getHttps()
if (parsed_args.with_https_proxy != false || getBooleanValue(rtHttpsProxy?.isEnabled()) != false) {
    if (parsed_args.with_https_proxy != getBooleanValue(rtHttpsProxy?.isEnabled())) {
        gitChangeMessage.add("https proxy = ${parsed_args.with_https_proxy}")
        runtimeChangeMessage.add("https proxy = ${getBooleanValue(rtHttpsProxy?.isEnabled())}")
    }
    if (parsed_args.https_proxy_host != rtHttpsProxy?.getHost()) {
        gitChangeMessage.add("https host = ${parsed_args.https_proxy_host}")
        runtimeChangeMessage.add("https host = ${rtHttpsProxy?.getHost()}")
    }
    if (parsed_args.https_proxy_port != rtHttpsProxy?.getPort()) {
        gitChangeMessage.add("https port = ${parsed_args.https_proxy_port}")
        runtimeChangeMessage.add("https port = ${rtHttpsProxy?.getPort()}")
    }
    if (parsed_args.nexus_https_proxy_username != rtHttpsProxy?.getAuthentication()?.getUsername()) {
        gitChangeMessage.add("https proxy username = ${parsed_args.nexus_https_proxy_username}")
        runtimeChangeMessage.add("https proxy username = ${rtHttpsProxy?.getAuthentication()?.getUsername()}")
    }
    if (parsed_args.nexus_https_proxy_password != rtHttpsProxy?.getAuthentication()?.getPassword()) {
        gitChangeMessage.add("https proxy password = ${parsed_args.nexus_https_proxy_password}")
        runtimeChangeMessage.add("https proxy password = ${rtHttpsProxy?.getAuthentication()?.getPassword()}")
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
