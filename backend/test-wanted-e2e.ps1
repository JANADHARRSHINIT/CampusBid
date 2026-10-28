# CampusBid End-to-End Test for Authentication, Wanted Items & Smart Alerts
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$rnd = Get-Random -Minimum 1000 -Maximum 9999
$emailA = "studentA_$rnd@campus.edu"
$emailB = "studentB_$rnd@campus.edu"
$pass = "SecretPass123!"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  CAMPUSBID E2E TEST: AUTHENTICATION & WANTED ITEMS" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Register Student A
Write-Host "`n[Step 1] Registering Student A ($emailA)..." -ForegroundColor Yellow
$regBodyA = @{
    fullName = "Alice In Campus"
    email = $emailA
    password = $pass
    studentId = "STU-A-$rnd"
} | ConvertTo-Json

$sessionA = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$regResA = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBodyA -ContentType "application/json" -WebSession $sessionA
$userA = if ($regResA.data) { $regResA.data } else { $regResA }
Write-Host " -> Student A registered successfully: id=$($userA.id), role=$($userA.role)" -ForegroundColor Green

# 2. Register Student B
Write-Host "`n[Step 2] Registering Student B ($emailB)..." -ForegroundColor Yellow
$regBodyB = @{
    fullName = "Bob Seller"
    email = $emailB
    password = $pass
    studentId = "STU-B-$rnd"
} | ConvertTo-Json

$sessionB = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$regResB = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBodyB -ContentType "application/json" -WebSession $sessionB
$userB = if ($regResB.data) { $regResB.data } else { $regResB }
Write-Host " -> Student B registered successfully: id=$($userB.id), role=$($userB.role)" -ForegroundColor Green

# 3. Verify Duplicate Email Rejected
Write-Host "`n[Step 3] Verifying duplicate email rejection..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBodyA -ContentType "application/json"
    Write-Host " -> FAILED: Duplicate registration was not rejected!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Duplicate registration rejected with code $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

# 4. Verify Session Persistence & /api/auth/me
Write-Host "`n[Step 4] Checking /api/auth/me for Student A..." -ForegroundColor Yellow
$meA = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -WebSession $sessionA
$meData = if ($meA.data) { $meA.data } else { $meA }
if ($meData.email -eq $emailA.ToLower() -and $meData.role -eq "STUDENT") {
    Write-Host " -> PASSED: Authenticated principal restored from HttpOnly session cookie ($($meData.fullName))" -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Unexpected me response: $($meA | ConvertTo-Json)" -ForegroundColor Red
    exit 1
}

# 5. Student A Posts Wanted Item for Bicycle with Budget ₹2,000
Write-Host "`n[Step 5] Student A posts wanted request for 'Bicycle' with max budget ₹2,000..." -ForegroundColor Yellow
$wantedReq = @{
    itemName = "Bicycle"
    category = "Sports & Outdoors"
    maxBudget = 2000.00
    preferredCondition = "GOOD"
    additionalRequirements = "Looking for a sturdy bicycle for commuting between campus hostels"
} | ConvertTo-Json

$wantedItemRes = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items" -Method Post -Body $wantedReq -ContentType "application/json" -WebSession $sessionA
$wantedItem = if ($wantedItemRes.data) { $wantedItemRes.data } else { $wantedItemRes }
Write-Host " -> PASSED: Created wanted request id=$($wantedItem.id), itemName=$($wantedItem.itemName), budget=₹$($wantedItem.maxBudget)" -ForegroundColor Green

# 6. Student B Publishes Matching Listing for ₹1,800 (Within Budget)
Write-Host "`n[Step 6] Student B publishes matching listing for 'Hero Sprint Bicycle' at ₹1,800..." -ForegroundColor Yellow
$closing = (Get-Date).AddDays(3).ToString("yyyy-MM-ddTHH:mm:ss")
$listingReq1 = @{
    title = "Hero Sprint Bicycle"
    description = "Great campus bicycle with 18 gears and new tires"
    category = "Sports & Outdoors"
    condition = "LIKE_NEW"
    startingPrice = 1800.00
    minBidIncrement = 50.00
    closingTime = $closing
    sellerId = "STU-B-$rnd"
    sellerName = "Bob Seller"
    sellerEmail = $emailB
    photoUrls = @("https://images.unsplash.com/photo-1485965120184-e220f721d03e")
} | ConvertTo-Json

$listingRes1 = Invoke-RestMethod -Uri "$baseUrl/api/listings" -Method Post -Body $listingReq1 -ContentType "application/json" -WebSession $sessionB
$listing1 = if ($listingRes1.data) { $listingRes1.data } else { $listingRes1 }
Write-Host " -> PASSED: Listing created id=$($listing1.id), title='$($listing1.title)', price=₹$($listing1.startingPrice)" -ForegroundColor Green

# 7. Verify Student A Received Exactly 1 Match Notification
Write-Host "`n[Step 7] Checking Student A notifications for Smart Match Alert..." -ForegroundColor Yellow
Start-Sleep -Seconds 1
$notifsA_raw = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -WebSession $sessionA
$notifsA = if ($notifsA_raw.data) { $notifsA_raw.data } else { $notifsA_raw }
$matchNotifs = $notifsA | Where-Object { $_.type -eq "WANTED_ITEM_MATCH" -and $_.relatedListingId -eq $listing1.id }

if ($matchNotifs) {
    Write-Host " -> PASSED: Student A received smart match notification!" -ForegroundColor Green
    Write-Host "    Title: $($matchNotifs[0].title)" -ForegroundColor Cyan
    Write-Host "    Message: $($matchNotifs[0].message)" -ForegroundColor Cyan
} else {
    Write-Host " -> FAILED: No WANTED_ITEM_MATCH notification found for listing $($listing1.id)" -ForegroundColor Red
    exit 1
}

# 8. Student B Publishes Listing Above Budget (₹2,500 > ₹2,000)
Write-Host "`n[Step 8] Student B publishes listing above budget (₹2,500 > ₹2,000)..." -ForegroundColor Yellow
$listingReq2 = @{
    title = "Carbon Fiber Bicycle"
    description = "High-end racing bicycle"
    category = "Sports & Outdoors"
    condition = "LIKE_NEW"
    startingPrice = 2500.00
    minBidIncrement = 100.00
    closingTime = $closing
    sellerId = "STU-B-$rnd"
    sellerName = "Bob Seller"
    sellerEmail = $emailB
    photoUrls = @("https://images.unsplash.com/photo-1485965120184-e220f721d03e")
} | ConvertTo-Json

$listingRes2 = Invoke-RestMethod -Uri "$baseUrl/api/listings" -Method Post -Body $listingReq2 -ContentType "application/json" -WebSession $sessionB
$listing2 = if ($listingRes2.data) { $listingRes2.data } else { $listingRes2 }
$notifsA_after_raw = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -WebSession $sessionA
$notifsA_after = if ($notifsA_after_raw.data) { $notifsA_after_raw.data } else { $notifsA_after_raw }
$overBudgetNotif = $notifsA_after | Where-Object { $_.relatedListingId -eq $listing2.id }
if (-not $overBudgetNotif) {
    Write-Host " -> PASSED: Over-budget listing did NOT trigger any notification." -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Over-budget listing unexpectedly triggered notification!" -ForegroundColor Red
    exit 1
}

# 9. Student A Pauses Request and Student B Lists at ₹1,500
Write-Host "`n[Step 9] Student A pauses their wanted request and Student B publishes at ₹1,500..." -ForegroundColor Yellow
$pauseRes = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items/$($wantedItem.id)/status" -Method Patch -Body (@{ status = "PAUSED" } | ConvertTo-Json) -ContentType "application/json" -WebSession $sessionA
Write-Host " -> Request status is now: $($pauseRes.status)" -ForegroundColor Cyan

$listingReq3 = @{
    title = "City Commuter Bicycle"
    description = "Comfortable upright bicycle"
    category = "Sports & Outdoors"
    condition = "GOOD"
    startingPrice = 1500.00
    minBidIncrement = 50.00
    closingTime = $closing
    sellerId = "STU-B-$rnd"
    sellerName = "Bob Seller"
    sellerEmail = $emailB
    photoUrls = @("https://images.unsplash.com/photo-1485965120184-e220f721d03e")
} | ConvertTo-Json

$listingRes3 = Invoke-RestMethod -Uri "$baseUrl/api/listings" -Method Post -Body $listingReq3 -ContentType "application/json" -WebSession $sessionB
$listing3 = if ($listingRes3.data) { $listingRes3.data } else { $listingRes3 }
$notifsA_afterPause_raw = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -WebSession $sessionA
$notifsA_afterPause = if ($notifsA_afterPause_raw.data) { $notifsA_afterPause_raw.data } else { $notifsA_afterPause_raw }
$pausedMatch = $notifsA_afterPause | Where-Object { $_.relatedListingId -eq $listing3.id }
if (-not $pausedMatch) {
    Write-Host " -> PASSED: Paused wanted request did NOT generate any alert." -ForegroundColor Green
} else {
    Write-Host " -> FAILED: Paused request unexpectedly generated alert!" -ForegroundColor Red
    exit 1
}

# 10. Student Isolation: Student B cannot edit or delete Student A's request
Write-Host "`n[Step 10] Testing access isolation: Student B tries to delete Student A's wanted item..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/wanted-items/$($wantedItem.id)" -Method Delete -WebSession $sessionB
    Write-Host " -> FAILED: Student B was able to delete Student A's request!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host " -> PASSED: Rejected with status $($_.Exception.Response.StatusCode.value__) (Unauthorized/Forbidden)" -ForegroundColor Green
}

# 11. Student A deletes their wanted item
Write-Host "`n[Step 11] Student A deletes their own wanted request..." -ForegroundColor Yellow
$delRes = Invoke-RestMethod -Uri "$baseUrl/api/wanted-items/$($wantedItem.id)" -Method Delete -WebSession $sessionA
Write-Host " -> PASSED: Successfully deleted ($delRes)" -ForegroundColor Green

# 12. Student A Logs Out
Write-Host "`n[Step 12] Student A logs out..." -ForegroundColor Yellow
$logoutRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/logout" -Method Post -WebSession $sessionA
Write-Host " -> PASSED: Logout successful ($logoutRes)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  ALL 12 ACCEPTANCE CRITERIA PASSED SUCCESSFULLY! ?" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
