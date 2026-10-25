package com.emergencyroutefinder.tests;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class EmergencyAppUITest {

    private AndroidDriver driver;
    private WebDriverWait wait;

    @BeforeMethod
    public void setUp() throws MalformedURLException {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        // Replace with your emulator/device name
        options.setDeviceName("emulator-5554");
        // Path to the generated APK
        options.setApp("C:\\Users\\ajana\\Downloads\\ch\\android\\app\\build\\outputs\\apk\\debug\\app-debug.apk");
        options.setAutomationName("UiAutomator2");
        // We do not reset the app state to allow persisting logins during the suite if needed, 
        // but for clean tests, fullReset could be set to true.
        options.setNoReset(false); 

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @Test
    public void testUserLoginFlow() {
        // Find Email Input
        WebElement etEmail = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("com.emergencyroutefinder:id/et_email")));
        etEmail.sendKeys("testuser@example.com");

        // Find Password Input
        WebElement etPassword = driver.findElement(By.id("com.emergencyroutefinder:id/et_password"));
        etPassword.sendKeys("password123");

        // Click Login Button
        WebElement btnLogin = driver.findElement(By.id("com.emergencyroutefinder:id/btn_login"));
        btnLogin.click();

        // Verify successful login by checking if User Dashboard appears
        WebElement requestEmergencyBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("com.emergencyroutefinder:id/btn_request_emergency")));
        Assert.assertTrue(requestEmergencyBtn.isDisplayed(), "User Dashboard did not load properly after login.");
    }

    @Test
    public void testRequestEmergencyFlow() {
        // Assume user is already logged in (or we can re-login here)
        testUserLoginFlow();

        // Click Request Emergency Button
        WebElement btnRequest = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("com.emergencyroutefinder:id/btn_request_emergency")));
        btnRequest.click();

        // Ensure we reached Request Screen by waiting for the description field
        WebElement etDescription = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("com.emergencyroutefinder:id/et_description")));
        etDescription.sendKeys("Appium UI Test Emergency Request");

        // Ensure GPS fix or use manual location fallback (we mock manual for tests if GPS is disabled on emulator)
        WebElement rbManual = driver.findElement(By.id("com.emergencyroutefinder:id/rb_manual"));
        rbManual.click();

        WebElement etAddress = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("com.emergencyroutefinder:id/et_manual_address")));
        etAddress.sendKeys("Chennai, India");

        WebElement btnResolve = driver.findElement(By.id("com.emergencyroutefinder:id/btn_resolve_address"));
        btnResolve.click();

        // Wait for resolve text to show success (checking color or text is complex in Appium, we wait for a bit)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {}

        // Submit Emergency
        WebElement btnSubmit = driver.findElement(By.id("com.emergencyroutefinder:id/btn_submit_emergency"));
        btnSubmit.click();

        // Verify transition to LiveResponseMapActivity by checking ETA or Status text
        WebElement tvStatus = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("com.emergencyroutefinder:id/tv_status")));
        Assert.assertTrue(tvStatus.getText().contains("Locating") || tvStatus.getText().contains("Responder"), 
                "Live Response Map did not display correctly.");
    }

    @Test
    public void testWorkerToggleOnlineStatus() {
        // Login as worker for this test (Assumes we start at Login screen and have a worker account)
        WebElement etEmail = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("com.emergencyroutefinder:id/et_email")));
        etEmail.sendKeys("testworker@example.com");

        WebElement etPassword = driver.findElement(By.id("com.emergencyroutefinder:id/et_password"));
        etPassword.sendKeys("worker123");

        driver.findElement(By.id("com.emergencyroutefinder:id/btn_login")).click();

        // Wait for worker dashboard
        WebElement btnToggle = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("com.emergencyroutefinder:id/btn_toggle_availability")));
        
        String initialText = btnToggle.getText();
        Assert.assertTrue(initialText.contains("GO ONLINE") || initialText.contains("GO OFFLINE"));

        // Click toggle
        btnToggle.click();

        // Wait for text change
        try { Thread.sleep(2000); } catch (Exception ignored) {}

        String newText = driver.findElement(By.id("com.emergencyroutefinder:id/btn_toggle_availability")).getText();
        Assert.assertNotEquals(initialText, newText, "Worker availability toggle failed to update the UI.");
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
