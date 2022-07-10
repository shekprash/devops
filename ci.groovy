node {
    checkout scm
    adapter_file=new File(currentBuild.rawBuild.parent.definition.scriptPath).getParent()+"/py-service.properties"
    adapter = readProperties file: adapter_file
    if(adapter.get('BUILD_TEMPLATE_GIT_URL')!=null && adapter.get('BUILD_TEMPLATE_GIT_URL')!=""){
        BUILD_TEMPLATE_GIT_BRANCH=adapter.get('BUILD_TEMPLATE_GIT_BRANCH')
        BUILD_TEMPLATE_GIT_URL=adapter.get('BUILD_TEMPLATE_GIT_URL')
        println "INFO: Checkout code from BUILD_TEMPLATE_GIT_URL=${BUILD_TEMPLATE_GIT_URL} BUILD_TEMPLATE_GIT_BRANCH:${BUILD_TEMPLATE_GIT_BRANCH}"
        checkout([$class: 'GitSCM',branches: [[name: adapter.get('BUILD_TEMPLATE_GIT_BRANCH')]],doGenerateSubmoduleConfigurations: false,extensions: [[$class: 'RelativeTargetDirectory',relativeTargetDir: 'template'], [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true]],gitTool: 'Default',submoduleCfg: [],userRemoteConfigs: [[credentialsId: adapter.get('BUILD_TEMPLATE_GIT_CREDENTIAL_ID'),url: adapter.get('BUILD_TEMPLATE_GIT_URL')]]])
        utilsPath="template/utils/Py-Utils"
    }else{
        utilsPath="utils/Py-Utils/"
    }
    jenkins = readProperties file: utilsPath+'jenkins.properties'
    utils = load utilsPath+'PyBuildUtils.groovy'
    utils.setPropertyFile(adapter)
    //currentBuild.displayName = BUILD_NUMBER+":"+version+" "+TargetExchange
}
def GIT_URL=adapter.get('GIT_URL')
def DEFAULT_BRANCH=adapter.get('DEFAULT_BRANCH')
def DEFAULT_VERSION=adapter.get('DEFAULT_VERSION')
def jpath=adapter.get('JFROG_ARTIFACT_PATH')


webhookToken=utils.getAdapterWebhooktoken(adapter,"CI")
CRON_SETTINGS = utils.getCronParameters(adapter)
pipeline {
    agent { label adapter.get("BUILD_AGENT_CI") }

    options {
        disableConcurrentBuilds()
        timestamps ()
    }
    parameters {
        gitParameter name: 'branch' ,branch: '' ,branchFilter: 'origin/(.*)' ,quickFilterEnabled: true ,defaultValue: DEFAULT_BRANCH ,type: 'PT_BRANCH',selectedValue: 'NONE',sortMode: 'NONE',description: "Branch name,Default branch :${DEFAULT_BRANCH}",useRepository: GIT_URL
        booleanParam(name: 'Build', defaultValue: true, description: 'Build python code, Mandatory with selection of below sections')
        booleanParam(name: 'PyTest',defaultValue: false, description: '')
        booleanParam(name: 'BlackDuck',defaultValue: false, description: '')
        booleanParam(name: 'CheckMarx',defaultValue: false, description: '')
        booleanParam(name: 'PublishArtifact',defaultValue: false, description: 'PUBLISH ARTIFACT')
        string(name: 'version', defaultValue: DEFAULT_VERSION, description: 'Version to push into Repository', trim: true)
        //string(name: 'jpath', defaultValue: DEFAULT_JPATH, description: 'Target JFROG path to push the asset', trim: true)
    }
    triggers {
        parameterizedCron(CRON_SETTINGS)
        GenericTrigger(causeString: 'GenericTrigger:Adapter CI build', genericVariables: [
            [key: 'body', defaultValue: '', value: '$'],
            [key: 'branch', defaultValue: DEFAULT_BRANCH, value: '$.branch'],
            [key: 'Build_Adapter', defaultValue: "true", value: '$.Build_Adapter'],
            [key: 'munit', defaultValue: "true", value: '$.munit'],
            [key: 'BlackDuck', defaultValue: "false", value: '$.BlackDuck'],
            [key: 'CheckMarx', defaultValue: "false", value: '$.CheckMarx'],
            [key: 'PublishToJFrog', defaultValue: "false", value: '$.PublishToJFrog'],

        ], printPostContent: true, printContributedVariables: true, tokenCredentialId: webhookToken)
    }
    stages {
        stage("Checkout-ArtifactUpdate") {
            steps {
                script {
                    utils.checkoutProject(adapter,branch)
                    utils.checkoutTemplate(utils,adapter,jenkins)
                }
            }
        }
        stage('Build') {
            when {expression { utils.isTrue(Build) }}
            steps {
                script {
                    utils.neoAdapterBuild()
                    //utils.createArtifact(version)
                }
            }
        }
        stage('Unit-Test') {
            when {expression { utils.isTrue(PyTest) }}
            steps {
                script {
                    utils.pytest()
                }
            }
        }
        stage('Security Scan') {
            when {expression { utils.trueCheck(BlackDuck,CheckMarx) }}
            steps {
                script {
                    if(utils.isTrue(Build)){
                        utils.runSecurityScan(adapter,version,BlackDuck,CheckMarx)
                    }
                }
            }
        }

        stage('PublishArtifact') {
            when {expression {  utils.trueCheck(PublishArtifact) }}
            steps {
                script {
                    if(utils.isTrue(Build)){
                        utils.PublishArtifact(adapter,version,jpath)
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                if(utils.isTrue(Build)){
                    utils.sendEmail_CI_NEO(adapter,true,version,jpath)
                    //utils.publishConfluenceReport_CI(TargetExchange,version,true)
                }
                echo "Build success !!!"
            }
        }
        failure {
            script {
                if(utils.isTrue(Build)){
                    utils.sendEmail_CI_NEO(adapter,false,version,jpath)
                    //utils.publishConfluenceReport_CI(TargetExchange,version,false)
                }
                echo "Build failure !!!"
            }
        }
        always {
            script {
                echo "Completed...!!!"
            }
        }
    }
}
