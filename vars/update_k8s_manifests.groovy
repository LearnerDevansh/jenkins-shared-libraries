#!/usr/bin/env groovy

/**
 * Securely updates Kubernetes manifests with new image tags and pushes changes to GitHub.
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-creds'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def gitBranch = config.gitBranch ?: 'master'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {

        sh """
            set -e
            echo "Workspace content:"
            ls -R

            # Configure Git identity
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            # Ensure the manifests path exists
            if [ ! -d "${manifestsPath}" ]; then
                echo "Error: manifestsPath '${manifestsPath}' does not exist!"
                exit 1
            fi

            # Update main app deployment
            if [ -f "${manifestsPath}/08-easyshop-deployment.yaml" ]; then
                sed -i "s|image: devanshpandey21/easyshop-app:.*|image: devanshpandey21/easyshop-app:${imageTag}|g" "${manifestsPath}/08-easyshop-deployment.yaml"
            else
                echo "Warning: ${manifestsPath}/08-easyshop-deployment.yaml not found"
            fi

            # Update migration job
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: devanshpandey21/easyshop-migration:.*|image: devanshpandey21/easyshop-migration:${imageTag}|g" "${manifestsPath}/12-migration-job.yaml"
            else
                echo "Warning: ${manifestsPath}/12-migration-job.yaml not found"
            fi

            # Update ingress
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" "${manifestsPath}/10-ingress.yaml"
            else
                echo "Warning: ${manifestsPath}/10-ingress.yaml not found"
            fi

            # Setup temporary Git credentials
            git config credential.helper store
            printf "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com\\n" > ~/.git-credentials

            # Commit changes if there are any
            git add ${manifestsPath}/*.yaml || true
            if git diff --cached --quiet; then
                echo "No changes detected in Kubernetes manifests."
            else
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]" || echo "Nothing to commit"
                echo "Pushing changes to branch: ${gitBranch}"
                git push origin HEAD:${gitBranch}
            fi
        """
    }
}
