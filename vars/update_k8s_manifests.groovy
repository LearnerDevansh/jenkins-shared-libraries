#!/usr/bin/env groovy
/**
 * Jenkins Shared Library Step
 * Updates Kubernetes manifests with a new Docker image tag and pushes the change to GitHub.
 * Handles credentials securely and gracefully skips if no changes are detected.
 */
def call(Map config = [:]) {
    // --- Required Inputs ---
    def imageTag       = config.imageTag        ?: error("❌ Image tag is required")
    def manifestsPath  = config.manifestsPath   ?: 'kubernetes'
    def gitCredentials = config.gitCredentials  ?: 'github-creds'
    def gitUserName    = config.gitUserName     ?: 'Jenkins CI'
    def gitUserEmail   = config.gitUserEmail    ?: 'jenkins@example.com'
    def gitBranch      = env.GIT_BRANCH         ?: 'master'
    def repoUrl        = "https://github.com/LearnerDevansh/tws-e-commerce-app_hackathon.git"

    echo "🔧 Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {

        sh """
            set -e
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            # Update main app deployment
            sed -i "s|image: devanshpandey21/easyshop-app:.*|image: laxg66/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: devanshpandey21/easyshop-migration:.*|image: laxg66/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Update ingress if present
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Check for any differences before committing
            if git diff --quiet; then
                echo "✅ No changes detected in manifests. Skipping commit."
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                
                # Securely update origin and push
                git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${GIT_USERNAME}/tws-e-commerce-app_hackathon.git
                git push origin HEAD:${gitBranch}
                echo "🚀 Kubernetes manifests updated and pushed to ${gitBranch}"
            fi
        """
    }
}
