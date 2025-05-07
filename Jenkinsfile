pipeline {
    agent {
        label 'development-server'
    }

    options {
        // Clean before build
        skipDefaultCheckout(true)
    }

    tools {
        maven '3.9.9'
    }

    environment {
        USERNAME = "tienminhktvn2"
        CUSTOMERS_IMAGE_TAG = "latest"
        VETS_IMAGE_TAG      = "latest"
        VISITS_IMAGE_TAG    = "latest"
        GENAI_IMAGE_TAG     = "latest"
    }

    stages {
        stage('Check SCM') {
            steps {
                cleanWs()

                checkout scm

                script {
                    // Get the first 8 characters of the SHA Git Commit
                    def gitCommitHash = sh(script: "git rev-parse --short=8 HEAD", returnStdout: true).trim()
                    env.COMMIT_HASH = gitCommitHash
                }
            }
        }

        stage('Check Changed Files') {
            steps {
                script {
                    def branch_name = ""

                    if (env.CHANGE_ID) {
                        branch_name = "${env.CHANGE_TARGET}"

                        // Fetch main branch if it is a Pull Request
                        sh("git fetch origin ${branch_name}:${branch_name} --no-tags")
                    }
                    else {
                        branch_name = 'HEAD~1'
                    }

                    def changedFiles = sh(script: "git diff --name-only ${branch_name}", returnStdout: true).trim()

                    echo "${changedFiles}"

                    def folderList = ['spring-petclinic-customers-service', 'spring-petclinic-vets-service', 'spring-petclinic-visits-service']
                    
                    def changedFolders = changedFiles.split('\n')
                        .collect { it.split('/')[0] }
                        .unique()
                        .findAll { folderList.contains(it) }
                    
                    echo "Changed Folders: \n${changedFolders.join('\n')}"
                    
                    env.CHANGED_MODULES = changedFolders.join(',')

                    if (changedFolders.contains('spring-petclinic-customers-service')) {
                        env.CUSTOMERS_IMAGE_TAG = env.COMMIT_HASH
                    }
                    if (changedFolders.contains('spring-petclinic-vets-service')) {
                        env.VETS_IMAGE_TAG = env.COMMIT_HASH
                    }
                    if (changedFolders.contains('spring-petclinic-visits-service')) {
                        env.VISITS_IMAGE_TAG = env.COMMIT_HASH
                    }
                }
            }
        }  

        stage('Run Unit Test') {
            when {
                expression { env.CHANGED_MODULES?.trim() }
            }
            steps {
                script {
                    def modules = env.CHANGED_MODULES ? env.CHANGED_MODULES.split(',') : []

                    for (module in modules) {
                        def testCommand = "mvn test -pl ${module}"
                        echo "Running tests for affected modules: ${module}"
                        sh "${testCommand}"

                        // Generate JaCoCo HTML Report
                        jacoco classPattern: "**/${module}/target/classes", 
                            execPattern: "**/${module}/target/coverage-reports/jacoco.exec",
                            runAlways: true, 
                            sourcePattern: "**/${module}/src/main/java"

                        // Pushish HTML Artifact of Code Coverage Report
                        publishHTML(
                            target: [
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: "${module}/target/site/jacoco",
                                reportFiles: 'index.html',
                                reportName: "${module}_code_coverage_report_${env.COMMIT_HASH}_v${env.BUILD_ID}"
                            ]
                        )

                        // Get Code Coverage
                        def codeCoverages = []
                        def coverageReport = readFile(file: "${WORKSPACE}/${module}/target/site/jacoco/index.html")
                        def matcher = coverageReport =~ /<tfoot>(.*?)<\/tfoot>/
                        if (matcher.find()) {
                            def coverage = matcher[0]
                            def instructionMatcher = coverage =~ /<td class="ctr2">(.*?)%<\/td>/
                            if (instructionMatcher.find()) {
                                def coveragePercentage = instructionMatcher[0][1]
                                echo "Overall code coverage of ${module}: ${coveragePercentage}%"
                                
                                codeCoverages.add(coveragePercentage)
                            }
                        }

                        env.CODE_COVERAGES = codeCoverages.join(',')
                    }
                }
            }
        }

        stage('Maven Build') {
            steps {
                script {
                    boolean testSuccess = true
                    boolean buildSuccess = true

                    def reports = env.CODE_COVERAGES ? env.CODE_COVERAGES.split(',') : []

                    if (env.CHANGE_ID && env.CHANGE_TARGET == 'main') {
                        for (codeCoverage in reports) {
                            if (codeCoverage.toDouble() < 70) {
                                testSuccess = false           

                                break
                            }
                        }
                    }
                    
                    def modules = env.CHANGED_MODULES ? env.CHANGED_MODULES.split(',') : []
                    if (testSuccess && modules.size() > 0) {
                        
                        for (module in modules) {
                            def buildCommand = "mvn -pl ${module} -am clean install -DskipTests"
                            echo "Build for affected modules: ${module}"
                            sh "${buildCommand}"
                        }

                        try {
                            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true, allowEmptyArchive: false
                        }
                        catch (Exception e) {
                            echo "No artifacts found to archive. Skipping artifact archival."
                            buildSuccess = false
                        }
                    }

                    if (testSuccess && buildSuccess && env.CHANGE_TARGET == 'main') {
                        publishChecks(
                            name: 'Test Code Coverage',
                            title: 'Code Coverage Check Success!',
                            summary: "All test code coverage is greater than 70%",
                            text: 'Check Success!',
                            detailsURL: env.BUILD_URL,
                            conclusion: 'SUCCESS'
                        )
                    }
                    else if (env.CHANGE_TARGET == 'main') {

                        publishChecks(
                            name: 'Test Code Coverage',
                            title: 'Code Coverage Check Failed',
                            summary: "Coverage must be at least 70%. Your coverage for one of modules is less then 70%.",
                            text: 'Increase test coverage and retry the build.',
                            detailsURL: env.BUILD_URL,
                            conclusion: 'FAILURE'
                        )
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    def modules = env.CHANGED_MODULES ? env.CHANGED_MODULES.split(',') : []
                    if (modules.size() > 0) {

                        // Build and Tag Images for changed modules
                        for (module in modules) {
                            def buildImagesCommand = "./mvnw clean install -pl ${module} -PbuildDocker -DskipTests"
                            echo "Build Images for affected modules: ${module}"
                            sh "${buildImagesCommand}"
                            sh "docker tag springcommunity/${module}:latest ${USERNAME}/${module}:${env.COMMIT_HASH}"
                        }
                    }
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                script {
                    def modules = env.CHANGED_MODULES ? env.CHANGED_MODULES.split(',') : []
                    if (modules.size() > 0) {
                        withCredentials([usernamePassword(
                            credentialsId: 'DOCKER_HUB_CREDENTIALS',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )]) {
                            sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"
                            for (module in modules) {
                                def imageName = "${USERNAME}/${module}:${env.COMMIT_HASH}"
                                echo "Pushing Docker image: ${imageName}"
                                sh "docker push ${imageName}"
                            }
                        }
                    } 
                    else {
                        echo "No changed modules; skipping Docker push."
                    }
                }
            }
        }

        stage('Trigger Developer Build Job') {
            steps {
                build job: 'developer_build', 
                    parameters: [
                    string(name: 'CUSTOMERS_IMAGE_TAG', value: env.CUSTOMERS_IMAGE_TAG),
                    string(name: 'VETS_IMAGE_TAG',      value: env.VETS_IMAGE_TAG),
                    string(name: 'VISITS_IMAGE_TAG',    value: env.VISITS_IMAGE_TAG),
                    string(name: 'GENAI_IMAGE_TAG',     value: env.GENAI_IMAGE_TAG)
                    ],
                    wait: false
            }
        }
    }
    
    post {
        always {
            echo 'Logging out of Docker Hub'
            sh 'docker logout'

            echo 'Cleaning up all Docker images…'
            sh 'docker image prune -af'
        }
    }
}