#!/usr/bin/env groovy

/**
 * Securely updates Kubernetes manifests with new image tags and pushes changes to GitHub.
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def gitBranch = config.gitBranch ?: 'master'

    echo "🔧 Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {

        sh '''
            set -e

            # Configure Git identity
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            # Update Kubernetes manifests
            sed -i "s|image: devanshpandey21/easyshop-app:.*|image: laxg66/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: devanshpandey21/easyshop-migration:.*|image: laxg66/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Prepare Git credentials temporarily
            git config credential.helper store
            printf "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com\n" > ~/.git-credentials

            # Add and commit changes (only if there are any)
            if git diff --quiet; then
                echo "✅ No changes detected in Kubernetes manifests."
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]" || echo "Nothing to commit"

                echo "🚀 Pushing changes to GitHub branch: ${gitBranch}"
                git push origin HEAD:${gitBranch}
            fi
        '''
    }
}
