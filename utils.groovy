def filep = null
import java.nio.file.*
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper
import hudson.console.ModelHyperlinkNote
import groovy.io.FileType
@NonCPS
String getLogFromRunWrapper(RunWrapper runWrapper, int logLines) {
    runWrapper.getRawBuild().getLog(logLines).join('\n')
}

def setPropertyFile(file){
    this.filep=file
}
def isGenericMode(adapterProp){
    if(isDataModeEnabled()){
        return isGenericModeEnabledInDataMode(adapterDataProp)
    }
    ENABLE_GENERIC_MODE= adapterProp.get("ENABLE_GENERIC_MODE")
    if(isNotNullandEmpty(ENABLE_GENERIC_MODE) && ENABLE_GENERIC_MODE == "false"){
        debug("INFO: Making ansible generic mode disable")
        ENABLE_GENERIC_MODE=false
    }
    else{
        debug("INFO: Making ansible generic mode enable")
        ENABLE_GENERIC_MODE=true
    }
    return ENABLE_GENERIC_MODE
}
def isGenericModeEnabledInDataMode(adapterDataProp){
    if(isDataModeEnabled()){
    ENABLE_GENERIC_MODE_DATA = adapterDataProp.get("ENABLE_GENERIC_MODE")
    debug("INFO: adapterDataProp ENABLE_GENERIC_MODE=${ENABLE_GENERIC_MODE_DATA}")
    if(isNotNullandEmpty(ENABLE_GENERIC_MODE_DATA) && ENABLE_GENERIC_MODE_DATA == "true" ){
        debug("INFO: Making ansible generic mode enable (datamode)")
        ENABLE_GENERIC_MODE_DATA = true
    }
    else{
        debug("INFO: Making ansible generic mode disable (datamode)")
        ENABLE_GENERIC_MODE_DATA = false
    }
    return ENABLE_GENERIC_MODE_DATA
    }

}
def getPropertyFile(){
    return this.filep
}
def getPropertyValue(key) {
    value=this.getPropertyFile().get(key)
    debug("Get ${key}=${value}")
    //echo  key+'='+value
    return value
}
def getPropertyKeys() {
    keySet=this.getPropertyFile().keySet()
    return keySet
}
def getPropertyValueNull(key){
    value=this.getPropertyValue(key)
    if(value){
        return value
    }
    else{
        debug('Return empty value for '+key)
        return ''
    }
}
def getProjectLocation(){
    return getPropertyValue("PROJECT_LOCATION")
}
def scriptWindows(command) {
    bat "${command}"
}
def scriptWindowsWithReturnCommand(command) {
    output = bat(returnStdout: true, script: command)
    return output
}

def archiveBuildArtifacts(archiveFilePath){
    exists = fileExists archiveFilePath
    if(exists){
        archiveArtifacts artifacts: archiveFilePath
    }
}
def publishHTMLReport(projectlocation,fileName,ReportName){
    exists = fileExists "${projectlocation}/${fileName}"
    if(exists){
        publishHTML (target: [
            allowMissing: false,
            includes: '**/*',
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: projectlocation,
            reportFiles: fileName,
            reportName: ReportName
            
        ])
    }
}
def copyFile(file,sourceFile,destFile) {
    if (isUnix()) {
        echo "Linux:Copying from ${sourceFile}/${file} to ${destFile}/${file}"
        sh "cp -rf ${sourceFile}${file} ${destFile} | true"
    }
    else {
        sourceFile=windowsPath(sourceFile)
        destFile=windowsPath(destFile)
        echo "Windows:Copying from ${sourceFile}/${file} to ${destFile}/${file}"
        bat "xcopy ${sourceFile}${file} ${destFile}  /Q /Y"
    }
}
def scriptLinux(command) {
    sh "${command}"
}
def scriptLinuxWithReturnCommand(command) {
    sh "${command} 2>&1|tee ${workspace}/build.log"
    output=readFile("${workspace}/build.log")
    new File("${workspace}/build.log").delete()
    return output
}
def debug(message){
    isDebug=this.getPropertyFile().get("DEBUG")
    //BUILD_DEBUG_ENABLE=env['BUILD_DEBUG_ENABLE']
    BUILD_DEBUG_ENABLE=this.getPropertyFile().get("DEBUG")
    if(isNotNullandEmpty(isDebug) && isTrue(isDebug)){
        echo "DEBUG : "+ message
    }else{
        if(isNotNullandEmpty(BUILD_DEBUG_ENABLE) && isTrue(BUILD_DEBUG_ENABLE)){
            echo "DEBUG : "+ message
        }
    }
}
def replaceContentRgx(filePath,oldValue,newValue) {
    debug("${oldValue}.*----->${newValue} ${filePath}")
    String fileContent = readFile(filePath).replaceFirst("(?m)${oldValue}+.*\$",newValue)
    //echo fileContent
    writeFile file:filePath, text: fileContent
}

def replaceContentAll(filePath,oldValue,newValue) {
    debug("${oldValue}----->${newValue} ${filePath}")
    String fileContent = readFile(filePath).replaceAll(oldValue,newValue)
    writeFile file:filePath, text: fileContent
}
//Functions
def scriptCall(command) {
    scriptCall(command,false)
}

def scriptCall(command,winPath) {
    if (isUnix()) {
        scriptLinux(command)
    }
    else {
        if(winPath)
        {
            command=windowsPath(command)
        }
        echo "command=${command}"
        scriptWindows(command)
    }
}
def scriptCallWithReturnCommand(command) {
    output=scriptCallWithReturnCommand(command,false)
    return output
}

def scriptCallWithReturnCommand(command,winPath) {
    if (isUnix()) {
        output=scriptLinuxWithReturnCommand(command)
    }
    else {
        if(winPath)
        {
            command=windowsPath(command)
        }
        echo "command=${command}"
        output=scriptWindowsWithReturnCommand(command)
    }
    return output
}
def deleteFileWithCommand(filePath){
    if (isUnix()) {
        scriptCall("rm -rf ${filePath} | true")
    }else{
        scriptCall("rm -rf ${filePath} | true")
    }
}
def deleteFilewithPattern(folderPath,patternMatch){
    if (isUnix()) {
        scriptCall("rm -rf  ${folderPath}/${patternMatch} | true ")
    }else{
        folderPath=windowsPath(folderPath)
        scriptCall("cd ${folderPath} && del ${patternMatch} >nul")
    }
}
def renameFilewithPattern(folderPath,fileOldPath,patternMatch,newName){
    debug("${folderPath} ${fileOldPath} ${patternMatch} ${newName}")
    if (isUnix()) {
        scriptCall("mv ${folderPath}/${patternMatch} ${folderPath}/${newName} | true")
    }else{
        folderPath=windowsPath(folderPath)
        patternMatch=windowsPath(patternMatch)
        fileOldPath=windowsPath(fileOldPath)
        scriptCall("ren ${patternMatch} ${newName}")
        if(fileOldPath!=""){
            bat "xcopy ${folderPath}\\${fileOldPath}\\${newName} ${folderPath}  /Q /Y"
        }
    }
}
def pythonCall(command) {
    if (isUnix()) {
        scriptLinux("python3 "+command)
    }
    else {
        scriptWindows("python "+command)
    }
}
def pythonCall(path ,command) {
    if (isUnix()) {
        scriptLinux("cd ${path} && python3 "+command)
    }
    else {
        scriptWindows("cd ${path} && python "+command)
    }
}

def fileSeperator(){
    if (isUnix()) {
        return '/'
    }
    else{
        return '\\'
    }
}
def getAnsibleDir() {
    projectLocation=getProjectLocation()
    if(projectLocation == null || projectLocation == ""){
        projectLocation=""
        return "Ansible/"
    }else
    {return "${projectLocation}-ansible/Ansible/"}
}
def checkoutProject(adapter,branch){
    cleanWorkspace()
    dir('adapter'){deleteDir()}
    gitUrl=adapter.get('GIT_URL')
    PROJECT_LOCATION=adapter.get('PROJECT_LOCATION')
    dir(PROJECT_LOCATION){deleteDir()}
    dir(PROJECT_LOCATION+"-build"){deleteDir()}
    echo "INFO : Cloning branch=${branch} from ${gitUrl}"
    checkout([$class: 'GitSCM',
        branches: [[name: "${branch}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory',
                relativeTargetDir: 'adapter'], [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true]],
        gitTool: 'Default',
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: adapter.get('GIT_CREDENTIAL_ID'),url: adapter.get('GIT_URL')]]])
}

def checkoutProjectAndZip(gitUrl,branch,GIT_CREDENTIAL_ID,type,zipFlag,zipFileName){
    //dir(type){deleteDir()}
    //scriptCall("mkdir -p  ${workspace}"+fileSeperator()+type)
    zipFilePath=''
    echo "INFO : Cloning branch=${branch} from gitUrl=${gitUrl} with credentialsId=${GIT_CREDENTIAL_ID}"
    checkout([$class: 'GitSCM',
        branches: [[name: "${branch}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory',
                relativeTargetDir: type], [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true]],
        gitTool: 'Default',
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: GIT_CREDENTIAL_ID,url: gitUrl]]])

    if(zipFlag){
        scriptCall("jar -cMf "+zipFileName+".zip -C "+type+" .> nul ")
        zipFileName=zipFileName+".zip"
        zipFilePath="${workspace}/${zipFileName}"
    }
    return resolveWindowsPath(zipFilePath)
}


def buildTriggeredBy(){
    shortDescription= currentBuild.getBuildCauses()[0].shortDescription
    userId= currentBuild.getBuildCauses()[0].userId
    if( userId != null){
        BUILD_TRIGGER_BY = "${userId}"
    }else{
        BUILD_TRIGGER_BY = shortDescription
    }
    debug("BUILD_TRIGGER_BY: ${BUILD_TRIGGER_BY}")
    return BUILD_TRIGGER_BY
}
def buildDuration(){
    BUILD_DURATION = currentBuild.getDurationString()
    debug("BUILD_DURATION=${BUILD_DURATION}")
    BUILD_DURATION = BUILD_DURATION.split("and")[0].trim()
    return BUILD_DURATION
}
def cleanWorkspace(){
    try{
        fileOperations([fileDeleteOperation(includes: '*.pdf')])
        fileOperations([fileDeleteOperation(includes: '*.html')])
        fileOperations([fileDeleteOperation(includes: '*.xml')])
    }catch (e) {
        echo "cleanWorkspace() failed. Skipping cleanup..!!"
    }
}
def checkoutTemplate(utils,adapter,jenkins){
    checkoutTemplate(utils,adapter,jenkins,true)
}
def checkoutTemplate(utils,adapter,jenkins,isCleanup){
    if(!isDataModeEnabled()){
        if(isCleanup){
            //clean workspace for cicd flow
            cleanWorkspace()
        }
        buildTriggeredBy()
        //Started by timer with parameters  : cron |Started by user npddcsv / npd-dcsv |Started by upstream project|GenericTrigger:.*
        if(adapter.get('BUILD_TEMPLATE_GIT_URL')!=null && adapter.get('BUILD_TEMPLATE_GIT_URL')!=""){
            utils.checkoutGitRepo(adapter.get('BUILD_TEMPLATE_GIT_URL'),adapter.get('BUILD_TEMPLATE_GIT_BRANCH'),"template",adapter.get('BUILD_TEMPLATE_GIT_CREDENTIAL_ID'))
            deleteDirectory("utils")
            copyFolder("template/utils","${workspace}","utils")
        }
    }
}
def getBuildAgent(adapterProp,environment){
    key="BUILD_AGENT_CD"+"_"+environment
    BUILD_AGENT_CD = adapterProp.get("BUILD_AGENT_CD")
    BUILD_AGENT_CD_ENV = adapterProp.get(key)
    if(isNotNullandEmpty(BUILD_AGENT_CD_ENV)){
        echo "INFO: build machine ${key}=${BUILD_AGENT_CD_ENV} for environment ${environment} setup"
        return BUILD_AGENT_CD_ENV
    }
    else{
        if(isNotNullandEmpty(BUILD_AGENT_CD)){
            echo "INFO: build machine BUILD_AGENT_CD=${BUILD_AGENT_CD}"
            return BUILD_AGENT_CD
        }else{

            try{
                echo "WARN: BUILD_AGENT_CD is not existed in properties. Please pass BUILD_AGENT_CD or ${key} in adapter properties,Using default Agent=${agent}"
                return agent
            }catch (e) {
                error "BUILD_AGENT_CD is not existed in properties. Please pass BUILD_AGENT_CD or ${key} in adapter properties"
            }
        }
    }
    error "${key} is not existed in properties. Please pass BUILD_AGENT_CD or ${key} in adapter properties"
}
def getBuildAgent(adapterProp,environment,data){
    if(isNotNullandEmpty(data)){
        BUILD_AGENT_CD = adapterDataProp.get("BUILD_AGENT_CD")
        if(isNotNullandEmpty(BUILD_AGENT_CD)){
            return BUILD_AGENT_CD
        }else{
            echo "INFO: build machine agent=${agent}"
            return agent
        }

    }else{
        return getBuildAgent(adapterProp,environment)
    }
}
def deleteDirectory(directoryPath){
    if(fileExists(directoryPath)){
        dir(directoryPath){deleteDir()}
    }
}
def deleteFile(filePath){
    if (fileExists(filePath)) {
        new File(filePath).delete()
    } else {
        println "${filePath} file not found to delete"
    }
}
def windowsPath(tempfilePath){
    tempfilePath=tempfilePath.replace("/","\\")
    return tempfilePath
}
def copyFolder(srcPath,destinationPath,directoryName){
    if (isUnix()) {
        scriptCall("cp -r ${srcPath}/ ${destinationPath}/${directoryName} | true")
    }
    else{
        srcPath=windowsPath(srcPath)
        destinationPath=windowsPath(destinationPath)
        if(!fileExists("${destinationPath}\\${directoryName}")){
            bat("mkdir ${destinationPath}\\${directoryName}")
        }
        bat returnStatus: true, script: "xcopy ${srcPath} ${destinationPath}\\${directoryName} /O /X /E /H /K /Y /F /I >nul"
    }
}
def wincopyFolder(srcPath,destinationPath){
    bat returnStatus: true, script: "xcopy ${srcPath} ${destinationPath} /O /X /E /H /K /Y /F /I >nul"
}
def copyFileFolder(srcPath,destinationPath){
    fileOperations([fileCopyOperation(
        excludes: '',
        flattenFiles: false,
        includes: "${srcPath}",
        targetLocation: "${destinationPath}"
        )])
}
def checkoutGitRepo(url,branch,checkoutDir,credId){
    dir(checkoutDir){deleteDir()}
    echo "INFO : Cloning branch=${branch} from ${url}"
    checkout([$class: 'GitSCM',
        branches: [[name: branch]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory',
                relativeTargetDir: checkoutDir], [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true]],
        gitTool: 'Default',
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId:credId,url: url]]])
}
def setupScripts(ansibleTool)
{
    ansiblePath="${workspace}/"+getAnsibleDir()
    deleteDirectory(ansiblePath)
    if(isUnix()){
        sh "mkdir -p ${ansiblePath}"
    }
    else{
        fileOperations([folderCreateOperation("${workspace}/"+getAnsibleDir())])
    }
    unzip zipFile: "${workspace}/${ansibleTool}", dir: "${workspace}/"+getAnsibleDir(),quiet: true
    copyFile("ansible.cfg","${workspace}/"+getAnsibleDir(),"${workspace}")
}
def setupScripts(ansibleTool,path)
{
    scriptCall("rm -rf  ${workspace}/"+path+" | true ")
    scriptCall("mkdir -p  ${workspace}/"+path)
    scriptCall("unzip ${workspace}/${ansibleTool} -d ${workspace}/"+getAnsibleDir()+"  1>/dev/null")
    sh("cp -r ${workspace}/"+path+"/ansible.cfg ${workspace}/ansible.cfg")
}

def findAndReplaceInFileWithGroovy(filePath,oldValue,newValue) {
    String environmentFileContent = readFile(filePath).replaceAll(oldValue,newValue)
    writeFile file:filePath, text: environmentFileContent
}

def resolveAnsibleEnvironment(EnvironmentFilePath,hostFilePathParentPath){
    sh "python3 ${workspace}/"+getAnsibleDir()+"scripts/python_utility/Ansible_Customer_Env.py ${workspace}/${EnvironmentFilePath} ${workspace}/${hostFilePathParentPath} 1>/dev/null"
}
def resolveAnsibleEnvironmentFullPath(EnvironmentFilePath,hostFilePathParentPath){
    sh "python3 ${workspace}/"+getAnsibleDir()+"scripts/python_utility/Ansible_Customer_Env.py ${EnvironmentFilePath} ${workspace}/${hostFilePathParentPath} 1>/dev/null"
}

def getCurrentBuildLogs(){
    buildlogs = currentBuild.getRawBuild().getLog(20000).join('\n')
    newLines= buildlogs.split("\n")
    def buildlogsNew= ""
    newLines.each { String line ->
        if(!(line.startsWith("[Pipeline]") || line.startsWith(" > git") || line.startsWith("Started by ")|| line.startsWith("Started by ")|| line.startsWith("Fetching ")|| line.startsWith("Running ")|| line.startsWith("using ") || line.startsWith("Checking") || line.startsWith("Commit ") || line.startsWith("Obtained")|| line.startsWith("originally") || line.startsWith(" Started"))){
            buildlogsNew=buildlogsNew+line +"\n"
        }
    }
    return buildlogsNew
}

def buildJob(jobName,buildParams){
    RunWrapper output=build job: jobName,parameters: buildParams, propagate: false
    def buildMessage = getLogFromRunWrapper(output, 20000)
    def newFile= buildMessage.split("\n")
    def logs= ""
    def isPrint= false
    newFile.each { String line ->
        if(!(line.startsWith("[Pipeline]") || line.startsWith(" > git") || line.startsWith("Started by ")|| line.startsWith("Started by ")|| line.startsWith("Fetching ")|| line.startsWith("Running ")|| line.startsWith("using ") || line.startsWith("Checking") || line.startsWith("Commit ") || line.startsWith("Obtained")|| line.startsWith("originally") || line.startsWith(" Started"))){
            logs=logs+line +"\n"
        }
    }
    echo logs
    if(output.getCurrentResult() == "FAILURE"){
        error('Build failed:'+output.getAbsoluteUrl() )
    }
    return logs
}
def buildJob(jobName,buildParams,filters){
    RunWrapper output=build job: jobName,parameters: buildParams, propagate: false
    def buildMessage = getLogFromRunWrapper(output, 20000)
    def newFile= buildMessage.split("\n")
    def logs= ""
    def isPrint= false
    newFile.each { String line ->
        if(!(line.startsWith("[Pipeline]") || line.startsWith(" > git") || line.startsWith("Started by ")|| line.startsWith("Started by ")|| line.startsWith("Fetching ")|| line.startsWith("Running ")|| line.startsWith("using ") || line.startsWith("Checking") || line.startsWith("Commit ") || line.startsWith("Obtained")|| line.startsWith("originally") || line.startsWith(" Started"))){
            isAdd = true
            for (filter in filters) {
                if(line.startsWith(filter) || line.endsWith(filter)){
                    isAdd = false
                }
            }
            if(isAdd){
                logs=logs+line +"\n"
            }
        }
    }
    echo logs
    if(output.getCurrentResult() == "FAILURE"){
        error('Build failed:'+output.getAbsoluteUrl() )
    }
    return logs
}


def checkout(directory,GIT_URL,credentials,branch_name){
    dir(directory){deleteDir()}
    checkout poll: false,
    scm: [$class: 'GitSCM', branches: [[name: branch_name]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory',
                relativeTargetDir: directory]],
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: credentials,url: GIT_URL]]]
}

def resolveCredentialsIntoFile(credentialsId,userNameKey,passwordKey,filepath,filename){
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: credentialsId,
            usernameVariable: 'username', passwordVariable: 'password']]) {
        resolveCredentialsIntoFile(username,password,userNameKey,passwordKey,filepath,filename)

    }
}
def resolveCredentialsIntoFile(username,password,userNameKey,passwordKey,filepath,filename){
    replaceContentAll(filepath+fileSeperator()+filename,userNameKey,username)
    replaceContentAll(filepath+fileSeperator()+filename,passwordKey,password)
}

def resolveCredentialsIntoFilePath(credentialsId,userNameKey,passwordKey,filepath){
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: credentialsId,
            usernameVariable: 'username', passwordVariable: 'password']]) {
        resolveCredentialsIntoFilePath(username,password,userNameKey,passwordKey,filepath)

    }
}
def resolveCredentialsIntoFilePath(username,password,userNameKey,passwordKey,filepath){
    replaceContentAll(filepath,userNameKey,username)
    replaceContentAll(filepath,passwordKey,password)

}
def resolveCredentialsIntoFilePathWithRegX(credentialsId,userNameKey,passwordKey,filepath){
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: credentialsId,
            usernameVariable: 'username', passwordVariable: 'password']]) {
        resolveCredentialsIntoFilePathWithRegX(username,password,userNameKey,passwordKey,filepath)

    }
}
def resolveCredentialsIntoFilePathWithRegX(username,password,userNameKey,passwordKey,filepath){
    replaceContentRgx(filepath,userNameKey,userNameKey + username)
    if(password.startsWith("![")){
        debug("Password in encrypted format for ${username}, Converting into password escape chars")
        password= "\"\\\\\"${password}\\\\\"\""
    }
    replaceContentRgx(filepath,passwordKey,passwordKey + password)

}

def getRelatedPipelineFullName(BUILD_PIPELINE){
    CURRENT_FULL_PIPELINE=JOB_NAME
    THEJOB="${JOB_NAME.substring(JOB_NAME.lastIndexOf('/') + 1, JOB_NAME.length())}"
    BUILD_PIPELINE=CURRENT_FULL_PIPELINE.replace(THEJOB,BUILD_PIPELINE)
    return BUILD_PIPELINE
}

//CICD pipeline : CD stage

def getSettingsFileName(){
    settingFileName=jenkins.get("SETTINGS_FILE_NAME")
    return settingFileName
}
def getSettingsPath(){
    setupSettingsFile()
    MVN_SETTINGS_TEMPLATE=jenkins.get("MVN_SETTINGS_TEMPLATE")
    MVN_SETTINGS_TEMPLATE_PATH=new File("${workspace}/${MVN_SETTINGS_TEMPLATE}").getParent()
    settingParentPath = MVN_SETTINGS_TEMPLATE_PATH
    return settingParentPath
}
def setupSettingsFile(){
    MVN_SETTINGS_TEMPLATE=jenkins.get("MVN_SETTINGS_TEMPLATE")
    MVN_SETTINGS_TEMPLATE_PATH=new File("${workspace}/${MVN_SETTINGS_TEMPLATE}").getParent()
    settingFileName=jenkins.get("SETTINGS_FILE_NAME")
    settingsFilePath = "${MVN_SETTINGS_TEMPLATE_PATH}/${settingFileName}"
    isSettingsFileExists = fileExists settingsFilePath
    debug("Checking setting.xml file:"+settingsFilePath)
    if(!isSettingsFileExists){
        unzip zipFile: "${workspace}/${MVN_SETTINGS_TEMPLATE}", dir: "${MVN_SETTINGS_TEMPLATE_PATH}",quiet: true
    }
    return settingsFilePath
}

def convertListToString(list){
    envsStr=""
    for (i in list) {
        if(envsStr==""){
            envsStr =i
        }else{
            envsStr =envsStr+","+i
        }
    }
    return envsStr
}


def createFile(filePath,content){
    def filecontent=""
    content.each { item ->
        filecontent=filecontent+"\n"+item
    }
    writeFile file: filePath, text: filecontent
}
def getApplicationAction(applicationAction){
    if(applicationAction=="reinstall" || applicationAction=="reinstall,backup"){
        applicationAction="redeploy"
    }
    if(applicationAction=="delete"){
        applicationAction="undeploy"
    }
    return applicationAction
}
def checkApplicationValidAction(applicationAction){
    validApplicationActions=['', 'redeploy', 'undeploy', 'start', 'stop', 'restart', 'status' , 'urestart', 'enableLogger', 'disableLogger', 'log4jPatch']
    if(validApplicationActions.contains(applicationAction)){
        return true
    }
    else{
        return false
    }
}

def utilJarPath(){
    return jenkins.get("CONNECT_INSTALL_UTIL_PATH")
}
def getJdaConnectToolPath(){
    projectLocation=getProjectLocation()
    utilJarPath=utilJarPath()
    return "${workspace}/${projectLocation}-ansible/Ansible/${utilJarPath}"
}
def getJdaConnectToolPathWithAnsiblePath(ansUtils){
    getAnsibleDir=ansUtils.getAnsibleDir()
    utilJarPath=utilJarPath()
    return "${workspace}/${getAnsibleDir}/${utilJarPath}"
}


def getContentLine(buildLogs,startsWithContent){
    findLine=""
    buildLogsList=buildLogs.split("\n")
    buildLogsList.each { lines ->
        lines=lines.trim()
        //debug(lines)
        if(lines.startsWith(startsWithContent)){
            debug("${startsWithContent} found under ${lines}")
            findLine = lines
            return findLine
        }
    }
    return findLine
}
def isTrue(state){
    if (state instanceof String){
        if(state == "true"){
            return true
        }else{
            return false
        }}
    if(state instanceof boolean){
        return state
    }
    if(state instanceof java.lang.Boolean){
    return state
    }
    return state
}
def getContentFromFile(content,startValue,endValue){
    capturedContent=""
    if(isNotNullandEmpty(content)){
        contentList=content.split("\n")
        foundFirst=false
        foundLast=false
        contentList.each { lines ->
            if(foundLast){
                return
            }
            if(lines.contains(startValue)){
                foundFirst=true
            }
            if(foundFirst){
                capturedContent=capturedContent+"\n"+lines
            }
            if(foundFirst){
                if(lines.contains(endValue)){
                    foundLast=true
                }
            }
        }}
    return capturedContent
}
def isFalse(state){
    if(isTrue(state)){
        return false
    }else{
        return true
    }
}
def trueCheck(String ... args){
    return args.contains("true")
}
def isTrueAll(String ... args){
    return !args.contains("false")
}
def isEmpty(value){
    if (value == ""){
        return true
    }else{
        return false
    }
}
def isNullorEmpty(value){
    if (value == ""){
        return true
    }
    if (value == null){
        return true
    }else{
        return false
    }
}
def isNotNullandEmpty(value){
    if (value!=null && value!=""){
        return true
    }else{
        return false
    }
}

def isNotEmpty(value){
    return !isEmpty(value)
}
def isNotEmptyString(value){
    result=!isEmpty(value)
    if (result){
        return "true"
    }else{
        return "false"
    }
}



def getAdapterWebhooktoken(adapterProp,typebuild){
    webhookToken=adapterProp.get('ADAPTER_'+typebuild+'_WEBHOOK_TOKEN')
    debug("webhookToken=${webhookToken}")
    if(isNotNullandEmpty(webhookToken)){
        return webhookToken
    }else{
        return UUID.randomUUID().toString()
    }
}


def isAnsibleMode(adapterProp){
    if(isDataModeEnabled()){
        genericMode = isGenericModeEnabledInDataMode(adapterDataProp)
        if(genericMode){
           debug("INFO: isAnsibleMode: Making ansible generic mode enable")
           return false
        }
    }
    ANSABLE_DEPLOY_MODE= adapterProp.get("ANSABLE_DEPLOY_MODE")
    if(isNotNullandEmpty(ANSABLE_DEPLOY_MODE) && ANSABLE_DEPLOY_MODE =="false"){
        debug("INFO: Making ansible mode disable")
        ANSABLE_DEPLOY_MODE=false
    }
    else{
        debug("INFO: Making ansible mode enable")
        ANSABLE_DEPLOY_MODE=true
    }
    return ANSABLE_DEPLOY_MODE
}
def isAnsibleMode(adapterProp,envNodeValue){
    if(isDataModeEnabled()){
        genericMode = isGenericModeEnabledInDataMode(adapterDataProp)
        if(genericMode){
            debug("INFO: isAnsibleMode: Making ansible generic mode enable")
            return false
        }
    }
    ANSABLE_DEPLOY_MODE_ENV= adapterProp.get("ANSABLE_DEPLOY_MODE_"+envNodeValue)
    debug("ANSABLE_DEPLOY_MODE_${envNodeValue}=${ANSABLE_DEPLOY_MODE_ENV}")
    if(isNotNullandEmpty(ANSABLE_DEPLOY_MODE_ENV) && ANSABLE_DEPLOY_MODE_ENV =="false"){
        debug("INFO: Making ansible mode disable for environment=${envNodeValue}")
        return false
    }
    if(isNotNullandEmpty(ANSABLE_DEPLOY_MODE_ENV) && ANSABLE_DEPLOY_MODE_ENV =="true"){
        debug("INFO: Making ansible mode enable for environment=${envNodeValue}")
        return true
    }
    return isAnsibleMode(adapterProp)

}

def appsInList(Framework_Applications){
    fwList=Framework_Applications.trim().split(",")
    debug("fwList="+fwList)
    if(isEmpty(Framework_Applications)){
        return []
    }
    return fwList
}


def isEnvironmentEmpty(environment){
    if(!isNotNullandEmpty(environment) && !isDataModeEnabled()){
        error "Select required environment from ${environments} build perameters to perform action on target environment."
    }
}
def isEnvironmentFileEmpty(environmentPath,environmentKey){
    if(!isNotNullandEmpty(environmentPath)){
        error "${environmentKey} not exist in properties file."
    }
}
def getFrameworkServerNodes(adapterProp){
    FW_SERVERS = adapterProp.get("FW_SERVERS")
    if(isNotNullandEmpty(FW_SERVERS)){
        return FW_SERVERS
    }
    else{
        echo "WARN: Taking default server nodes as FW_SERVERS not configured in properties"
        return "serversvc,servering"
    }
}
def getPreSetupNodes(adapterProp){
    PRESETUP_NODES = adapterProp.get("PRESETUP_NODES")
    if(isNotNullandEmpty(PRESETUP_NODES)){
        return PRESETUP_NODES
    }
    else{
        return "java,python,directories,mssqlclient,mulednscheck,certificate,splunkforwarder,activemq,command,info,orclsqlsetup"
    }
}
def setCurrentBuildName(environment,Framework_Applications,Adapters){
    Framework_Applications = Framework_Applications.trim()
    Adapters = Adapters.trim()
    curentDisplayName = BUILD_NUMBER+": "
    if(isNotEmpty(environment)){
        curentDisplayName = curentDisplayName + "[${environment}] "
    }
    if(isNotEmpty(Framework_Applications)){
        if(isTrue(cps)){
            curentDisplayName = curentDisplayName + "(cps,${Framework_Applications}"
        }
        else{
            curentDisplayName = curentDisplayName + "(${Framework_Applications}"
        }
    }
    if(isNotEmpty(Adapters)){
        if(isNotEmpty(Framework_Applications)){
            curentDisplayName = curentDisplayName + ",${Adapters})"
        }else{
            curentDisplayName = curentDisplayName + "(${Adapters})"
        }
    }else{
        if(isNotEmpty(Framework_Applications)){
            curentDisplayName = curentDisplayName + ")"
        }
    }

    infra = infra.trim()
    currentBuild.displayName = curentDisplayName
    curentDisplayDescription = "{\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"infra\": \"${infra}\",\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"Framework_Servers\": \"${Framework_Servers}\",\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"cps\": ${cps},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"db\": ${db},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"api\": ${api},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"configpush\": ${configpush},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"configupdate\": ${configupdate},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"Framework_Applications\": \"${Framework_Applications}\",\n"
    curentDisplayDescription = curentDisplayDescription+ "\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"Adapters\": \"${Adapters}\",\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"AdapterServer\": ${AdapterServer},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"adapterdb\": ${adapterdb},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"adapterapi\": ${adapterapi},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"adapterconfigpush\": ${adapterconfigpush},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"adapterconfigupdate\": ${adapterconfigupdate},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"adapter\": ${adapter},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"REGRESSION_TEST\": ${REGRESSION_TEST},\n"
    curentDisplayDescription = curentDisplayDescription+ "  \"action\": \"${action}\"\n"
    curentDisplayDescription = curentDisplayDescription+ " }\n"
    if(isDataModeEnabled()){
        echo "INFO: Getting customer"
        customerName = adapterDataProp.get("customer")
        if(isNotNullandEmpty(customerName)){
            curentDisplayDescription = curentDisplayDescription+ "\nINFO: customer=${customerName}\n"
        }
    }
    currentBuild.description = curentDisplayDescription
    echo curentDisplayDescription
}
def setCurrentBuildDescription(messageDesc){
    if(isDataModeEnabled()){
        curentDisplayDescription = curentDisplayDescription + messageDesc + "\n"
        currentBuild.description = curentDisplayDescription
    }
}
def isDataModeEnabled(){
    try{
        dataModeUpdated = dataMode
        debug("dataMode defined.....!!")
        return dataModeUpdated
    }catch (e) {
        debug("dataMode undefined.....!!")
        return false
    }
}


def resolveCommonConfigurationForDataMode(envFilePath){
    if(isDataModeEnabled()){
        updateJfrogCreds = true
        updatearmExchangeCreds = true
        ARTIFACT_SOURCE = adapterDataProp.get("ARTIFACT_SOURCE")
        if(isNotNullandEmpty(ARTIFACT_SOURCE)){
            echo "INFO: ARTIFACT_SOURCE=" + ARTIFACT_SOURCE
            switch(ARTIFACT_SOURCE) {
                case "local":
                    echo "INFO: Taking artifact from local repository for server installation & application deployment (api/db/config) by replacing L_ART_"
                    findAndReplaceInFileWithGroovy(envFilePath, "L_ART_", "")
                    updateJfrogCreds = false
                    echo "Skip updating JFROG_CREDENTIAL_ID to environment file " + envFilePath
                    updatearmExchangeCreds = false
                    echo "Skip updating ARM_EXCHANGE_CREDENTIAL_ID to environment file " + envFilePath
                    break;
                case "jfrog":
                    echo "INFO: Taking artifact from jfrog repository for server installation & application deployment (api/db/config) by replacing JF_ART_ & JF_ART_MRT_ to "
                    findAndReplaceInFileWithGroovy(envFilePath, "JF_ART_", "")
                    findAndReplaceInFileWithGroovy(envFilePath, "JF_ART_MRT_", "")
                    updateJfrogCreds = true
                    echo "Skip updating ARM_EXCHANGE_CREDENTIAL_ID to environment file " + envFilePath
                    updatearmExchangeCreds = false
                    break;
                case "exchange":
                    echo "INFO: Taking artifact from jfrog repository for server installation & jar from exchange application deployment (api/db/config) by replacing JF_ART_ & JF_ART_MRT_ to "
                    findAndReplaceInFileWithGroovy(envFilePath, "EX_ART_", "")
                    findAndReplaceInFileWithGroovy(envFilePath, "JF_ART_MRT_", "")
                    break;
            }
        }
        jFrogCredentialsId = adapterDataProp.get("JFROG_CREDENTIAL_ID")
        armExchangeUserCedentialsId = adapterDataProp.get("ARM_EXCHANGE_CREDENTIAL_ID")
        armUserCredentialsId = adapterDataProp.get("ARM_CREDENTIAL_ID")
        if(isNotNullandEmpty(armExchangeUserCedentialsId) && updatearmExchangeCreds){
            ansUtils.resolveCredentialsIntoEnvironmentFileGroovy(armExchangeUserCedentialsId,'EXTERNAL_EXCHANGE_ANYPOINT_USER_NAME_VALUE','EXTERNAL_EXCHANGE_ANYPOINT_USER_PASSWORD_VALUE',envFilePath)
        }
        if(isNotNullandEmpty(jFrogCredentialsId) && updateJfrogCreds){
            ansUtils.resolveCredentialsIntoEnvironmentFileGroovy(jFrogCredentialsId,'JFROG_USERNAME_VALUE','JFROG_PASSWORD_VALUE',envFilePath)
        }
        if(isNotNullandEmpty(armUserCredentialsId)){
            ansUtils.resolveCredentialsIntoEnvironmentFileGroovy(armUserCredentialsId,'ANYPOINT_USER_NAME_VALUE','ANYPOINT_USER_PASSWORD_VALUE',envFilePath)
        }
        if(isFalse(api)){
            findAndReplaceInFileWithGroovy(envFilePath, "cpsConfigForAPIInstance", "cpsConfigForNotAPIInstance")
        }
    }
}
def resolveDataTypeMachineCredentials(envFilePath){
    credentialsId = adapterDataProp.get("VM_CRED_ID")
    credentialsIdType = adapterDataProp.get("VM_CRED_TYPE")
    if(isNotNullandEmpty(credentialsIdType) && credentialsIdType == "password"){
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: credentialsId,
                usernameVariable: 'username', passwordVariable: 'password']]) {
            debug("Updating sudo password for user [${username}] into file ${envFilePath}")
            findAndReplaceInFileWithGroovy(envFilePath, '\"ansible_connection\": \"ssh\"', "\"ansible_connection\": \"ssh\",\"ansible_become_pass\": \"${password}\"")
        }
    }
    fileContent = readFile("${workspace}/${envFilePath}")
    //TODO: remove print
    echo "----------------${envFilePath}------------------------"
    echo "${fileContent}"
    echo "------------------------------------------------------"
}


def getValue(propertyFile,keyNode){
    value = propertyFile.get(keyNode)
    debug("${keyNode}=${value}")
    return  value
}


def InfraSetup(adapterProp,environment,infra,action){
    buildBeforeDuration=buildDuration()
    infra = infra.trim()
    echo "INFO: Installation started for [${infra}], Start build duration = ${buildBeforeDuration}"
    if(isDataModeEnabled()){
        ENVIRONMENT_INFRA = infraEnvironmentFilePath
        resolveDataTypeMachineCredentials(ENVIRONMENT_INFRA)
    }
    else{
        isEnvironmentEmpty(environment)
        ENVIRONMENT_INFRA=adapterProp.get("ENVIRONMENT_INFRA"+"_"+environment)
        if(isNullorEmpty(ENVIRONMENT_INFRA)){
            echo "ENVIRONMENT_INFRA_${environment} not set in properties file. Taking ENVIRONMENT_FW_${environment} value for infra setup"
            ENVIRONMENT_INFRA=adapterProp.get("ENVIRONMENT_FW"+"_"+environment)
            isEnvironmentFileEmpty(ENVIRONMENT_INFRA,"ENVIRONMENT_FW"+"_"+environment)
            echo "ENVIRONMENT_FW_${environment}=${workspace}/${ENVIRONMENT_INFRA}"
            credentialsId=getCredentials(adapterProp,environment,"FW_VM_CRED_ID")
        }else{
            isEnvironmentFileEmpty(ENVIRONMENT_INFRA,"ENVIRONMENT_INFRA"+"_"+environment)
            echo "INFO: ENVIRONMENT_INFRA_${environment}=${workspace}/${ENVIRONMENT_INFRA}"
            credentialsId=getCredentials(adapterProp,environment,"INFRA_VM_CRED_ID")
        }
        resolveCommonDependenciesInEnvironmentFile(adapterProp,ENVIRONMENT_INFRA,environment,true,credentialsId,"")
    }

    environmentDir = utils.getAnsibleDir()+"environment/ConnectEnvironmentInfra"
    hostFilePath = "${environmentDir}/hosts.ini"

    ansUtils.setupAnsibleCommon(ansibleTool)
    if(isNotNullandEmpty(infra)){
        infraNodes=infra.split(",")
        if(isAnsibleMode(adapterProp,environment)){
            ansUtils.resolveAnsibleEnvironmentFileFullPath(ENVIRONMENT_INFRA,environmentDir)
            if(infraNodes.contains("mountdisk")){
                ansUtils.mountDiskData(credentialsId,hostFilePath,"mountdisk")
            }
            if(infraNodes.contains("command")){
                ansUtils.ansibleNativeCommand(credentialsId,hostFilePath,"command","{{COMMAND_CLI}}")
            }
            if(infraNodes.contains("jdk") || infraNodes.contains("java")){
                ansUtils.installTools(credentialsId,hostFilePath,'java',"jdk")
            }
            if(infraNodes.contains("python")){
                ansUtils.installTools(credentialsId,hostFilePath,'python',"python")
            }
            if(infraNodes.contains("python_modules")){
                ansUtils.installPythonModule(credentialsId,hostFilePath,'python_modules')
            }

            if(infraNodes.contains("directories")|| infraNodes.contains("mkdir")){
                directories= ["{{SERVER_INSTALL_PATH}}", "{{MULE_SERVER_TEMP_PATH}}", "{{BACKUPS_PATH}}", "{{ARCHIVE_FILES}}", "{{CPS_NATIVE_CONFIG_PATH}}"]
                for(i = 0; i < directories.size() ; i++){
                    ansUtils.makeDirectoryPath(credentialsId,hostFilePath,"mkdir",directories[i])
                }
                if(infraNodes.contains("certificate")){
                    ansUtils.makeDirectoryPath(credentialsId,hostFilePath,"mkdir","{{CERTIFICATE_PATH}}")
                }
            }
            if(infraNodes.contains("mssqlclient")){
                ansUtils.mssqlClientInstall(credentialsId,hostFilePath,"mssqlclient")
            }
            if(infraNodes.contains("mulednscheck")){
                ansUtils.muleDNSCheck(credentialsId,hostFilePath,'mulednscheck',"False")
            }
            if(infraNodes.contains("certificate")){
                ansUtils.certificate(credentialsId,hostFilePath,'certificate')
            }
            if(infraNodes.contains("splunkforwarder")){
                ansUtils.installSplunk(credentialsId,hostFilePath,'splunk',action)
            }
            if(infraNodes.contains("orclsqlsetup")){
                ansUtils.installOracle(credentialsId,hostFilePath,'orclsqlsetup',action)
            }
            if(infraNodes.contains("activemq")){
                ansUtils.dbutilCheckscript(credentialsId,hostFilePath,"activemqdb")
                ansUtils.installActiveMQ(credentialsId,hostFilePath,"activemq",action)
                if(action!="stop"){
                    ansUtils.validateActiveMQ(credentialsId,hostFilePath,"activemq")
                }
            }
            if(infraNodes.contains("info")){
                ansUtils.utils(credentialsId,hostFilePath,"info")
            }
            if(infraNodes.contains("orgEnvCreate")){
                // Org & environment creation :
                if(isDataModeEnabled()){
                    resolveInfraMuleAccountDatamode(frameworkEnvironmentFilePath)
                    ansUtils.createMuleOrgUtil(credentialsId,hostFilePathFW,"orgutil","createOrganization")
                    ansUtils.createMuleOrgEnvironmentUtil(credentialsId,hostFilePathFW,"orgutil","createMuleEnvironment")
                    //delete Sandbox Design if exists
                    if(infraNodes.contains("deleteOrgDefaultEnvs")){
                        echo "INFO: Deleting Sandbox,Design from Organization"
                        replaceContentRgx(ENVIRONMENT_FW,"\"ARM_ENVIRONMENT_NAME\":","\"ARM_ENVIRONMENT_NAME\": \"Sandbox,Design\",")
                        ansUtils.resolveAnsibleEnvironmentFileFullPath("${workspace}/${ENVIRONMENT_FW}","${workspace}/${environmentDir}")
                        ansUtils.createMuleOrgEnvironmentUtil(credentialsId,hostFilePathFW,"orgutil","deleteMuleEnvironment")
                    }
                }
            }
            if(infraNodes.contains("arm_access")){
                if(isDataModeEnabled()){
                    try{
                        if(!infraNodes.contains("orgEnvCreate")){
                            resolveInfraMuleAccountDatamode(frameworkEnvironmentFilePath)
                        }
                        ansUtils.createMuleOrgUtil(credentialsId,hostFilePathFW,"orgutil","getOrgEnv")
                        ansUtils.createMuleOrgUtil(credentialsId,hostFilePathFW,"orgutil","getOrgEnvs")
                    }catch (e) {
                        echo "WARN: Getting all Organizations details failed. Skipping getOrgEnvs..!!"
                    }
                }
            }
        }
        else{
            if(infraNodes.contains("splunkforwarder")){
                ansUtils.splunkLocalInstallation(ENVIRONMENT_INFRA,action,isGenericMode(adapterProp))
            }
            if(infraNodes.contains("activemq")){
                ansUtils.activeMQLocalInstallation(ENVIRONMENT_INFRA,action,isGenericMode(adapterProp))
            }
            if(infraNodes.contains("directories")){
                ansUtils.directoryLocalInstallation(ENVIRONMENT_INFRA,isGenericMode(adapterProp))
            }
            if(infraNodes.contains("mulednscheck")){
                ansUtils.muleDNSLocalCheck(ENVIRONMENT_INFRA,isGenericMode(adapterProp))
            }
            if(infraNodes.contains("command")){
                ansUtils.commandlocal(ENVIRONMENT_INFRA,isGenericMode(adapterProp))
            }
        }
        buildAfterDuration=buildDuration()
        buildAfterDurationMsg = "INFO: Installation completed for infra=[${infra}], Duration summary: [${buildBeforeDuration}]-->[${buildAfterDuration}]"
        echo buildAfterDurationMsg
        setCurrentBuildDescription(buildAfterDurationMsg)
    }

}

def resolveInfraMuleAccountDatamode(frameworkEnvironmentFilePath){
    ENVIRONMENT_FW = frameworkEnvironmentFilePath
    resolveCommonConfigurationForDataMode(ENVIRONMENT_FW)
    resolveDataTypeMachineCredentials(ENVIRONMENT_FW)
    environmentDir = utils.getAnsibleDir()+"environment/ConnectEnvironment"
    hostFilePathFW = "${environmentDir}/hosts.ini"
    ansUtils.setupAnsibleCommon(ansibleTool)
    ansUtils.resolveAnsibleEnvironmentFileFullPath("${workspace}/${ENVIRONMENT_FW}","${workspace}/${environmentDir}")
}
//Regression

def checkBranch(newBranch){
    dir('adapter'){
        branch =sh(returnStdout: true, script: "git ls-remote --heads origin ${newBranch}").trim()
        echo "INFO: branch=${branch}"
    }
}
//SCAN

def runBlackduck(adapter,version){
    BLACKDUCK_PROJECT_NAME=adapter.get('BLACKDUCK_PROJECT_NAME')
    blackduck(BLACKDUCK_PROJECT_NAME,version)

}
def runCheckmarx(adapter,version){
    CHECKMARX_PROJECT_NAME=adapter.get('CHECKMARX_PROJECT_NAME')
    CHECKMARX_TEAM_PATH=adapter.get('CHECKMARX_TEAM_PATH')
    checkmarx(CHECKMARX_PROJECT_NAME,version,CHECKMARX_TEAM_PATH)
    directory=getProjectLocation()
    fullAppPath="${WORKSPACE}"+fileSeperator()+directory
    //scriptCall("rm -rf ${WORKSPACE}/${directory}-build/Checkmarx | true ")
    deleteDirectory("${WORKSPACE}/${directory}-build/Checkmarx")
    //scriptCall("mv ${WORKSPACE}/${directory}/Checkmarx ${WORKSPACE}/${directory}-build/ | true ")
    copyFolder("${WORKSPACE}/${directory}/Checkmarx","${WORKSPACE}/${directory}-build","Checkmarx")
    deleteDirectory("${WORKSPACE}/${directory}/Checkmarx")
}

def runSecurityScan(adapter,version,BlackDuck,CheckMarx){
    if(isTrue(BlackDuck)){
        runBlackduck(adapter,version)
    }
    if(isTrue(CheckMarx)){
        runCheckmarx(adapter,version)
    }
    echo "TODO: SonarQube"
}
def blackduck(BLACKDUCK_PROJECT_NAME,BLACKDUCK_PROJECT_VERSION){
    echo "INFO: Building BlackDuck scan....!!!!"
    PROJECT_DIRECTORY=getProjectLocation()
    //scriptCall("rm -rf  ${workspace}/*BlackDuck_RiskReport.pdf | true ")
    deleteFilewithPattern(workspace,"*BlackDuck_RiskReport.pdf")
    BD_ADDITIONAL_ARGS=adapter.get('BD_ADDITIONAL_ARGS')
    BD_Command="--detect.project.name=${BLACKDUCK_PROJECT_NAME} --detect.project.version.name=${BLACKDUCK_PROJECT_VERSION}  --detect.source.path=${WORKSPACE}/${PROJECT_DIRECTORY}"
    //BD_Command=BD_Command+" --detect.maven.build.command='--settings ${settingsPath}'"
    echo "INFO: BlackDUck build arguments : "+BD_Command
    synopsys_detect "${BD_Command} ${BD_ADDITIONAL_ARGS}"
    if(isFalse(PublishArtifact)){
        //scriptCall("rm -rf ${settingsPath} | true ")
        deleteFile(settingsPath)
    }

}
def checkmarx(CHECKMARX_PROJECT_NAME,CHECKMARX_PROJECT_VERSION,CHECKMARX_TEAM_PATH){
    echo "INFO: Building CheckMarx scan....!!!!"
    PROJECT_DIRECTORY=getProjectLocation()
    CHECKMARX_CREDENTIAL_ID=adapter.get('CHECKMARX_CREDENTIAL_ID')
    CHECKMARX_URL=adapter.get('CHECKMARX_URL')
    echo "INFO: Building CheckMarx scan with CHECKMARX_URL=${CHECKMARX_URL} ${CHECKMARX_PROJECT_NAME}:${CHECKMARX_PROJECT_VERSION} on teampath ${CHECKMARX_TEAM_PATH} with credentialid ${CHECKMARX_CREDENTIAL_ID}"
    dir(PROJECT_DIRECTORY){
        step([$class: 'CxScanBuilder',
            comment: CHECKMARX_PROJECT_VERSION,
            credentialsId: CHECKMARX_CREDENTIAL_ID,
            excludeFolders: '.git',
            excludeOpenSourceFolders: '',
            exclusionsSetting: 'job',
            failBuildOnNewResults: false,
            generateXmlReport: true,
            generatePdfReport: true,
            failBuildOnNewSeverity: 'HIGH',
            filterPattern: '''!**/_cvs/**/*, !**/.svn/**/*,   !**/.hg/**/*,   !**/.git/**/*,  !**/.bzr/**/*, !**/bin/**/*,
                                    !**/obj/**/*,  !**/backup/**/*, !**/.idea/**/*, !**/*.DS_Store, !**/*.ipr,     !**/*.iws,
                                    !**/*.bak,     !**/*.tmp,       !**/*.aac,      !**/*.aif,      !**/*.iff,     !**/*.m3u, !**/*.mid, !**/*.mp3,
                                    !**/*.mpa,     !**/*.ra,        !**/*.wav,      !**/*.wma,      !**/*.3g2,     !**/*.3gp, !**/*.asf, !**/*.asx,
                                    !**/*.avi,     !**/*.flv,       !**/*.mov,      !**/*.mp4,      !**/*.mpg,     !**/*.rm,  !**/*.swf, !**/*.vob,
                                    !**/*.wmv,     !**/*.bmp,       !**/*.gif,      !**/*.jpg,      !**/*.png,     !**/*.psd, !**/*.tif, !**/*.swf,
                                    !**/*.jar,     !**/*.zip,       !**/*.rar,      !**/*.exe,      !**/*.dll,     !**/*.pdb, !**/*.7z,  !**/*.gz,
                                    !**/*.tar.gz,  !**/*.tar,       !**/*.gz,       !**/*.ahtm,     !**/*.ahtml,   !**/*.fhtml, !**/*.hdm,
                                    !**/*.hdml,    !**/*.hsql,      !**/*.ht,       !**/*.hta,      !**/*.htc,     !**/*.htd, !**/*.war, !**/*.ear,
                                    !**/*.htmls,   !**/*.ihtml,     !**/*.mht,      !**/*.mhtm,     !**/*.mhtml,   !**/*.ssi, !**/*.stm,
                                    !**/*.stml,    !**/*.ttml,      !**/*.txn,      !**/*.xhtm,     !**/*.xhtml,   !**/*.class, !**/*.iml,!Checkmarx/Reports/*.*''',
            fullScanCycle: 10,
            //groupId: '',
            includeOpenSourceFolders: '',
            osaArchiveIncludePatterns: '*.zip, *.war, *.ear, *.tgz',
            osaInstallBeforeScan: false,
            //password: '',
            preset: '36',
            projectName: CHECKMARX_PROJECT_NAME,
            sastEnabled: true,
            serverUrl: CHECKMARX_URL,
            teamPath: CHECKMARX_TEAM_PATH,
            sourceEncoding: '5',
            //username: '',
            vulnerabilityThresholdResult: 'FAILURE',
            waitForResultsEnabled: true])
    }
}
def getExchangeObject(){
    [name: TargetExchange, username: ExchangeUser, password: ""]
}
//Email enable
def sendEmail_CI(adapterProp,buildStatus){
    EMAIL_GROUP_CI=adapterProp.get("EMAIL_GROUP_CI")
    EMAIL_SUBJECT_CI=adapterProp.get("EMAIL_SUBJECT_CI")
    EMAIL_CONTENT_CI=adapterProp.get("EMAIL_CONTENT_CI")
    EMAIL_ENABLE_CI=adapterProp.get("EMAIL_ENABLE_CI")
    GIT_URL=adapterProp.get("GIT_URL")
    EXCHANGE_API_ASSET_ID=getPropertyValue("EXCHANGE_API_ASSET_ID")
    EXCHANGE_ASSET_ID=getPropertyValue("EXCHANGE_ASSET_ID")
    debug("buildStatus=${buildStatus}")
    debug("EXCHANGE_ASSET_ID=${EXCHANGE_ASSET_ID}")
    EMAIL_CONTENT_CI=EMAIL_CONTENT_CI.replace('${GIT_URL}',GIT_URL)
    BUILD_TRIGGER_BY = buildTriggeredBy()
    BUILD_DURATION= buildDuration()
    EMAIL_CONTENT_CI=EMAIL_CONTENT_CI.replace('${BUILD_TRIGGER_BY}',BUILD_TRIGGER_BY)
    EMAIL_CONTENT_CI=EMAIL_CONTENT_CI.replace('${BUILD_DURATION}',BUILD_DURATION)
    if(buildStatus){
        debug("munit=${munit}")
        debug("PublishToExchange=${PublishToExchange}")
        debug("munit=${munit}")
        debug("EXCHANGE_API_ASSET_ID=${EXCHANGE_API_ASSET_ID}")
        if(isTrue(munit)){
            munitUrl="<br>MunitCoverageReport= ${BUILD_URL}AdapterMunitReport<br>"
            munitData=getMunitBuildData()
            munitCodeCoverage=munitData.get("munitCodeCoverage")
            Tests=munitData.get("Tests")
            Errors=munitData.get("Errors")
            Failures=munitData.get("Failures")
            Skipped=munitData.get("Skipped")
            munitUrl=munitUrl+"<br>MunitCoverage=${munitCodeCoverage}  [Tests=${Tests}, Errors=${Errors}, Failures=${Failures}, Skipped=${Skipped}]<br>"
            munitTestResult=munitData.get("munitTestResult")
            writeFile file:"${workspace}/MunitTestResult.html", text: munitTestResult
            archiveBuildArtifacts("MunitTestResult.html")
            EMAIL_CONTENT_CI=EMAIL_CONTENT_CI+munitUrl
        }
        if(utils.isTrue(PublishToExchange)){
            ExchangeAsset="<br><br>Exchange asset URL= https://anypoint.mulesoft.com/exchange/${businessGroupId}/${EXCHANGE_ASSET_ID}/${version}<br>    TargetExchange=${TargetExchange}<br>    EXCHANGE_ASSET_ID=${EXCHANGE_ASSET_ID}<br>    EXCHANGE_ASSET_VERSION=${version}"
            EMAIL_CONTENT_CI=EMAIL_CONTENT_CI+ExchangeAsset

            EMAIL_ENABLE_CI=adapterProp.get("EMAIL_ENABLE_CI")
            if(isNotNullandEmpty(EXCHANGE_API_ASSET_ID)){
                ExchangeAsset="<br><br>Exchange API asset URL= https://anypoint.mulesoft.com/exchange/${businessGroupId}/${EXCHANGE_API_ASSET_ID}/${version}<br>    TargetExchange=${TargetExchange}<br>    EXCHANGE_API_ASSET_ID=${EXCHANGE_ASSET_ID}<br>    EXCHANGE_API_ASSET_VERSION=${version}"
                EMAIL_CONTENT_CI=EMAIL_CONTENT_CI+ExchangeAsset
            }
        }
    }else{
        EMAIL_CONTENT_CI=EMAIL_CONTENT_CI + "<br>Please find build logs attachment (or) check build console for failure Build console: ${BUILD_URL}console<br> <br>"
    }

    EMAIL_CONTENT_CI=EMAIL_CONTENT_CI+"<br><br> Thanks & Regards <br>Build Admin"
    attachment=""
    directory=getProjectLocation()
    PROJECT_LOCATION=getPropertyValue("PROJECT_LOCATION")
    fullAppPath="${WORKSPACE}"+fileSeperator()+directory
    projectRootPath=fullAppPath+fileSeperator()
    if(isTrue(BlackDuck)){
        attachment="**/*_BlackDuck_RiskReport.pdf"
        archiveArtifacts artifacts: attachment
    }
    if(isTrue(CheckMarx)){
        if(attachment!=""){attachment=attachment+","}
        checkmarxReport="**/${PROJECT_LOCATION}-build/Checkmarx/Reports/Report_CxSAST.html"
        checkmarxReportPDF="**/${PROJECT_LOCATION}-build/Checkmarx/Reports/CxSASTReport*.pdf"
        checkmarxReportXML="**/${PROJECT_LOCATION}-build/Checkmarx/Reports/ScanReport.xml"
        attachment=attachment+checkmarxReportPDF
        //archiveArtifacts artifacts: checkmarxReport
        archiveArtifacts artifacts: checkmarxReportPDF
        //archiveArtifacts artifacts: checkmarxReportXML
    }

    projectlocation="**/${PROJECT_LOCATION}-build/target/site/munit/coverage"
    reportDir=projectlocation+"/summary.html"
    echo "INFO: Munit report path=${reportDir}"
    if(isTrue(munit)){
        if(attachment!=""){attachment=attachment+","}
        attachment=attachment+"${reportDir}"
        attachment=attachment+",MunitTestResult.html"
    }

    if(buildStatus){
        attachBuildLogs=false
    }else{
        attachment=""
        attachBuildLogs=true
    }
    echo "INFO: attachment=${attachment}"
    sendEmail(EMAIL_GROUP_CI,EMAIL_SUBJECT_CI,EMAIL_CONTENT_CI,EMAIL_ENABLE_CI,attachment,attachBuildLogs)

}
//Email enable
def sendEmail_CD(adapterProp,environment,Framework_Servers,cps,Framework_Applications,Adapters,ADAPTER_EXCHANGE_ASSET_VERSION,TargetExchangeMap,AdapterServer,REGRESSION_TEST,action,buildStatus){
    EMAIL_GROUP_CD=adapterProp.get("EMAIL_GROUP_CD")
    EMAIL_SUBJECT_CD=adapterProp.get("EMAIL_SUBJECT_CD")
    EMAIL_CONTENT_CD=adapterProp.get("EMAIL_CONTENT_CD")
    EMAIL_ENABLE_CD=adapterProp.get("EMAIL_ENABLE_CD")
    FITNESSE_HOST=adapterProp.get("FITNESSE_HOST")
    FITNESSE_PORT=adapterProp.get("FITNESSE_PORT")
    FITNESSE_SUITE=adapterProp.get("FITNESSE_SUITE")
    EMAIL_FITNESSE_CD =adapterProp.get("EMAIL_FITNESSE_CD")
    FITNESSE_USE_DEFAULT_BRANCH=adapterProp.get("FITNESSE_USE_DEFAULT_BRANCH")
    FITNESSE_WIKI_DEFAULT_BRANCH =adapterProp.get("FITNESSE_WIKI_DEFAULT_BRANCH")
    validAction=['status', 'reinstall', 'delete', 'license', 'redeploy', 'undeploy', '']
    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD.replace('${GIT_URL}',GIT_URL)
    BUILD_TRIGGER_BY = buildTriggeredBy()
    BUILD_DURATION= buildDuration()
    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD.replace('${BUILD_TRIGGER_BY}',BUILD_TRIGGER_BY)
    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD.replace('${BUILD_DURATION}',BUILD_DURATION)
    if(validAction.contains(action)){
        if(buildStatus){
            EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+"<br><strong>BUILD ARGUMENTS:</strong>"
            EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+"<br>environment: ${environment}"
            if(EMAIL_FITNESSE_CD =="false"){
                if(isNotNullandEmpty(infra)){
                    infra_content= "<br>infra: ${infra}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+infra_content
                }
                if(isNotNullandEmpty(Framework_Servers)){
                    Frameworkserver="<br>Framework_Servers: ${Framework_Servers}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+Frameworkserver
                }
                if(isTrue(cps)){
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+"<br>cps: ${cps}"
                }
                if(isNotNullandEmpty(Framework_Applications)){
                    Fwapplication= "<br>Framework_Applications: ${Framework_Applications}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+Fwapplication
                }
                if(isNotNullandEmpty(ADAPTER_EXCHANGE_ASSET_VERSION)){
                    ADAPTER_EXCHANGE_ASSET_VERSION= "<br>ADAPTER_EXCHANGE_ASSET_VERSION: ${ADAPTER_EXCHANGE_ASSET_VERSION}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+ADAPTER_EXCHANGE_ASSET_VERSION
                }
                if(isNotNullandEmpty(TargetExchangeMap)){
                    TargetExchangeMap= "<br>TargetExchangeMap: ${TargetExchangeMap}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+TargetExchangeMap
                }
                if(isNotNullandEmpty(ExchangeUser)){
                    ExchangeUser= "<br>ExchangeUser: ${ExchangeUser}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+ExchangeUser
                }
                if(isNotNullandEmpty(AdapterServer)){
                    AdapterServer= "<br>AdapterServer: ${AdapterServer}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+AdapterServer
                }
                if(isNotNullandEmpty(Adapter)){
                    Adapter= "<br>Adapter: ${Adapter}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+Adapter
                }
                if(isNotNullandEmpty(branch)){
                    if(FITNESSE_USE_DEFAULT_BRANCH=="true"){
                        branch= "<br>branch: ${FITNESSE_WIKI_DEFAULT_BRANCH}"
                    }
                    else{
                        branch= "<br>branch: ${branch}"
                    }
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+branch
                }
                if(isNotNullandEmpty(action)){
                    actions="<br>action: ${action}"
                    EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+actions
                }
            }
            EMAIL_ENABLE_CD=adapterProp.get("EMAIL_ENABLE_CD")
            debug("email enable cd ${EMAIL_ENABLE_CD}")
            if(REGRESSION_TEST=="true"){
                REGRESSION_TEST= "<br>REGRESSION_TEST: ${REGRESSION_TEST}"
                EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+REGRESSION_TEST
                EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+"<br> Fitnesse URL: ${FitnesseUrl} <br> Fitnesse Report: ${BUILD_URL}fitnesseReport/"
            }
        }
        else{
            EMAIL_CONTENT_CD=EMAIL_CONTENT_CD + "<br>Please find build logs attachment (or) check build console for failure Build console: ${BUILD_URL}console<br> <br>"
        }
        EMAIL_CONTENT_CD=EMAIL_CONTENT_CD+"<br><br>Thanks & Regards<br>Build Admin"
        attachment=""
        directory=getProjectLocation()
        PROJECT_LOCATION=getPropertyValue("PROJECT_LOCATION")
        fullAppPath="${WORKSPACE}"+fileSeperator()+directory
        projectRootPath=fullAppPath+fileSeperator()
        if(buildStatus){
            attachBuildLogs=false
        }else{
            attachment=""
            attachBuildLogs=true
        }
        echo "INFO: attachment=${attachment}"
        sendEmail(EMAIL_GROUP_CD,EMAIL_SUBJECT_CD,EMAIL_CONTENT_CD,EMAIL_ENABLE_CD,attachment,attachBuildLogs)
    }
    else{
        echo "INFO: This action is not eligible for email feature "
    }
}

def sendEmail_CICD(adapterProp,environment,FrameworkDefault,AdapterServer,adapterconfigpush,adapterconfigupdate,adapter,version,TargetExchange,ExchangePassword,ExchangeUser,branch,action,regresstion,buildStatus){
    EMAIL_GROUP_CICD=adapterProp.get("EMAIL_GROUP_CICD")
    EMAIL_SUBJECT_CICD=adapterProp.get("EMAIL_SUBJECT_CICD")
    EMAIL_FITNESSE_CICD=adapterProp.get("EMAIL_FITNESSE_CICD")
    EMAIL_CONTENT_CICD=adapterProp.get("EMAIL_CONTENT_CICD")
    EMAIL_ENABLE_CICD=adapterProp.get("EMAIL_ENABLE_CICD")
    FITNESSE_HOST=adapterProp.get("FITNESSE_HOST")
    FITNESSE_PORT=adapterProp.get("FITNESSE_PORT")
    FITNESSE_SUITE=adapterProp.get("FITNESSE_SUITE")
    EMAIL_FITNESSE=adapterProp.get("EMAIL_FITNESSE")
    FW_DEFAULT_SERVERS =adapterProp.get("FW_DEFAULT_SERVERS")
    EXCHANGE_API_ASSET_ID=adapterProp.get("EXCHANGE_API_ASSET_ID")
    EXCHANGE_ASSET_ID=adapterProp.get("EXCHANGE_ASSET_ID")
    FW_DEFAULT_APPLICATIONS=adapterProp.get("FW_DEFAULT_APPLICATIONS")
    EMAIL_ENABLE_CD=adapterProp.get("EMAIL_ENABLE_CD")
    EMAIL_ENABLE_CI=adapterProp.get("EMAIL_ENABLE_CI")
    validAction=['status', 'reinstall', 'delete', 'license', 'redeploy', 'undeploy', '']
    GIT_URL=adapterProp.get("GIT_URL")
    EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD.replace('${GIT_URL}',GIT_URL)
    BUILD_TRIGGER_BY = buildTriggeredBy()
    BUILD_DURATION= buildDuration()
    EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD.replace('${BUILD_TRIGGER_BY}',BUILD_TRIGGER_BY)
    EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD.replace('${BUILD_DURATION}',BUILD_DURATION)

    debug("cicd content ${EMAIL_CONTENT_CICD}")
    if(EMAIL_ENABLE_CD=="false" && EMAIL_ENABLE_CI=="false"){
        if(buildStatus){
            debug("munit=${munit}")
            debug("PublishToExchange=${PublishToExchange}")
            debug("TargetExchange=${TargetExchange}")
            debug("EXCHANGE_ASSET_ID=${EXCHANGE_ASSET_ID}")
            debug("EXCHANGE_API_ASSET_ID=${EXCHANGE_API_ASSET_ID}")
            debug("Build_Adapter= ${Build_Adapter}")
            EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+"<br><strong>CI BUILD ARGUMENTS:</strong>"
            if(isTrue(munit)){
                munitUrl="<br>MunitCoverageReport= ${BUILD_URL}AdapterMunitReport<br>"
                EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+munitUrl
            }
            if(utils.isTrue(PublishToExchange) && isNotNullandEmpty(TargetExchange)){
                ExchangeAsset="<br><br>Exchange asset URL= https://anypoint.mulesoft.com/exchange/${businessGroupId}/${EXCHANGE_ASSET_ID}/${version}<br>    TargetExchange=${TargetExchange}<br>    EXCHANGE_ASSET_ID=${EXCHANGE_ASSET_ID}<br>    EXCHANGE_ASSET_VERSION=${version}"
                EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+ExchangeAsset
            }
            if(utils.isTrue(Build_Adapter)){

                EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+"<br>Build_Adapter: ${Build_Adapter} "
            }
            if(isNotNullandEmpty(EXCHANGE_API_ASSET_ID) && utils.isTrue(PublishToExchange) && isNotNullandEmpty(TargetExchange)){
                ExchangeAsset="<br><br>Exchange API asset URL= https://anypoint.mulesoft.com/exchange/${businessGroupId}/${EXCHANGE_API_ASSET_ID}/${version}<br>   TargetExchange=${TargetExchange}<br>   EXCHANGE_API_ASSET_ID=${EXCHANGE_API_ASSET_ID}<br>   EXCHANGE_API_ASSET_VERSION=${version}"
                EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+ExchangeAsset
            }
            if(validAction.contains(action)){
                EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+"<br><br><strong>CD BUILD ARGUMENTS:</strong>"
                EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+"<br>environment: ${environment}"
                if(EMAIL_FITNESSE_CICD=="false"){
                    if(isTrue(FrameworkDefault)){
                        Frameworkserver="<br>Framework_Servers: ${FW_DEFAULT_SERVERS} <br>Framework_Applications : ${FW_DEFAULT_APPLICATIONS} <br>configpush : true <br>configupdate : true "
                        EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+Frameworkserver
                    }
                    if(isNotNullandEmpty(TargetExchange)){
                        TargetExchange= "<br>TargetExchange: ${TargetExchange}"
                        EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+TargetExchange
                    }
                    if(isNotNullandEmpty(ExchangeUser)){
                        ExchangeUser= "<br>ExchangeUser: ${ExchangeUser}"
                        EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+ExchangeUser
                    }
                    if(isTrue(AdapterServer)){
                        AdapterServer= "<br>AdapterServer: ${AdapterServer}"
                        EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+AdapterServer
                    }
                    if(isTrue(Adapter)){
                        Adapter= "<br>Adapter: ${Adapter}"
                        EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+Adapter
                    }
                    if(isNotNullandEmpty(action)){
                        actions="<br>action: ${action}"
                        EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+actions
                    }
                }
                if(isTrue(regresstion)){
                    FITNESSE_REPORT_DATA=getContentLine(cdBuildLogs,"Fitnesse Report : ")
                    debug("Get Fitnesse Report from CD logs : "+FITNESSE_REPORT_DATA)
                    REGRESSION_TEST= "<br>REGRESSION_TEST: ${regresstion}<br>Fitnesse Branch :${branch}"
                    EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+REGRESSION_TEST
                    EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+"<br> Fitnesse URL: http://${FITNESSE_HOST}:${FITNESSE_PORT}/${FITNESSE_SUITE} <br> ${FITNESSE_REPORT_DATA} "
                }
            }
            else{
                echo "INFO: This action is not eligible for CD email feature "

            }
        }
        else{
            EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD + "<br>Please find build logs attachment (or) check build console for failure Build console: ${BUILD_URL}console<br> <br>"
        }
        EMAIL_CONTENT_CICD=EMAIL_CONTENT_CICD+"<br><br>Thanks & Regards<br>Build Admin"
        attachment=""
        directory=getProjectLocation()
        PROJECT_LOCATION=getPropertyValue("PROJECT_LOCATION")
        fullAppPath="${WORKSPACE}"+fileSeperator()+directory
        projectRootPath=fullAppPath+fileSeperator()

        if(buildStatus){
            attachBuildLogs=false
        }else{
            attachment=""
            attachBuildLogs=true
        }
        echo "INFO: attachment=${attachment}"
        sendEmail(EMAIL_GROUP_CICD,EMAIL_SUBJECT_CICD,EMAIL_CONTENT_CICD,EMAIL_ENABLE_CICD,attachment,attachBuildLogs)
    }
}

def getCronParameters(adapter){
    CRON_CI=adapter.get("CRON_CI")
    if(isNotNullandEmpty(CRON_CI)){
        CRON_SETTINGS= ''+CRON_CI+''
        echo "INFO: CRON_SETTINGS=${CRON_SETTINGS}"
    }else{
        echo "INFO: Disabled cron build trigger. Setting empty for CRON_SETTINGS"
        CRON_SETTINGS=""
    }
    return CRON_SETTINGS
}
def getCronParameters(adapter,key){
    CRON_TIMER=adapter.get(key)
    if(isNotNullandEmpty(CRON_TIMER)){
        CRON_SETTINGS= ''+CRON_TIMER+''
        echo "INFO: CRON_SETTINGS=${CRON_SETTINGS}"
    }else{
        echo "INFO: Disabled cron build trigger. Setting empty for CRON_SETTINGS"
        CRON_SETTINGS=""
    }
    return CRON_SETTINGS
}


def getCDCronParameters(adapter){
    CRON_CD=adapter.get("CRON_CD")
    if(isNotNullandEmpty(CRON_CD)){
        CRON_SETTINGS= ''+CRON_CD+''
        echo "INFO: CRON_SETTINGS=${CRON_SETTINGS}"
    }else{
        echo "INFO: Disabled cron build trigger. Setting empty for CRON_SETTINGS"
        CRON_SETTINGS=""
    }
    return CRON_SETTINGS
}

def getCICDCronParameters(adapter){
    CRON_CICD=adapter.get("CRON_CICD")
    if(isNotNullandEmpty(CRON_CICD)){
        CRON_SETTINGS= ''+CRON_CICD+''
        echo "INFO: CRON_SETTINGS=${CRON_SETTINGS}"
    }else{
        echo "INFO: Disabled cron build trigger. Setting empty for CRON_SETTINGS"
        CRON_SETTINGS=""
    }
    return CRON_SETTINGS
}



def sendEmail(senders,subject,content,enable){
    sendEmail(senders,subject,content,enable,"",false)
}
def sendEmail(senders,subject,content,enable,attachment,attachBuildLogs){
    debug("enable : ${enable}")
    if (enable=="true"){
        debug("email enable in send email ${enable}")
        debug("content of the mail ${content}")
        debug("attachBuildLogs=${attachBuildLogs}")
        debug("attachment=${attachment}")
        //with build log
        if(attachBuildLogs){
            echo "INFO: Sending mail with build.log as attachment ,attachBuildLogs=${attachBuildLogs}"
            emailext (subject: subject,attachLog: attachBuildLogs ,mimeType: 'text/html', to: senders,recipientProviders: [[$class: 'RequesterRecipientProvider']],body: content)
        }
        else{
            //Attach archive files
            if(attachment!=""){
                echo "INFO: Sending mail with files attachment=${attachment}"
                emailext (subject: subject,mimeType: 'text/html',attachmentsPattern: attachment, to: senders,recipientProviders: [[$class: 'RequesterRecipientProvider']],body: content)
            }
            // Attach email without logs
            else{
                echo "INFO: Sending mail without attachment"
                emailext (subject: subject,mimeType: 'text/html', to: senders,recipientProviders: [[$class: 'RequesterRecipientProvider']],body: content)
            }
        }
    }else{
        echo "INFO: Email notification disabled with below information"
    }
    echo "INFO: To : ${senders}"
    echo "INFO: Subject:${subject}"
    echo "INFO: Content:\n${content}"
}

//Conflunce publish
def publishConfluenceReport_CI(TargetExchange,version,buildStatus){
    CONFLUENCE_ENABLE_PUBLISH_CI=getPropertyValue("CONFLUENCE_ENABLE_PUBLISH_CI")
    debug("CONFLUENCE_ENABLE_PUBLISH_CI=${CONFLUENCE_ENABLE_PUBLISH_CI}")
    debug("TargetExchange=${TargetExchange}")
    debug("version=${version}")
    debug("buildStatus=${buildStatus}")
    if(CONFLUENCE_ENABLE_PUBLISH_CI != "" && CONFLUENCE_ENABLE_PUBLISH_CI=="true"){
        CONFLUENCE_MACRO_FILE=jenkins.get("CONFLUENCE_MACRO_FILE")
        trueValue='Y'
        falseValue='N'
        EXCHANGE_ASSET_ID=getPropertyValue("EXCHANGE_ASSET_ID")
        CONFLUENCE_DOMAIN=jenkins.get("CONFLUENCE_DOMAIN")
        debug("CONFLUENCE_MACRO_FILE=${CONFLUENCE_MACRO_FILE}")
        munitData=getMunitBuildData()
        BUILD_TRIGGER_BY = buildTriggeredBy()
        CONFLUENCE_ATTACHMENT=""
        if(isTrue(BlackDuck)){
            renameFilewithPattern(workspace,"","*_BlackDuck_RiskReport.pdf","${EXCHANGE_ASSET_ID}_${BUILD_NUMBER}_BlackDuck_RiskReport.pdf")
            CONFLUENCE_ATTACHMENT="**/*_BlackDuck_RiskReport.pdf"
        }
        if(isTrue(CheckMarx)){
            if(CONFLUENCE_ATTACHMENT!=""){CONFLUENCE_ATTACHMENT=CONFLUENCE_ATTACHMENT+","}
            checkmarxReportPDF="**/${PROJECT_LOCATION}-build/Checkmarx/Reports/CxSASTReport*.pdf"
            checkmarxReportPDFPattern="**/CxSASTReport*.pdf"
            renameFilewithPattern(workspace,"${PROJECT_LOCATION}-build/Checkmarx/Reports","${PROJECT_LOCATION}-build/Checkmarx/Reports/CxSASTReport*.pdf","CxSASTReport_${EXCHANGE_ASSET_ID}_${BUILD_NUMBER}.pdf")
            CONFLUENCE_ATTACHMENT=CONFLUENCE_ATTACHMENT+checkmarxReportPDFPattern
        }

        targetExchngeText=isNotNullandEmpty(TargetExchange) ? TargetExchange :'NA'
        versionValue=isNotNullandEmpty(version) ? version :'NA'

        replaceContentAll(CONFLUENCE_MACRO_FILE,'branch',trunkText(branch,30))
        replaceContentAll(CONFLUENCE_MACRO_FILE,'mun',isTrue(munit) ? trueValue :falseValue)
        replaceContentAll(CONFLUENCE_MACRO_FILE,'bd',isTrue(BlackDuck) ? trueValue :falseValue)
        replaceContentAll(CONFLUENCE_MACRO_FILE,'cm',isTrue(CheckMarx) ? trueValue :falseValue)
        replaceContentAll(CONFLUENCE_MACRO_FILE,'pub',isTrue(PublishToExchange) ? trueValue :falseValue)
        replaceContentAll(CONFLUENCE_MACRO_FILE,'tarex',targetExchngeText)
        replaceContentAll(CONFLUENCE_MACRO_FILE,'vers',versionValue)
        replaceContentAll(CONFLUENCE_MACRO_FILE,'build-status',buildStatus ? 'Green' :'Red')
        replaceContentAll(CONFLUENCE_MACRO_FILE,'totaltest',munitData.get("Tests"))
        replaceContentAll(CONFLUENCE_MACRO_FILE,'err',munitData.get("Errors"))
        replaceContentAll(CONFLUENCE_MACRO_FILE,'skip',munitData.get("Skipped"))
        replaceContentAll(CONFLUENCE_MACRO_FILE,'fail',munitData.get("Failures"))
        replaceContentAll(CONFLUENCE_MACRO_FILE,'buildby',trunkText(BUILD_TRIGGER_BY,30))
        replaceContentAll(CONFLUENCE_MACRO_FILE,'codecover',munitData.get("munitCodeCoverage"))
        replaceContentAll(CONFLUENCE_MACRO_FILE,'timestmp',hoverText(buildDuration(),BUILD_TIMESTAMP))

        debug(readFile(CONFLUENCE_MACRO_FILE))
        publishConfluencePage(CONFLUENCE_MACRO_FILE,getPropertyValue("CONFLUENCE_SPACE_NAME_CI"),getPropertyValue("CONFLUENCE_PAGE_CI"),CONFLUENCE_DOMAIN,CONFLUENCE_ATTACHMENT)

    }else{
        echo "INFO: Confluence Publish is disabled ....!!!"
    }
}
def trunkText(value,space){
    updatedValue = ""
    if(value.length()==space || value.length()<space){
        updatedValue=value
    }
    else
    if(value.length()>space){
        updatedValue=value.substring(0,space-2)+".."
        updatedValue = hoverText(value,updatedValue)
    }
    return updatedValue
}
def hoverText(actualText,hoverTextValue){
    trunkMacro="utils/ci-utils/confluence/spanMacro.txt"
    fileContent = readFile(trunkMacro)
    if(actualText.contains("\"")){
        actualText = actualText.replaceAll("\"","'")
    }
    fileContent = fileContent.replaceAll("plaintext",actualText)
    fileContent = fileContent.replaceAll("trunktext",hoverTextValue)
    return fileContent
}
def publishConfluencePage(CONFLUENCE_MACRO_FILE,SPACE_NAME,CONFLUENCE_PAGE,CONFLUENCE_DOMAIN,CONFLUENCE_ATTACHMENT){
    debug("attachment_file=${CONFLUENCE_ATTACHMENT}")
    publishConfluence (
            buildIfUnstable: true,
            fileSet: CONFLUENCE_ATTACHMENT,
            editorList: [confluenceBeforeToken(generator: confluenceFile(CONFLUENCE_MACRO_FILE), markerToken: '</tbody></table>')],
            labels: ',',
            pageName: CONFLUENCE_PAGE,
            siteName: CONFLUENCE_DOMAIN,
            spaceName: SPACE_NAME
            )
}

// PYTHON SERVICE
def neoAdapterBuild(){

    PYTHON_HOME="C:\\python"

    scriptCall("cd ${workspace}/adapter && py -m pip install -e.")
    //bat "cd ${workspace}/adapter&&python -m pip install -e."
    scriptCall("cd ${workspace}/adapter/pyadapter && py -m pip install pyinstaller")
    scriptCall("cd  ${workspace}/adapter/pyadapter && ${PYTHON_HOME}\\Scripts\\pyinstaller -p pyadapter launch.py")
    //bat "${workspace}/utils/scripts.bat"
    bat "xcopy  ${workspace}\\adapter\\pyadapter\\config_repo  ${workspace}\\adapter\\pyadapter\\dist\\launch\\config_repo /e /i /h"
    scriptCall("mkdir ${workspace}\\adapter\\pyadapter\\dist\\launch\\connect")
    scriptCall("mkdir ${workspace}\\adapter\\pyadapter\\dist\\launch\\connect\\cps")
    scriptCall("copy ${workspace}\\adapter\\pyadapter\\connect\\cps\\adapter-cps.json ${workspace}\\adapter\\pyadapter\\dist\\launch\\connect\\cps\\")
    scriptCall("mkdir ${workspace}\\adapter\\pyadapter\\dist\\launch\\log")
    scriptCall("mkdir ${workspace}\\adapter\\pyadapter\\dist\\launch\\log\\archived")
    scriptCall("copy ${workspace}\\adapter\\log-config.json ${workspace}\\adapter\\pyadapter\\dist\\launch")
    scriptCall("mkdir ${workspace}\\adapter\\pyadapter\\dist\\launch\\confluent_kafka.libs")
    //wincopyFolder("${PYTHON_HOME}\\Lib\\site-packages\\confluent_kafka.libs","${workspace}\\adapter\\pyadapter\\dist\\launch\\confluent_kafka.libs")
    scriptCall("copy ${PYTHON_HOME}\\Lib\\site-packages\\confluent_kafka.libs ${workspace}\\adapter\\pyadapter\\dist\\launch\\confluent_kafka.libs")
    scriptCall("robocopy ${workspace}\\adapter\\pyadapter\\dist\\launch  ${workspace}\\adapter\\pyadapter\\dist\\pyadapter /E /MOVE & IF %ERRORLEVEL% LEQ 4 exit /B 0")
    echo "BUILD COMPLETED"


}
def createArtifact(version){

    echo "Zipping Files..."
    ZIP_HOME="C:\\zip\\7z.exe"

    //zip zipFile: "connect-neoservice-${version}.zip", dir:"${workspace}/adapter\\pyadapter\\dist\\pyadapter" , archive: true , overwrite: true
    fileOperations([fileDeleteOperation(includes: '*.zip')])
    bat "${ZIP_HOME} a -tzip ${workspace}/connect-neoservice-${version}.zip ${workspace}/adapter\\pyadapter\\dist\\pyadapter\\*"
    neofile="connect-neoservice-${version}.zip"
    archiveArtifacts artifacts: neofile
}
def PublishArtifact(adapter,version,jpath){
    echo "Publishing artifact to JFROG..."

    def JFROG_INSTANCE_ID=adapter.get('JFROG_INSTANCE_ID')

    def server = Artifactory.server JFROG_INSTANCE_ID
    def uploadSpec = """    {
                    "files": [
                    {
                    "pattern": "${workspace}/connect-neoservice-${version}.zip",
                    "target": "${jpath}/${version}/"
                    }
                    ]
                    }"""
    server.upload spec: uploadSpec  

}
def sendEmail_CI_NEO(adapterProp,buildStatus,version,jpath){
    EMAIL_GROUP_CI=adapterProp.get("EMAIL_GROUP_CI")
    EMAIL_SUBJECT_CI=adapterProp.get("EMAIL_SUBJECT_CI")
    EMAIL_CONTENT_CI=adapterProp.get("EMAIL_CONTENT_CI")
    EMAIL_ENABLE_CI=adapterProp.get("EMAIL_ENABLE_CI")
    GIT_URL=adapterProp.get("GIT_URL")
    ARTIFACT_URL="${BUILD_URL}/artifact/connect-neoservice-${version}.zip"
    PYTEST_REPORT="${BUILD_URL}/NeoPytestReport/"
    debug("buildStatus=${buildStatus}")
    EMAIL_CONTENT_CI=EMAIL_CONTENT_CI.replace('${GIT_URL}',GIT_URL)
    attachment=""
    if(isTrue(BlackDuck)){
       attachment="**/*_BlackDuck_RiskReport.pdf"
       archiveArtifacts artifacts: attachment
    }
    if(isTrue(CheckMarx)){
        if(attachment!=""){attachment=attachment+","}
        checkmarxReport="**/${PROJECT_LOCATION}-build/Checkmarx/Reports/Report_CxSAST.html"
        checkmarxReportPDF="**/${PROJECT_LOCATION}-build/Checkmarx/Reports/CxSASTReport*.pdf"
        checkmarxReportXML="**/${PROJECT_LOCATION}-build/Checkmarx/Reports/ScanReport.xml"
        attachment=attachment+checkmarxReportPDF
        archiveArtifacts artifacts: checkmarxReportPDF
    }
    if(buildStatus){
       EMAIL_CONTENT_CI=EMAIL_CONTENT_CI + "<br>Please find build logs attachment (or) check build console for failure <br> Build console: ${BUILD_URL}console<br> <br> ARTIFACT URL : ${ARTIFACT_URL}<br>"
    }
    if(utils.isTrue(PublishArtifact) && buildStatus){
       EMAIL_CONTENT_CI=EMAIL_CONTENT_CI +" <br> JFROG_URL : https://jdasoftware.jfrog.io/artifactory/${jpath}/${version}/connect-neoservice-${version}.zip <br> "
    }
    if(utils.isTrue(PyTest) && buildStatus){
       EMAIL_CONTENT_CI=EMAIL_CONTENT_CI +" <br> PyTest-Report : ${PYTEST_REPORT} <br> "
    }


    EMAIL_CONTENT_CI=EMAIL_CONTENT_CI+"<br><br> Thanks & Regards <br>Build Admin"
    directory=getProjectLocation()
    PROJECT_LOCATION=getPropertyValue("PROJECT_LOCATION")
    fullAppPath="${WORKSPACE}"+fileSeperator()+directory
    projectRootPath=fullAppPath+fileSeperator()

    if(buildStatus){
        attachBuildLogs=false
    }else{
        attachment=""
        attachBuildLogs=true
    }
    echo "INFO: attachment=${attachment}"
    sendEmail(EMAIL_GROUP_CI,EMAIL_SUBJECT_CI,EMAIL_CONTENT_CI,EMAIL_ENABLE_CI,attachment,attachBuildLogs)
}
def pytest(){
    echo "Running Pytest...."
    SKIP_FAILURE_ON_PYTEST=getPropertyValue("SKIP_FAILURE_ON_PYTEST")
    scriptCall("cd ${workspace}\\adapter\\ && py -m pip install -r test-requirements.in")

    if(SKIP_FAILURE_ON_PYTEST!="" && SKIP_FAILURE_ON_PYTEST=="true"){
        scriptCall("cd ${workspace}\\adapter\\ && py.test & EXIT /B 0")
    }else{
        scriptCall("cd ${workspace}\\adapter\\ && py.test")
    }
    //scriptCall("cd ${workspace}\\adapter\\ && py.test & EXIT /B 0")

    testLocation = "${workspace}\\adapter\\Neo Test Coverage\\"
    artifactLocation = "${workspace}\\adapter\\Neo Test Coverage\\index.html"
    echo "${testLocation}"
    
    publishHTMLReport(testLocation,"index.html","NeoPytestReport")
    //archiveBuildArtifacts("adapter/Neo Test Coverage/index.html")

}

return this
