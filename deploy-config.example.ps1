# ============================================
# Dedicated Server Deployment Settings
#
# Copy this file to:
#
# deploy-config.ps1
#
# Then replace the example values with your
# own Linux server configuration.
# ============================================

@{
    ServerUser = "YOUR_LINUX_USERNAME"

    ServerHost = "YOUR_SERVER_ADDRESS"

    SshKey = "$env:USERPROFILE\.ssh\chess-like-server-deploy"

    RemoteTempJar = "/tmp/chess-like-server.jar"

    RemoteServerJar = "/opt/chess-like-server/chess-like-server.jar"

    ServerService = "chess-like-server.service"
}