# Start Appium Server (assuming it's installed via npm)
Write-Host "Starting Appium server in background..."
Start-Process -FilePath "appium" -NoNewWindow -PassThru

# Wait a few seconds for it to start
Start-Sleep -Seconds 5

Write-Host "Make sure your emulator is running!"
Write-Host "To run the tests, install Maven (https://maven.apache.org/download.cgi) and run:"
Write-Host "mvn clean test"
